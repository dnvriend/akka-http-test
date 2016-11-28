// see: https://github.com/playframework/playframework/blob/master/framework/src/sbt-plugin/src/main/scala/play/sbt/PlayImport.scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.10")

// enable conductr integration
// https://github.com/typesafehub/sbt-conductr
addSbtPlugin("com.lightbend.conductr" % "sbt-conductr" % "2.1.18")

// to format scala source code
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

// enable updating file headers eg. for copyright
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.1")