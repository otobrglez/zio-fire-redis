# ZIO Fire Redis

[zio-fire-redis] is a Redis\* client for ZIO ecosystem that depends only on the official Redis Java Client - [
`redis/jedis`][jedis].

## Q&A

### What is between ZIO Fire Redis and ZIO Redis?

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