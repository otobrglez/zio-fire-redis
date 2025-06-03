package zio.fire.redis

import redis.clients.jedis.*
import redis.clients.jedis.args.FlushMode
import redis.clients.jedis.params.{GetExParams, SetParams, XAddParams, XReadParams}
import redis.clients.jedis.resps.StreamEntry
import zio.*
import zio.ZIO.*

import java.util
import java.util.List as JavaList
import scala.collection.immutable.Map
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

type Binary              = Array[Byte]
type TTL                 = FiniteDuration
type Key                 = String
type Value               = String
type Field               = String
type RedisTask[Env, Out] = ZIO[Env, Throwable, Out]
type RedisURI            = java.net.URI

private[redis] trait DSL[-Env]:
  def get(key: Key): RedisTask[Env, Option[Value]]
  def set(key: Key, value: Value): RedisTask[Env, Value]
  def del(key: Key): RedisTask[Env, Long]
  def mGet(keys: Key*): RedisTask[Env, List[Value]]
  def hSet(key: Key, values: (Key, Value)*): RedisTask[Env, Long]
  def hGetAll(key: Key): RedisTask[Env, Map[Key, Value]]
  def hGet(key: Key, field: Field): RedisTask[Env, Value]
  def rPush(key: Key, values: Value*): RedisTask[Env, Long]
  def lPush(key: Key, values: Value*): RedisTask[Env, Long]
  def rPop(key: Key): RedisTask[Env, Value]
  def lPop(key: Key): RedisTask[Env, Value]
  def lPop(key: Key, count: Int): RedisTask[Env, List[Value]]
  def lLen(key: Key): RedisTask[Env, Long]
  def sAdd(key: Key, members: Value*): RedisTask[Env, Long]
  def sMembers(key: Key): RedisTask[Env, Set[Value]]
  def flushDB: RedisTask[Env, String]
  def flushDB(flushMode: FlushMode): RedisTask[Env, String]
  def hIncrBy(key: Key, field: Field, value: Long): RedisTask[Env, Long]
  def xAdd(key: Key, streamEntryID: StreamEntryID, values: (Key, Value)*): RedisTask[Env, StreamEntryID]
  def xAdd(key: Key, params: XAddParams, values: (Key, Value)*): RedisTask[Env, StreamEntryID]
  def xLen(key: Key): RedisTask[Env, Long]
  def xRange(key: Key, start: StreamEntryID, end: StreamEntryID): RedisTask[Env, List[StreamEntry]]
  def xRange(key: Key, start: String, end: String): RedisTask[Env, List[StreamEntry]]
  def xRange(key: Key, start: StreamEntryID, end: StreamEntryID, count: Int): RedisTask[Env, List[StreamEntry]]
  def xRange(key: Key, start: String, end: String, count: Int): RedisTask[Env, List[StreamEntry]]

  def xRead(
    params: XReadParams,
    streams: (String, StreamEntryID)*
  ): RedisTask[Env, List[(String, List[(StreamEntryID, Map[String, String])])]]

object RedisOps:
  given Conversion[TTL, Long]                                                           = _.toSeconds.toInt
  given javaBinaryToScalaList: Conversion[JavaList[Binary], List[Binary]]               = _.asScala.toList
  given javaStringToScalaList: Conversion[JavaList[Value], List[Value]]                 = _.asScala.toList
  given javaEntryToScalaList: Conversion[JavaList[StreamEntry], List[StreamEntry]]      = _.asScala.toList
  given javaSetToScalaSet: Conversion[java.util.Set[Value], Set[Value]]                 = _.asScala.toSet
  given kvSeqToJavaMap: Conversion[Seq[(Key, Value)], java.util.Map[String, String]]    = _.toMap.asJava
  given kvJavaMapToScalaMap: Conversion[java.util.Map[String, String], Map[Key, Value]] =
    _.asScala.toMap

  given Conversion[TTL, SetParams]   = new SetParams().nx().ex(_)
  given Conversion[TTL, GetExParams] = new GetExParams().ex(_)

