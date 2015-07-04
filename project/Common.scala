import sbt._
import sbt.Keys._

object Common {
  lazy val settings = Seq(
    scalacOptions ++= Seq("-feature", "-language:higherKinds", "-language:implicitConversions", "-deprecation", "-Ybackend:GenBCode", "-Ydelambdafy:method", "-target:jvm-1.8"),
    licenses += ("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))
  ) ++ ScalariformSettings.settings ++ RevolverSettings.settings
}

/**
 * enable and configure sbt revolver
 */
object RevolverSettings {
  import spray.revolver.RevolverPlugin.Revolver

  lazy val settings = Revolver.settings ++ Seq(
    Revolver.enableDebugging(port = 5050, suspend = false),
    mainClass in Revolver.reStart := Some("com.github.dnvriend.SimpleServer")
  )
}

/**
 * enable scala code formatting
 */
object ScalariformSettings {
  import scalariform.formatter.preferences._
  import com.typesafe.sbt.SbtScalariform
  import com.typesafe.sbt.SbtScalariform.ScalariformKeys

  lazy val settings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 90)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(RewriteArrowSymbols, true)
    .setPreference(CompactControlReadability, true)
    .setPreference(CompactStringConcatenation, false)
    .setPreference(IndentLocalDefs, true)
  )
}