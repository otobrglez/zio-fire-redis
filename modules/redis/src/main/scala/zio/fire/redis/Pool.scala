package zio.fire.redis

import redis.clients.jedis.{JedisPool, JedisPoolConfig}
import zio.ZIO.attempt
import zio.{RIO, Scope, Task, ZIO, ZLayer}

object Pool:
  private val defaultPoolConfig: JedisPoolConfig =
    val poolConfig = new JedisPoolConfig()
    poolConfig

  private def mkPoolConfig(): Task[JedisPoolConfig] = ZIO.succeed(defaultPoolConfig)

  def fromConfigZIO(
    uri: RedisURI,
    config: Option[JedisPoolConfig] = None
  ): RIO[Scope, JedisPool] = for
    poolConfig <- config.fold(mkPoolConfig())(c => ZIO.succeed(c))
    pool       <- ZIO.fromAutoCloseable(attempt(new JedisPool(poolConfig, uri)))
  yield pool

  def fromConfig(
    uri: RedisURI,
    builder: JedisPoolConfig => JedisPoolConfig = identity
  ): ZLayer[Scope, Throwable, JedisPool] =
    ZLayer.fromZIO(fromConfigZIO(uri, config = Some(builder(defaultPoolConfig))))
