scalaVersion := "2.12.4"

libraryDependencies := Seq(
    "com.github.scopt" % "scopt_2.12" % "3.7.0",
    "com.github.pathikrit" %% "better-files" % "3.4.0",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
)

assemblyJarName in assembly := "utils.jar"