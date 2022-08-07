val scala3Version = "3.1.3"

lazy val root = project
  .in(file("."))
  .settings(
    name := "voting",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
    semanticdbEnabled := true,
  )

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
