name := "akka-http-test"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= {
  val akkaVersion       = "2.3.11"
  val akkaStreamVersion = "1.0-RC2"
  val scalaTestVersion  = "2.2.4"
  Seq(
    "com.typesafe.akka" %% "akka-actor"                           % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-scala-experimental"         % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-xml-experimental"           % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-testkit-scala-experimental" % akkaStreamVersion,
    "org.scalatest"     %% "scalatest"                            % scalaTestVersion    % Test
  )
}

import spray.revolver.RevolverPlugin.Revolver

Revolver.settings

Revolver.enableDebugging(port = 5050, suspend = false)

mainClass in Revolver.reStart := Some("com.github.dnvriend.SimpleServer")