final private[redis] case class Redis private (private val pool: JedisPool) extends DSL[Any]:
  import RedisOps.given

  private def wPool[A](f: Jedis => Task[A]): Task[A] =
    acquireReleaseWith(attempt(pool.getResource))(r => attempt(r.close()).orElse(unit))(f)

  private def redis[A](r: => Jedis => A): Task[A] = wPool(redis => attempt(r(redis)))

  private def redisBlocking[A](r: => Jedis => A): Task[A] = wPool(redis => ZIO.attemptBlocking(r(redis)))

  def get(key: Key): RedisTask[Any, Option[Value]]                       = redis(_.get(key)).map(Option(_))
  def set(key: Key, value: Value): RedisTask[Any, Value]                 = redis(_.set(key, value))
  def del(key: Key): RedisTask[Any, Long]                                = redis(_.del(key))
  def mGet(keys: Key*): RedisTask[Any, List[Value]]                      = redis(_.mget(keys*))
  def hSet(key: Key, values: (Key, Value)*): RedisTask[Any, Long]        = redis(_.hset(key, values.toSeq))
  def hGetAll(key: Key): RedisTask[Any, Map[Key, Value]]                 = redis(_.hgetAll(key))
  def hGet(key: Key, field: Field): RedisTask[Any, Value]                = redis(_.hget(key, field))
  def rPush(key: Key, values: Value*): RedisTask[Any, Long]              = redis(_.rpush(key, values*))
  def lPush(key: Key, values: Value*): RedisTask[Any, Long]              = redis(_.lpush(key, values*))
  def rPop(key: Key): RedisTask[Any, Value]                              = redis(_.rpop(key))
  def lPop(key: Key): RedisTask[Any, Value]                              = redis(_.lpop(key))
  def lPop(key: Key, count: Int): RedisTask[Any, List[Value]]            = redis(_.lpop(key, count))
  def lLen(key: Key): RedisTask[Any, Long]                               = redis(_.llen(key))
  def sAdd(key: Key, members: Value*): RedisTask[Any, Long]              = redis(_.sadd(key, members*))
  def sMembers(key: Key): RedisTask[Any, Set[Value]]                     = redis(_.smembers(key))
  def flushDB: RedisTask[Any, String]                                    = redis(_.flushDB())
  def flushDB(flushMode: FlushMode): RedisTask[Any, String]              = redis(_.flushDB(flushMode))
  def hIncrBy(key: Key, field: Field, value: Long): RedisTask[Any, Long] = redis(_.hincrBy(key, field, value))

  def xAdd(key: Key, streamEntryID: StreamEntryID, values: (Key, Value)*): RedisTask[Any, StreamEntryID] =
    redis(_.xadd(key, streamEntryID, values.toSeq))
  def xAdd(key: Key, params: XAddParams, values: (Key, Value)*): RedisTask[Any, StreamEntryID]           =
    redis(_.xadd(key, params, values.toSeq))

  def xLen(key: Key): RedisTask[Any, Long] = redis(_.xlen(key))

  def xRange(key: Key, start: StreamEntryID, end: StreamEntryID): RedisTask[Any, List[StreamEntry]]             =
    redis(_.xrange(key, start, end))
  def xRange(key: Key, start: String, end: String): RedisTask[Any, List[StreamEntry]]                           =
    redis(_.xrange(key, start, end))
  def xRange(key: Key, start: StreamEntryID, end: StreamEntryID, count: Int): RedisTask[Any, List[StreamEntry]] =
    redis(_.xrange(key, start, end, count))
  def xRange(key: Key, start: String, end: String, count: Int): RedisTask[Any, List[StreamEntry]]               =
    redis(_.xrange(key, start, end, count))

  def xRead(
    params: XReadParams,
    streams: (String, StreamEntryID)*
  ): RedisTask[Any, List[(String, List[(StreamEntryID, Map[String, String])])]] =
    redis(_.xread(params, streams.toMap.asJava)).map {
      case null => Nil
      case b    =>
        b.asScala.toList.map(e =>
          e.getKey -> e.getValue.asScala.toList
            .map(e => e.getID -> e.getFields.toList.toMap)
        )
    }

