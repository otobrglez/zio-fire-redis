import sbt.*

object Dependencies {
  type Version = String
  type Modules = Seq[ModuleID]

  object Versions {
    val zio: Version               = "2.1.19"
    val zioTestContainers: Version = "0.6.0"
    val jedis: Version             = "6.0.0"
    val zioLogging: Version        = "2.5.0"
    val logback: Version           = "1.5.18"
  }

  lazy val zio: Modules = Seq(
    "dev.zio" %% "zio",
    "dev.zio" %% "zio-streams"
  ).map(_ % Versions.zio) ++ Seq(
    "com.github.sideeffffect" %% "zio-testcontainers" % Versions.zioTestContainers % Test
  )

  lazy val logging: Modules = Seq(
    "dev.zio" %% "zio-logging",
    "dev.zio" %% "zio-logging-slf4j2"
  ).map(_ % Versions.zioLogging) ++ Seq(
    "ch.qos.logback" % "logback-classic" % Versions.logback
  )

  lazy val redisCore: Modules = Seq(
    "redis.clients" % "jedis" % Versions.jedis
  )
}
