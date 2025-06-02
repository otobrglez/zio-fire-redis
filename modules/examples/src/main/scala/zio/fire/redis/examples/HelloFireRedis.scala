package zio.fire.redis.examples

import redis.clients.jedis.StreamEntryID
import redis.clients.jedis.params.XReadParams
import zio.Console.printLine
import zio.Runtime.removeDefaultLoggers
import zio.fire.redis.Redis
import zio.logging.backend.SLF4J
import zio.stream.ZStream
import zio.{Random, Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

object HelloFireRedis extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = removeDefaultLoggers >>> SLF4J.slf4j

  def program = for
    _ <- printLine("Hello this is example w/ ZIO Fire Redis.")
    // Basic key-value examples
    _ <- Redis.set("name", "Oto") *> Redis.set("env", "development")
    _ <- Redis.get("name").debug("name")
    // Hash examples
    _ <- Redis.hSet("hello::hash", "this" -> "is", "a" -> "hash")
    _ <- Redis.hGet("hello::hash", "this").debug("hash value")
    _ <- Redis.hGetAll("hello::hash").debug("hash")
    // Set examples
    _ <- Redis.sAdd("hello::set", "one", "two", "three", "two")
  yield ()

  def programWithService = for
    redis <- ZIO.service[Redis]
    _     <- redis.set("greet", "Hello World!") *> redis.get("greet").debug("greeting")
    _     <- redis.mGet("name", "greet").debug("name and greeting")
    // List examples
    _     <- redis.lPush("list", "one", "two", "three") *> redis.lLen("list").debug("length")
  yield ()

  def streamsExample = for
    _               <- printLine("~~~ Streams ~~~")
    streamPublisher <-
      ZStream
        .repeatZIO(Random.nextIntBetween(0, 1600) <&> Random.nextIntBetween(0, 1200))
        .take(10)
        .runForeach((x, y) => Redis.xAdd("clicks", "position_x" -> x.toString, "position_y" -> y.toString))
        .fork

    _ <- Redis.xLen("clicks").debug("clicks")

    _ <- Redis.xRead(XReadParams.xReadParams().block(1_00), "clicks" -> StreamEntryID("0-0"))
    _ <- streamPublisher.join
  yield ()

  def run = (program *> programWithService *> streamsExample)
    .provide(
      Scope.default,
      Redis.liveUnscopedFromURI(new java.net.URI("redis://localhost:6379"))
    )
