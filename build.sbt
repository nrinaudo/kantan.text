kantanProject in ThisBuild := "text"


// - root projects -----------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
lazy val root = Project(id = "kantan-text", base = file("."))
  .settings(moduleName := "root")
  .enablePlugins(UnpublishedPlugin)
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
  .settings(libraryDependencies += "org.scalatest" %% "scalatest" % Versions.scalatest % "test")
  .dependsOn(core)

lazy val docs = project
  .enablePlugins(DocumentationPlugin)


// - core projects -----------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
lazy val core = project
  .settings(
    moduleName := "kantan.text",
    name       := "core"
  )
  .settings(libraryDependencies += "org.scalatest"      %% "scalatest"     % Versions.scalatest  % "test")
  .enablePlugins(PublishedPlugin)
