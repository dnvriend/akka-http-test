name := "akka-http-test"

version := "1.0.0"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val akkaVersion       = "2.3.11"
  val akkaStreamVersion = "1.0-RC4"
  val scalaTestVersion  = "2.2.4"
  Seq(
    "com.typesafe.akka"      %% "akka-actor"                           % akkaVersion,
    "com.typesafe.akka"      %% "akka-stream-experimental"             % akkaStreamVersion,
    "com.typesafe.akka"      %% "akka-http-core-experimental"          % akkaStreamVersion,
    "com.typesafe.akka"      %% "akka-http-experimental"               % akkaStreamVersion,
    "com.typesafe.akka"      %% "akka-http-spray-json-experimental"    % akkaStreamVersion,
    "com.typesafe.akka"      %% "akka-http-xml-experimental"           % akkaStreamVersion,
    "com.typesafe.akka"      %% "akka-http-testkit-experimental"       % akkaStreamVersion,
    "org.scalatest"          %% "scalatest"                            % "2.2.4"    % Test
  )
}

lazy val root = project.in(file("."))
  .settings(Common.settings)
  .enablePlugins(AutomateHeaderPlugin)

// enable updating file headers //
import de.heikoseeberger.sbtheader.license.Apache2_0

headers := Map(
  "scala" -> Apache2_0("2015", "Dennis Vriend"),
  "conf" -> Apache2_0("2015", "Dennis Vriend", "#"),
  "sbt" -> Apache2_0("2015", "Dennis Vriend", "#")
)