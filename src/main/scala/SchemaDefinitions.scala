import akka.http.scaladsl.model.DateTime
import Model._
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, Relation, RelationIds}
import sangria.macros.derive._
import sangria.schema.{Argument, Field, InputObjectType, IntType, InterfaceType, ListInputType, ListType, ObjectType, OptionType, ScalarType, Schema, StringType, fields, interfaces}
import sangria.validation.Violation
import sangria.marshalling.sprayJson._
import spray.json.DefaultJsonProtocol._

object SchemaDefinitions {
  case object DateTimeCoerceViolation extends Violation {
    override def errorMessage: String = "Error during parsing DateTime"
  }

  implicit val GraphQLDateTime = ScalarType[DateTime]( //1
    "DateTime", //2
    coerceOutput = (dt: DateTime, _) => dt.toString, //3
    coerceInput = { //4
      case StringValue(dt, _, _, _, _) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    },
    coerceUserInput = { //5
      case s: String => DateTime.fromIsoDateTimeString(s).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    }
  )

  val IntIdentifiableType = InterfaceType(
    "IntegerIdentifiable",
    "Entity that can be identified by an integer",

    fields[Unit, IntIdentifiable](
      Field("id", IntType, resolve = _.value.id)))

  implicit val carCreateDataFormat = jsonFormat3(CarCreateData)
  lazy val CarCreateInputType: InputObjectType[CarCreateData] = deriveInputObjectType[CarCreateData]()

  val Id = Argument("id", StringType)
  val IntId = Argument("id", IntType)
  val IntIdList = Argument("ids", ListInputType(IntType))

  val ManufactorType =
    deriveObjectType[Unit, Manufactor](
      ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
      , Interfaces(IntIdentifiableType)
    )

  val CarType = ObjectType[Unit, Car](
    "car",
    interfaces[Unit, Car](IntIdentifiableType),
    fields[Unit, Car](
      Field("model", StringType, resolve = _.value.model),
      Field("year", IntType, resolve = _.value.year),
      Field("manufactor", ManufactorType, resolve = c => manufactorFetcher.defer(id = c.value.manufactorId)),
    )
  )

  val QueryType = ObjectType("Query", fields[CarEnvironment, Unit](
    Field("car", OptionType(CarType),
      description = Some("Returns a car with specific `id`."),
      arguments = List(IntId),
      resolve = c => carFetcher.deferOpt(c.arg[Int]("id"))
    ),
    Field("manufactor", OptionType(ManufactorType),
      description = Some("Return a Manufactorer with specific id."),
      arguments = IntId :: Nil,
      resolve = c => manufactorFetcher.deferOpt(c.arg[Int]("id"))
    ),
    Field("manufactors",
      ListType(ManufactorType),
      arguments = IntIdList :: Nil,
      description = Some("Returns a list of all available manufactors"),
      resolve = c => manufactorFetcher.deferSeq(c.arg("ids")) // c.ctx.dao.getManufactors(c.arg[Seq[Int]]("ids"))
    )
  ))

  val CarCreateArg = Argument("carData", CarCreateInputType)


  val Mutation = ObjectType(
    "Mutation",
    fields[CarEnvironment, Unit](
      Field("createCar",
        CarType,
        arguments = CarCreateArg :: Nil,
        resolve = c => c.ctx.dao.createCar(c.arg(CarCreateArg))
      )
    )
  )
  val schema = Schema(QueryType, Some(Mutation))

  val carByManufactorRel = Relation[Car, Int]("byManufactor", car => Seq(car.manufactorId))

  // Using a cache and de-duplication Fetcher
  val manufactorFetcher = Fetcher(
    (ctx: CarEnvironment, ids: Seq[Int]) => ctx.dao.getManufactors(ids)
  )

  val carFetcher = Fetcher.rel(
    (ctx: CarEnvironment, ids: Seq[Int]) => ctx.dao.getCars(ids),
    (ctx: CarEnvironment, ids: RelationIds[Car]) => ctx.dao.getCarsByManufactorIds(ids(carByManufactorRel)),
  )

  val Resolver = DeferredResolver.fetchers(carFetcher, manufactorFetcher)

}
