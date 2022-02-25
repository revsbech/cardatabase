package dk.cardealer.graphql

import dk.cardealer.CarEnvironment
import dk.cardealer.Model.{CarCreateData, Car => CarModel}
import dk.cardealer.graphql.CarSchema.{IntId, IntIdentifiableType}
import sangria.execution.deferred.{Fetcher, Relation, RelationIds}
import sangria.macros.derive.deriveInputObjectType
import sangria.schema.{Argument, Field, InputObjectType, IntType, ObjectType, OptionType, StringType, fields, interfaces}
import spray.json.DefaultJsonProtocol.jsonFormat3
import sangria.marshalling.sprayJson._
import spray.json.DefaultJsonProtocol._

object Cars {

  val cartype: ObjectType[Unit, CarModel] = ObjectType[Unit, CarModel](
    "car",
    interfaces[Unit, CarModel](IntIdentifiableType),
    fields[Unit, CarModel](
      Field("model", StringType, resolve = _.value.model),
      Field("year", IntType, resolve = _.value.year),
      Field("manufactor", Manufactors.manufactorType, resolve = c => Manufactors.manufactorfetcer.defer(id = c.value.manufactorId)),
    )
  )
  lazy val getCar: Field[CarEnvironment, Unit] = {
    Field("getCar", OptionType(cartype),
      description = Some("Returns a car with specific `id`."),
      arguments = List(IntId),
      resolve = c => carfetcher.deferOpt(c.arg[Int]("id"))
    )
  }

  implicit val carCreateDataFormat = jsonFormat3(CarCreateData)
  lazy val CarCreateInputType: InputObjectType[CarCreateData] = deriveInputObjectType[CarCreateData]()
  val CarCreateArg = Argument("carData", CarCreateInputType)
  lazy val createCar: Field[CarEnvironment, Unit] = (
    Field("createCar",
      cartype,
      arguments = CarCreateArg :: Nil,
      resolve = c => c.ctx.dao.createCar(c.arg(CarCreateArg))
    )
  )

  val carByManufactorRel = Relation[CarModel, Int]("byManufactor", car => Seq(car.manufactorId))
  val carfetcher = Fetcher.rel(
    (ctx: CarEnvironment, ids: Seq[Int]) => ctx.dao.getCars(ids),
    (ctx: CarEnvironment, ids: RelationIds[CarModel]) => ctx.dao.getCarsByManufactorIds(ids(carByManufactorRel)),
  )

}
