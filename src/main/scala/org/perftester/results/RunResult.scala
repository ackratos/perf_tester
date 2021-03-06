package org.perftester.results

import java.util

import org.perftester.TestConfig

import scala.collection.SortedSet

case class RunResult(testConfig: TestConfig, rawData: Seq[PhaseResults], iterations: SortedSet[Int], phases: Set[String]) {

  rawData foreach { r => require(iterations(r.iterationId)) }

  rawData foreach { r => require(phases(r.phaseName)) }

  def filterIteration(min: Int, max: Int): RunResult = {
    val filter = (min to max).toSet
    RunResult(testConfig, rawData.filter(res => filter(res.iterationId)), iterations.intersect(filter), phases)
  }

  def filterPhases(phaseNames: String*): RunResult = {
    val filter = phaseNames.toSet
    RunResult(testConfig, rawData.filter(res => filter(res.phaseName)), iterations, phases.intersect(filter))
  }

  def filterNoGc: RunResult = RunResult(testConfig,
    rawData.filter { res =>
      res.gcTimeMS == 0
    },
    iterations, phases
  )

  class Aggregate(val grouped: Seq[PhaseResults]) {
    val totals: PhaseResults = PhaseResults.combine(grouped, _ + _)
    val min: PhaseResults = PhaseResults.combine(grouped, Math.min)
    val max: PhaseResults = PhaseResults.combine(grouped, Math.max)
    val mean: PhaseResults = PhaseResults.transform(totals, _ / grouped.size)
  }

  class Distribution(results: Array[Double]) {
    val min: Double = results.min
    val max: Double = results.max

    val mean = results.sum / size

    def size = results.length

    def median = at(.5)

    def iqr = at(.75) - at(.25)

    def at(pos: Double) = {
      assert(pos >= 0.0)
      assert(pos <= 1.0)
      val index = ((size - 1) * pos).toInt
      results(index)
    }
    def atPC(pos:Double) = {
      val value = at(pos)
      ((value / mean) -1) *100
    }

    override def toString: String = s"$mean [+${atPC(1)}% :${atPC(.9)}% -${atPC(0)}%"

    private def formatPercent(sigDigits: Int, decimalDigits: Int, value: Double): String = {
      String.format(s"%+$sigDigits.${decimalDigits}f", new java.lang.Double(value))
    }

    private def formatResult(sigDigits: Int, decimalDigits: Int, value: Double): String = {
      String.format(s"%,$sigDigits.${decimalDigits}f", new java.lang.Double(value))
    }

    def formatted(s: Int, p: Int) = {
      s"${formatResult(s, p, mean)} [${formatPercent(4, 2, atPC(0))}% ${formatPercent(4, 2, atPC(1))}%]"
    }
  }

  object Distribution {
    def range(lower: Double, upper: Double, aggregate: Aggregate, fn: (PhaseResults) => Double) = {
      val results: Array[Double] = aggregate.grouped.map(fn)(scala.collection.breakOut)
      util.Arrays.sort(results)
      new Distribution(results.slice((results.size * lower).toInt, (results.size * upper).toInt))
    }
  }

  lazy val byPhaseId = rawData.groupBy(_.phaseId) map { case (k, v) => k -> new Aggregate(v) }
  lazy val byPhaseName = rawData.groupBy(_.phaseName) map { case (k, v) => k -> new Aggregate(v) }
  lazy val byIteration = rawData.groupBy(_.iterationId) map { case (k, v) => k -> new Aggregate(v) }


  lazy val totals = {
    val byIteration = rawData.groupBy(_.iterationId) map { case (k, v) => k -> new Aggregate(v).totals }
    new Aggregate(byIteration.values.toList)
  }

  class Detail(lower: Double, upper: Double) {

    def phaseAllocatedBytes(phase: String) = Distribution.range(lower, upper, byPhaseName(phase), _.allocatedMB)

    def phaseWallClockMS(phase: String) = Distribution.range(lower, upper, byPhaseName(phase), _.wallClockTimeMS)

    def phaseCpuMS(phase: String) = Distribution.range(lower, upper, byPhaseName(phase), _.cpuTimeMS)

    lazy val allWallClockMS = Distribution.range(lower, upper, totals, _.wallClockTimeMS)
    lazy val allAllocated = Distribution.range(lower, upper, totals, _.allocatedMB)
    lazy val allCPUTime = Distribution.range(lower, upper, totals, _.cpuTimeMS)
  }

  val all = new Detail(0, 1)
  val std = new Detail(0, .9)
}
