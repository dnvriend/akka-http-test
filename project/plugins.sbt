// see: https://github.com/playframework/playframework/blob/master/framework/src/sbt-plugin/src/main/scala/play/sbt/PlayImport.scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.12")

// to format scala source code
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

// enable updating file headers eg. for copyright
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.1")

// https://github.com/typesafehub/sbt-conductr
addSbtPlugin("com.lightbend.conductr" % "sbt-conductr" % "2.3.0")

// see: https://developer.lightbend.com/docs/monitoring/latest/home.html
addSbtPlugin("com.lightbend.cinnamon" % "sbt-cinnamon" % "2.2.4")

// to create your '.credentials' file: https://developer.lightbend.com/docs/reactive-platform/2.0/setup/setup-sbt.html
// for credentials: https://www.lightbend.com/product/lightbend-reactive-platform/credentials
// create a developer account: https://www.lightbend.com/account
credentials += Credentials(Path.userHome / ".lightbend" / "commercial.credentials")

resolvers += Resolver.url("lightbend-commercial", url("https://repo.lightbend.com/commercial-releases"))(Resolver.ivyStylePatterns)
