package zio.fire.redis
import zio.ZIO.logInfo
import zio.test.*

import java.net.URI
import scala.util.Try
object RedisSpec extends ZIOSpecDefault:
  private def envUriSync(name: String): Option[URI] =
    Try(sys.env.get(name).map(new URI(_))).toOption.flatten

  private lazy val platforms: Map[String, URI] = Seq(
    "redis"  -> envUriSync("REDIS_URI"),
    "valkey" -> envUriSync("VALKEY_URI")
  ).collect { case (k, Some(v)) => k -> v }.toMap

  def spec = suite("RedisSpec")(
    platforms.toSeq.map { case (k, uri) =>
      suite(s"Integration with $k on ${uri}")(
        test("Strings") {
          check(Gen.alphaNumericStringBounded(1, 10).zip(Gen.alphaNumericString)) { (key, value) =>
            for
              _              <- Redis.set(key, value)
              collectedValue <- Redis.get(key)
              _              <- Redis.del(key)
              anotherV       <- Redis.get(key)
            yield assertTrue(
              collectedValue.get == value && anotherV.isEmpty
            )
          }
        } @@ TestAspect.beforeAll(
          Redis.flushDB <* logInfo("Flushed DB")
        )
      ).provide(Redis.liveFromURI(uri = uri)) @@ TestAspect.withLiveSystem
    }
  )
