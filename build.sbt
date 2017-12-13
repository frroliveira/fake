import ReleaseTransformations._

organization       := "com.github.frroliveira"
name               := "fake"
description        := "Fake Builder Library for Scala"
scalaVersion       := "2.12.3"
crossScalaVersions := Seq("2.12.3")
scalacOptions      := Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:dynamics",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-unused",
  "-Ywarn-unused-import",
  "-Ywarn-value-discard"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "macro-compat" % "1.1.1",
  scalaOrganization.value % "scala-reflect" % scalaVersion.value % "provided",
  scalaOrganization.value % "scala-compiler" % scalaVersion.value % "provided",
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.patch)
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)

publishMavenStyle       := true
publishTo               := Some(sonatypeDefaultResolver.value)
publishArtifact in Test := false
pomIncludeRepository    := Function.const(false)

homepage := Some(url("https://github.com/frroliveira/fake"))
licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))
scmInfo  := Some(ScmInfo(url("https://github.com/frroliveira/fake"), "scm:git:git@github.com:frroliveira/fake.git"))
pomExtra :=
  <developers>
    <developer>
      <id>frroliveira</id>
      <name>Filipe Oliveira</name>
      <url>https://github.com/frroliveira</url>
    </developer>
  </developers>

releaseCrossBuild             := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseProcess                := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)