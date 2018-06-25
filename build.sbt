lazy val root = (project in file(".")).
  settings(
    name := "sbt-meghanada",
    version := "0.1.1-SNAPSHOT",
    organization := "com.github.jypma",
    scalaVersion := "2.12.5",
    sbtPlugin := true,
    sbtVersion := "1.0.2",
    EclipseKeys.withSource := true,
  )
  