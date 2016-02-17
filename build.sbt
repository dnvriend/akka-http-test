name := "akka-http-test"

version := "1.0.0"

// see: https://github.com/scala/scala
scalaVersion := "2.11.7"

libraryDependencies ++= {
  val akkaVersion = "2.4.2"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-xml-experimental" % akkaVersion,
    "com.hunorkovacs" %% "koauth" % "1.1.0" exclude("com.typesafe.akka", "akka-actor_2.11"),
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "2.2.4" % Test
  )
}

scalacOptions ++= Seq("-feature", "-language:higherKinds", "-language:implicitConversions", "-deprecation", "-Ybackend:GenBCode", "-Ydelambdafy:method", "-target:jvm-1.8")

licenses +=("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

// enable updating file headers //
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbt.Keys._

headers := Map(
  "scala" -> Apache2_0("2016", "Dennis Vriend"),
  "conf" -> Apache2_0("2016", "Dennis Vriend", "#")
)

// enable scala code formatting //

import scalariform.formatter.preferences._

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)

// enable sbt-revolver
import spray.revolver.RevolverPlugin.Revolver

Revolver.settings ++ Seq(
  Revolver.enableDebugging(port = 5050, suspend = false),
  mainClass in Revolver.reStart := Some("com.github.dnvriend.SimpleServer")
)

// configure code lint //
//wartremoverWarnings ++= Seq(Wart.Any, Wart.Serializable)
wartremoverWarnings ++= Warts.unsafe

// configure build info //
// build info configuration //
buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

buildInfoPackage := "com.github.dnvriend"

// enable plugins //
enablePlugins(AutomateHeaderPlugin, BuildInfoPlugin)
