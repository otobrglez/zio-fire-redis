import Dependencies.Versions._

Global / onChangedBuildSource := ReloadOnSourceChanges
enablePlugins(ZioSbtEcosystemPlugin)

version := "0.0.1"
lazy val scala3Version = "3.7.0"

inThisBuild(
  List(
    name                       := "ZIO Fire Redis",
    developers                 := List(
      Developer("otobrglez", "Oto Brglez", "otobrglez@gmail.com", url("https://github.com/otobrglez"))
    ),
    startYear                  := Some(2025),
    scalaVersion               := scala3Version,
    // scala3                     := scala3Version,
    zioVersion                 := Dependencies.Versions.zio,
    allowUnsafeScalaLibUpgrade := true,
    scalacOptions ++= Seq(
      "-language:implicitConversions",
      "-explain",
      // "-indent",
      "-rewrite",
      "-new-syntax",
      "-release:11"
    )
  )
)

lazy val root = project
  .in(file("."))
  .settings(
    name           := "zio-fire-redis",
    // crossScalaVersions := Nil,
    publish / skip := true
  )
  .dependsOn(
    client,
    examples,
    `integration-tests`
  )

lazy val client = project
  .in(file("modules/redis"))
  .settings(
    name             := "zio-fire-redis",
    buildInfoPackage := "zio.fire.redis",
    libraryDependencies ++= Dependencies.redisCore ++ Dependencies.zio,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val examples = project
  .in(file("modules/examples"))
  .settings(
    name             := "examples",
    buildInfoPackage := "zio.fire.redis.examples",
    libraryDependencies ++= Dependencies.logging
  )
  .dependsOn(client)

lazy val `integration-tests` = project
  .in(file("modules/it"))
  .dependsOn(client)
  .settings(
    publish / skip := true,
    Test / fork    := false,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt")
addCommandAlias("fix", ";scalafixAll")
