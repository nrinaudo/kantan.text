// - Dependency versions -----------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
val scalacheckVersion  = "1.13.4"
val scalatestVersion   = "3.0.1"

kantanProject in ThisBuild := "text"


// - root projects -----------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
lazy val root = Project(id = "kantan-text", base = file("."))
  .settings(moduleName := "root")
  .enablePlugins(UnpublishedPlugin)
  .settings(doctestWithDependencies := false)
  .settings(
    initialCommands in console :=
    """
      |import kantan.text._
    """.stripMargin
  )
  .aggregate(tests, docs, core)
  .dependsOn(core)

lazy val tests = project
  .enablePlugins(UnpublishedPlugin)
  .settings(doctestWithDependencies := false)
  .settings(libraryDependencies += "org.scalatest" %% "scalatest" % scalatestVersion % "test")
  .dependsOn(core)

lazy val docs = project
  .settings(doctestWithDependencies := false)
  .enablePlugins(DocumentationPlugin)


// - core projects -----------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
lazy val core = project
  .settings(
    moduleName := "kantan.text",
    name       := "core"
  )
  .settings(doctestTestFramework := DoctestTestFramework.ScalaTest)
  .settings(doctestWithDependencies := false)
  .settings(libraryDependencies ++= Seq(
    "org.scalatest"      %% "scalatest"     % scalatestVersion  % "test",
    "org.scalacheck"     %% "scalacheck"    % scalacheckVersion % "test"
  ))
  .enablePlugins(PublishedPlugin)
