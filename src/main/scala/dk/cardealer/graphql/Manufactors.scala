package dk.cardealer.graphql

import dk.cardealer.CarEnvironment
import dk.cardealer.Model.{Manufactor => ManufactorModel}
import dk.cardealer.graphql.CarSchema.{GraphQLDateTime, IntId, IntIdList, IntIdentifiableType}
import sangria.execution.deferred.{Fetcher}
import sangria.macros.derive.{Interfaces, ReplaceField, deriveObjectType}
import sangria.schema.{Field, ListType, OptionType}

object Manufactors {
  val manufactorType = {
    deriveObjectType[Unit, ManufactorModel](
      ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
      , Interfaces(IntIdentifiableType)
    )
  }
  val getManufactor: Field[CarEnvironment, Unit] = {
    Field("getManufactor", OptionType(manufactorType),
      description = Some("Return a Manufactorer with specific id."),
      arguments = IntId :: Nil,
      resolve = c => manufactorfetcer.deferOpt(c.arg[Int]("id"))
    )
  }

  val listManufactorsByIds: Field[CarEnvironment, Unit] = {
    Field("listManufactors",
      ListType(manufactorType),
      arguments = IntIdList :: Nil,
      description = Some("Returns a list of all available manufactors"),
      resolve = c => manufactorfetcer.deferSeq(c.arg("ids"))
    )
  }

  val manufactorfetcer = Fetcher(
    (ctx: CarEnvironment, ids: Seq[Int]) => ctx.dao.getManufactors(ids)
  )
}
