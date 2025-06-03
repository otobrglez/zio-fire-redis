# ZIO Fire Redis
[![Ogrodje Site Build](https://github.com/otobrglez/zio-fire-redis/actions/workflows/scala.yml/badge.svg)](https://github.com/otobrglez/zio-fire-redis/actions/workflows/scala.yml)

[zio-fire-redis] is a Redis\* client for the ZIO ecosystem that depends only on [Jedis] - the official Redis Java Client.

## Usage

```scala 3
// Some other imports... and:
import zio.fire.redis.Redis

object HelloFireRedis extends ZIOAppDefault:

  def program = for
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

  def run = (program *> programWithService)
    .provide(
      Scope.default,
      Redis.liveUnscopedFromURI(new java.net.URI("redis://localhost:6379"))
    )
```

## Q&A

### What is the difference between ZIO Fire Redis and ZIO Redis?

- [zio-fire-redis] depends **only** on [jedis] and ZIO.
- [zio-fire-redis] supports streaming operations via [ZIO Streams][zio-streams].
- [zio-fire-redis] supports only Scala `>= 3`.
- [zio-redis] depends on `redis4cats` (that uses [redis/lettuce][lettuce] internally) and [Cats][cats]
  with [Cats Effect 3](https://typelevel.org/cats-effect/).

### Should I use [zio-fire-redis] or [zio-redis]?

If you are looking for a library that was in development for years, then [zio-redis] is what you should use. But.
If you want to use a **light-weight**, **ergonomic** and **fast** ZIO-native library, then [zio-fire-redis] has you
covered!

### Does this thing work only with official Redis?

[zio-fire-redis] is tested with the official and latest Redis and [Valkey][valkey] via their Docker images.


## Contributing

Please use the PRs and GitHub issues. Contributions are welcome!

[zio-fire-redis]: https://github.com/otobrglez/zio-fire-redis

[zio-redis]: https://github.com/zio/zio-redis

[jedis]: https://github.com/redis/jedis

[zio-streams]: https://zio.dev/reference/stream/

[lettuce]: https://github.com/redis/lettuce

[cats]: (https://typelevel.org/cats/)

[valkey]: https://hub.docker.com/r/valkey/valkey/
