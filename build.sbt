
// See README.md for license details.

ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.0.0"

val chiselVersion = "3.5.6"

lazy val prose = {
  (project in file("."))
    .settings(
      name := "project-name",
      libraryDependencies ++= Seq(
        "edu.berkeley.cs" %% "chisel3" % chiselVersion,
        "edu.duke.cs.apex" %% "beethoven-hardware" % "0.0.0"
      ),
      resolvers += ("reposilite-repository-releases" at "http://oak.cs.duke.edu:8080/releases").withAllowInsecureProtocol(true),
      addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
    )
}
