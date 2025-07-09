import scala.sys.process._
// OBS: sbt._ has also process. Importing scala.sys.process
// and explicitly using it ensures the correct operation

organization := "numano1"

version := scala.sys.process.Process("git rev-parse --short HEAD").!!.mkString.replaceAll("\\s", "")+"-SNAPSHOT"

scalaVersion := "2.13.8"

val chiselVersion = "3.5.4"
val chiselTestVersion = "0.5.4"
val breezeVersion = "2.0"
val dspVersion = "1.5.6"


lazy val async_set_register = (project in file("async_set_register"))

lazy val spi_slave = (project in file("."))
  .settings(
    name := "spi_slave",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % chiselTestVersion,
      "edu.berkeley.cs" %% "dsptools" % dspVersion,
      "edu.berkeley.cs" %% "chisel-iotesters" % "2.5.0",
      "org.scalanlp" %% "breeze" % breezeVersion,
      "org.scalanlp" %% "breeze-natives" % breezeVersion,
      "org.scalanlp" %% "breeze-viz" % breezeVersion
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-P:chiselplugin:genBundleElements",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
  )
  .dependsOn(async_set_register)
  .aggregate(async_set_register)
