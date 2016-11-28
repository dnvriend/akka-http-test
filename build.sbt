name := "akka-http-test"

version := "1.0.0"

scalaVersion := "2.11.8"

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

libraryDependencies ++= {
  val akkaVersion = "2.4.14"
  val httpVersion = "10.0.0"
  val scalazVersion = "7.2.7"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % httpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % httpVersion,
    "com.typesafe.akka" %% "akka-http-xml" % httpVersion,
    "de.heikoseeberger" %% "akka-http-play-json" % "1.10.1",
    "com.github.nscala-time" %% "nscala-time" % "2.14.0",
    "com.chuusai" %% "shapeless" % "2.3.2",
    "org.scalaz" %% "scalaz-core" % scalazVersion,
    "org.typelevel" %% "scalaz-outlaws" % "0.2",
    "com.hunorkovacs" %% "koauth" % "1.1.0" exclude("com.typesafe.akka", "akka-actor_2.11") exclude("org.specs2", "specs2_2.11"),
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "com.h2database" % "h2" % "1.4.193",
    "com.typesafe.akka" %% "akka-http-testkit" % httpVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "org.typelevel" %% "scalaz-scalatest" % "1.1.1" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0-M1" % Test
  )
}

licenses += ("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

// enable updating file headers //
import de.heikoseeberger.sbtheader.license.Apache2_0

headers := Map(
  "scala" -> Apache2_0("2016", "Dennis Vriend"),
  "conf" -> Apache2_0("2016", "Dennis Vriend", "#")
)

// enable scala code formatting //
import com.typesafe.sbt.SbtScalariform
import scalariform.formatter.preferences._

// Scalariform settings
SbtScalariform.autoImport.scalariformPreferences := SbtScalariform.autoImport.scalariformPreferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)

// enable plugins //
enablePlugins(AutomateHeaderPlugin, SbtScalariform, PlayScala)
