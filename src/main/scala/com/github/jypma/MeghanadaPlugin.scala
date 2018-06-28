package com.github.jypma

import sbt.Keys._
import sbt._
import complete.DefaultParsers._
import sbt.librarymanagement.ModuleDescriptor
import sbt.internal.BuildStructure
import java.io.FileWriter
import java.io.File
import java.io.PrintWriter

object MeghanadaPlugin extends AutoPlugin {

  private def fail(errorMessage: String)(implicit state: State): Nothing = {
    state.log.error(errorMessage)
    throw new IllegalArgumentException()
  }
  
  // our version of http://stackoverflow.com/questions/25246920
  implicit class RichSettingKey[A](key: SettingKey[A]) {
    def gimme(pr: ProjectRef)(implicit bs: BuildStructure, s: State): A =
      gimmeOpt(pr) getOrElse { fail(s"Missing setting: ${key.key.label}") }
    def gimmeOpt(pr: ProjectRef)(implicit bs: BuildStructure): Option[A] =
      key in pr get bs.data
  }
  
  implicit class RichTaskKey[A](key: TaskKey[A]) {
    def run(pr: ProjectRef)(implicit bs: BuildStructure, s: State): A =
      runOpt(pr).getOrElse { fail(s"Missing task key: ${key.key.label}") }
    def runOpt(pr: ProjectRef)(implicit bs: BuildStructure, s: State): Option[A] =
      EvaluateTask(bs, key, s, pr).map(_._2) match {
        case Some(Value(v)) => Some(v)
        case _              => None
      }

    def forAllProjects(state: State, projects: Seq[ProjectRef]): Task[Map[ProjectRef, A]] = {
      val tasks = projects.flatMap(p => key.in(p).get(Project.structure(state).data).map(_.map(it => (p, it))))
      std.TaskExtra.joinTasks(tasks).join.map(_.toMap)
    }
  }

  override def trigger = allRequirements
  
  object autoImport {
    val meghanada = taskKey[Unit]("Generate meghanada configuration for all projects")
    val meghanadaConfig = taskKey[Unit]("Generate meghanada configuration for a project")
  }

  import autoImport._
  
  def quotedArray(s: Seq[String]): String = s.map(f => "\"" + f + "\"").mkString(",\n")

  def generate(proj: ProjectRef)(implicit bs: BuildStructure, s: State): Unit = {
    val out = new PrintWriter(new FileWriter(s"${baseDirectory.gimme(proj)}${File.separator}.meghanada.conf"))
      out.println("""
java_home = "/usr/lib/jvm/default"

java-version = "1.8"

compile-source = "1.8"

compile-target = "1.8"""")

    val jars = (dependencyClasspath in Compile).run(proj)
      .filter(_.data.getPath.endsWith(".jar")) // Throw out project dependencies
      .map(_.data.getAbsolutePath)
    out.println(s"dependencies = [${quotedArray(jars)}]")

    val depSrcs: Seq[File] = thisProject.gimme(proj).dependencies.map(_.project).flatMap(p =>
      (sourceDirectories in Compile).gimme(p))

    val srcs =
      (sourceDirectories in Compile).gimme(proj).map(_.getAbsolutePath) ++ depSrcs.map(_.getAbsolutePath)
    out.println(s"sources = [${quotedArray(srcs)}]")

    val testJars = (dependencyClasspath in Test).run(proj)
      .filter(_.data.getPath.endsWith(".jar")) // Throw out project dependencies
      .map(_.data.getAbsolutePath)
    out.println(s"test-dependencies = [${quotedArray(testJars)}]")

    val testSrcs =
      (sourceDirectories in Test).gimme(proj).map(_.getAbsolutePath) ++ depSrcs.map(_.getAbsolutePath)
    out.println(s"test-sources = [${quotedArray(testSrcs)}]")

    out.println(s"""
resources = ["src/main/resources"]

output = "target/scala-2.12/classes"

test-resources = ["src/test/resources"]

test-output = "target/scala-2.12/test-classes"
""")
    out.close()
  }

  override lazy val globalSettings = Seq(
    // TODO consider for removal, since aggregating on meghanadaConfig just works.
    meghanada := {
      val extracted = Project.extract(state.value)
      implicit val st = state.value
      implicit val bs = extracted.structure
      val projs = Project.structure(state.value).allProjectRefs

      projs.foreach {  proj =>
        generate(proj)
      }
    }
  )

  override lazy val projectSettings = Seq(
    meghanadaConfig := {

      val out = new PrintWriter(new FileWriter(s"${baseDirectory.value}${File.separator}.meghanada.conf"))
      val extracted = Project.extract(state.value)
      implicit val st = state.value
      implicit val bs = extracted.structure

      out.println("""
java_home = "/usr/lib/jvm/default"

java-version = "1.8"

compile-source = "1.8"

compile-target = "1.8"""")
      val jars = (dependencyClasspath in Compile).value
        .filter(_.data.getPath.endsWith(".jar")) // Throw out project dependencies
        .map(_.data.getAbsolutePath)
      out.println(s"dependencies = [${quotedArray(jars)}]")

      val depSrcs: Seq[File] = thisProject.value.dependencies.map(_.project).flatMap(p => (sourceDirectories in Compile).gimme(p))
      val srcs = 
        (sourceDirectories in Compile).value.map(_.getAbsolutePath) ++ 
        depSrcs.map(_.getAbsolutePath)
      out.println(s"sources = [${quotedArray(srcs)}]")

      val testJars = (dependencyClasspath in Test).value
        .filter(_.data.getPath.endsWith(".jar")) // Throw out project dependencies
        .map(_.data.getAbsolutePath)
      out.println(s"test-dependencies = [${quotedArray(testJars)}]")

      val testSrcs = 
        (sourceDirectories in Test).value.map(_.getAbsolutePath) ++ 
        depSrcs.map(_.getAbsolutePath)
      out.println(s"test-sources = [${quotedArray(testSrcs)}]")
      
      out.println(s"""
resources = ["src/main/resources"]

output = "target/scala-2.12/classes"

test-resources = ["src/test/resources"]

test-output = "target/scala-2.12/test-classes"
""")
       out.close()
    }
  )
}
