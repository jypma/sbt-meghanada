# sbt-meghanada

This is an SBT plugin that generates configuration files for
[meghanada](http://www.skybert.net/emacs/java-programming-in-emacs-with-meghanada-mode/). This allows emacs
users to use `meghanada-mode` to navigate Java code that comes from an SBT project.

Meghanada itself only supports Maven or Grails natively, but will use a `.meghanada.conf` configuration file
if it finds one. This plugin generates that file for each SBT sub-project.

## Installing

Clone this git repository, `sbt publishLocal`, and add this to your `~/.sbt/1.0/plugins/plugins.sbt`: (only
SBT 1.0+ is supported)

```
addSbtPlugin("com.github.jypma" % "sbt-meghanada" % "0.1.1-SNAPSHOT")
```

After that, go into any SBT project that has Java sources, and do

```
sbt meghanadaConfig
```

This should `aggregate` into sub-projects if your project is set up that way.
