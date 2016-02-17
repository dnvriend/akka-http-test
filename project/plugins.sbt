resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += "bintray-sbt-plugin-releases" at "http://dl.bintray.com/content/sbt/sbt-plugin-releases"

// to kill and reload the spawned JVM
addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

// to show a dependency graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.0")

// to format scala source code
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// enable updating file headers eg. for copyright
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.0")

// generates Scala source from your build definitions //
// see: https://github.com/sbt/sbt-buildinfo
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.5.0")

// code lint //
addSbtPlugin("org.brianmckenna" % "sbt-wartremover" % "0.14")