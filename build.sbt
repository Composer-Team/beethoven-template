
// See README.md for license details.

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version := "0.0.0"

val chiselVersion = "7.1.0"

lazy val beethoven = RootProject(file("../Beethoven-Hardware"))


lazy val root = {
  (project in file("."))
    .settings(
      name := "project-name",
      libraryDependencies ++= Seq(
        "org.chipsalliance" %% "chisel" % chiselVersion,
        // "edu.duke.cs.apex" %% "beethoven-hardware" % "0.0.34"
      ),
      // we're currently hosting a maven server on an AWS instance, prior to official release on a global repository
      resolvers += ("reposilite-repository-releases" at "http://54.165.244.214:8080/releases").withAllowInsecureProtocol(true),
      addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full),
    )
    .dependsOn(beethoven)
}
