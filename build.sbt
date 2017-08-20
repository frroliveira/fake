organization       := "com.github.frroliveira"
version            := "0.0.1-SNAPSHOT"
scalaVersion       := "2.12.2"
crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.2")
scalacOptions      := Seq(
  "-feature",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Xfatal-warnings",
  "-deprecation",
  "-unchecked"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "macro-compat" % "1.1.1",
  scalaOrganization.value % "scala-reflect" % scalaVersion.value % "provided",
  scalaOrganization.value % "scala-compiler" % scalaVersion.value % "provided",
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.patch),
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)