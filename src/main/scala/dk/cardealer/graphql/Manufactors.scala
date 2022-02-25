package dk.cardealer.graphql

import dk.cardealer.CarEnvironment
import dk.cardealer.Model.{Manufactor => ManufactorModel}
import dk.cardealer.graphql.CarSchema.{GraphQLDateTime, IntId, IntIdList, IntIdentifiableType}
import sangria.execution.deferred.Fetcher
import sangria.macros.derive.{Interfaces, ReplaceField, deriveObjectType}
import sangria.schema.{Field, ListType, ObjectType, OptionType}

object Manufactors {

  val ManufactorModelType = {
    deriveObjectType[Unit, ManufactorModel](
      ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
      , Interfaces(IntIdentifiableType)
    )
  }

  val getManufactorField: Field[CarEnvironment, Unit] = {
    Field("get", OptionType(ManufactorModelType),
      description = Some("Return a Manufactorer with specific id."),
      arguments = IntId :: Nil,
      resolve = c => manufactorfetcer.deferOpt(c.arg[Int]("id"))
    )
  }

  val listManufactorsByIdsField: Field[CarEnvironment, Unit] = {
    Field("list",
      ListType(ManufactorModelType),
      arguments = IntIdList :: Nil,
      description = Some("Returns a list of all available manufactors"),
      resolve = c => manufactorfetcer.deferSeq(c.arg("ids"))
    )
  }

  val ManufactorsType: ObjectType[CarEnvironment, Unit] = {
    ObjectType(
      "Manufactors",
      "Grouping of function for getting manufactors",
      fields = List(getManufactorField, listManufactorsByIdsField)
    )
  }
  val queryField: Field[CarEnvironment, Unit] = Field("manufactors", ManufactorsType, resolve = _ => ())

  val manufactorfetcer = Fetcher(
    (ctx: CarEnvironment, ids: Seq[Int]) => ctx.dao.getManufactors(ids)
  )

}
