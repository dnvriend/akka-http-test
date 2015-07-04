resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

// to kill and reload the spawned JVM
addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

// to format scala source code
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// enable updating file headers eg. for copyright
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "1.5.0")