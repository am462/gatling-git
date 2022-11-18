import sbt._

object Dependencies {
  val GatlingVersion = "3.3.1"

  lazy val gatling = Seq(
    "io.gatling.highcharts" % "gatling-charts-highcharts",
    "io.gatling" % "gatling-test-framework",
  ).map(_ % GatlingVersion % Test)
}
