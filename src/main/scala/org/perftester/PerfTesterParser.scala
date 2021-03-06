package org.perftester

import java.io.File

import ammonite.ops.Path

object PerfTesterParser {
  val parser = new scopt.OptionParser[EnvironmentConfig]("ProfileMain") {
    head("perf_tester", "1.0")

    opt[Int]('i', "iterations").action( (x, c) =>
      c.copy(iterations = x) ).text("The number of iterations to run (default 50)")

    opt[File]('s', "scalaCDir").required().valueName("<dir>").
      action{ case (x, c) =>
        assert(x.exists(), "ScalaC checkout directory must exist")
        assert(x.isDirectory, "ScalaC checkout directory must be a directory")
        c.copy(checkoutDir = Path(x.getAbsolutePath)) }.
      text("The ScalaC checkout/build  dir (required)")


    opt[File]('a', "akkaDir").required().valueName("<dir>").
      action{ case (x, c) =>
        assert(x.exists(), "akka checkout directory must exist")
        assert(x.isDirectory, "akka checkout directory must be a directory")
        c.copy(testDir = Path(x.getAbsolutePath)) }.
      text("The test project directory (akka) (required)")

    opt[File]('r', "resultsDir").required().valueName("<dir>").
      action{ case (x, c) =>
        assert(x.exists(), "results directory must exist")
        assert(x.isDirectory, "results directory must be a directory")
        c.copy(outputDir = Path(x.getAbsolutePath)) }.
      text("Where to write results files (required)")

    opt[Boolean]("overwriteResults").action( (b, c) =>
      c.copy(overwriteResults = b) ).text("Whether to overwrite previous results")

    opt[Unit]("runWithDebug").action( (_, c) =>
      c.copy(runWithDebug = true) ).text("Run with debug args (default false)")

    opt[Unit]("analyseOnly").action( (_, c) =>
      c.copy(analyseOnly = true) ).text("only run analysis (default false)")

    help("help").text("prints this usage text")

  }
}
