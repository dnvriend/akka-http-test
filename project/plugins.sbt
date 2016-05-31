resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += "bintray-sbt-plugin-releases" at "http://dl.bintray.com/content/sbt/sbt-plugin-releases"

// to kill and reload the spawned JVM
// see: https://github.com/spray/sbt-revolver
addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")

// This caused problems for me, so disabled it. 0.7.5 works on my system. AKa300516
//
// to show a dependency graph
// see: https://github.com/jrudolph/sbt-dependency-graph
//addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

// to format scala source code
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

// enable updating file headers eg. for copyright
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.1")

// generates Scala source from your build definitions //
// see: https://github.com/sbt/sbt-buildinfo
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.5.0")

// code lint //
// see: https://github.com/puffnfresh/wartremover
addSbtPlugin("org.brianmckenna" % "sbt-wartremover" % "0.14")
