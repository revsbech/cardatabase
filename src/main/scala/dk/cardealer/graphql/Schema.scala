package dk.cardealer.graphql

import akka.http.scaladsl.model.DateTime
import dk.cardealer.CarEnvironment
import dk.cardealer.Model.{IntIdentifiable}
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, Relation, RelationIds}
import sangria.schema.{Argument, Field, InputObjectType, IntType, InterfaceType, ListInputType, ListType, ObjectType, OptionType, ScalarType, Schema, StringType, fields, interfaces}
import sangria.validation.Violation

object CarSchema {
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
  val IntId = Argument("id", IntType)
  val IntIdList = Argument("ids", ListInputType(IntType))

  val schema = Schema(
    ObjectType(
      "Query",
      fields[CarEnvironment, Unit](
        Cars.getCar,
        Manufactors.getManufactor,
        Manufactors.listManufactorsByIds
      )
    ),
    Some(
      ObjectType(
        "Mutation",
        fields[CarEnvironment, Unit](
          Cars.createCar
        )
      )
    )
  )

  val Resolver = DeferredResolver.fetchers(
    Cars.carfetcher,
    Manufactors.manufactorfetcer
  )
}