object Redis extends DSL[Redis]:
  final def get(key: Key): RedisTask[Redis, Option[Value]]                = serviceWithZIO[Redis](_.get(key))
  final def set(key: Key, value: Value): RedisTask[Redis, Value]          = serviceWithZIO[Redis](_.set(key, value))
  final def del(key: Key): RedisTask[Redis, Long]                         = serviceWithZIO[Redis](_.del(key))
  final def mGet(keys: Key*): RedisTask[Redis, List[Value]]               = serviceWithZIO[Redis](_.mGet(keys*))
  final def hSet(key: Key, values: (Key, Value)*): RedisTask[Redis, Long] = serviceWithZIO[Redis](_.hSet(key, values*))
  final def hGetAll(key: Key): RedisTask[Redis, Map[Key, Value]]          = serviceWithZIO[Redis](_.hGetAll(key))
  final def hGet(key: Key, field: Field): RedisTask[Redis, Value]         = serviceWithZIO[Redis](_.hGet(key, field))
  final def rPush(key: Key, values: Value*): RedisTask[Redis, Long]       = serviceWithZIO[Redis](_.rPush(key, values*))
  final def lPush(key: Key, values: Value*): RedisTask[Redis, Long]       = serviceWithZIO[Redis](_.lPush(key, values*))
  final def rPop(key: Key): RedisTask[Redis, String]                      = serviceWithZIO[Redis](_.rPop(key))
  final def lPop(key: Key): RedisTask[Redis, String]                      = serviceWithZIO[Redis](_.lPop(key))
  final def lPop(key: Key, count: Int): RedisTask[Redis, List[String]]    = serviceWithZIO[Redis](_.lPop(key, count))
  final def lLen(key: Key): RedisTask[Redis, Long]                        = serviceWithZIO[Redis](_.lLen(key))
  final def sAdd(key: Key, members: Value*): RedisTask[Redis, Long]       = serviceWithZIO[Redis](_.sAdd(key, members*))
  final def sMembers(key: Key): RedisTask[Redis, Set[Value]]              = serviceWithZIO[Redis](_.sMembers(key))
  final def flushDB: RedisTask[Redis, Key]                                = serviceWithZIO[Redis](_.flushDB)
  final def flushDB(flushMode: FlushMode): RedisTask[Redis, Key]          = serviceWithZIO[Redis](_.flushDB(flushMode))
  final def hIncrBy(
    key: Key,
    field: Field,
    value: Long
  ): RedisTask[Redis, Long] = serviceWithZIO[Redis](_.hIncrBy(key, field, value))

  final def xAdd(
    key: Key,
    streamEntryID: StreamEntryID,
    values: (Key, Value)*
  ): RedisTask[Redis, StreamEntryID] = serviceWithZIO[Redis](_.xAdd(key, streamEntryID, values*))

  final def xAdd(
    key: Key,
    params: XAddParams,
    values: (Key, Value)*
  ): RedisTask[Redis, StreamEntryID] = serviceWithZIO[Redis](_.xAdd(key, params, values*))

  final def xAdd(
    key: Key,
    values: (Key, Value)*
  ): RedisTask[Redis, StreamEntryID] = serviceWithZIO[Redis](_.xAdd(key, params = XAddParams.xAddParams(), values*))

  final def xLen(key: Key): RedisTask[Redis, Long] = serviceWithZIO[Redis](_.xLen(key))

  final def xRange(key: Key, start: StreamEntryID, end: StreamEntryID): RedisTask[Redis, List[StreamEntry]] =
    serviceWithZIO[Redis](_.xRange(key, start, end))
  final def xRange(key: Key, start: String, end: String): RedisTask[Redis, List[StreamEntry]]               =
    serviceWithZIO[Redis](_.xRange(key, start, end))
  final def xRange(
    key: Key,
    start: StreamEntryID,
    end: StreamEntryID,
    count: Int
  ): RedisTask[Redis, List[StreamEntry]] =
    serviceWithZIO[Redis](_.xRange(key, start, end, count))
  final def xRange(key: Key, start: String, end: String, count: Int): RedisTask[Redis, List[StreamEntry]]   =
    serviceWithZIO[Redis](_.xRange(key, start, end, count))

  final override def xRead(
    params: XReadParams,
    streams: (Key, StreamEntryID)*
  ): RedisTask[Redis, List[(Key, List[(StreamEntryID, Map[Key, Key])])]] =
    serviceWithZIO[Redis](_.xRead(params, streams*))

  final def liveFromURI(
    uri: RedisURI,
    poolConfig: Option[JedisPoolConfig] = None
  ): TaskLayer[Redis] =
    ZLayer.scoped:
      Pool.fromConfigZIO(uri, poolConfig).map(Redis(_))

  final def liveUnscopedFromURI(
    uri: RedisURI,
    poolConfig: Option[JedisPoolConfig] = None
  ): ZLayer[Scope, Throwable, Redis] =
    ZLayer.fromZIO:
      Pool.fromConfigZIO(uri, poolConfig).map(Redis(_))

  final def live: ZLayer[Scope & JedisPool, Throwable, Redis] =
    ZLayer.fromZIO:
      ZIO.serviceWith[JedisPool](Redis(_))
