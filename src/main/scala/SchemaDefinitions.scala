import sangria.schema._
import sangria.execution.deferred.HasId
import sangria.execution.deferred.Fetcher
import sangria.execution.deferred.DeferredResolver
import sangria.macros.derive._
import Model._


object SchemaDefinitions {

  val PictureTypeOld = ObjectType(
    "Picture",
    "The product picture",

    fields[Unit, Picture](
      Field("width", IntType, resolve = _.value.width),
      Field("height", IntType, resolve = _.value.height),
      Field("url", OptionType(StringType),
        description = Some("Picture CDN URL"),
        resolve = _.value.url)))

  /**
   * This will automatically have width and height, and we have overridden the url field
   */
  implicit val PictureType =
    deriveObjectType[Unit, Picture](
      ObjectTypeDescription("The product picture"),
      DocumentField("url", "Picture CDN URL"))

  val IdentifiableType = InterfaceType(
    "Identifiable",
    "Entity that can be identified",

    fields[Unit, Identifiable](
      Field("id", StringType, resolve = _.value.id)))

  val IntIdentifiableType = InterfaceType(
    "IntegerIdentifiable",
    "Entity that can be identified by an integer",

    fields[Unit, IntIdentifiable](
      Field("id", IntType, resolve = _.value.id)))

  val ProductType =
    deriveObjectType[Unit, Product](
      Interfaces(IdentifiableType),
      IncludeMethods("picture"))

  val Id = Argument("id", StringType)
  val IntId = Argument("id", IntType)
  val IntIdList = Argument("ids", ListInputType(IntType))

  val ManufactorType =
    deriveObjectType[Unit, Manufactor](
      Interfaces(IntIdentifiableType))
  implicit val linkHasId = HasId[Manufactor, Int](_.id)


  val QueryType = ObjectType("Query", fields[CarEnvironment, Unit](
    Field("product", OptionType(ProductType),
      description = Some("Returns a product with specific `id`."),
      arguments = Id :: Nil,
      resolve = c => c.ctx.productRepo.product(c arg Id)),

    Field("products", ListType(ProductType),
      description = Some("Returns a list of all available products."),
      resolve = _.ctx.productRepo.products),

    Field("manufactor", OptionType(ManufactorType),
      description = Some("Return a Manufactorer with specific id."),
      arguments = IntId :: Nil,
      resolve = c => carFetcher.deferOpt(c.arg[Int]("id"))
    ),
    Field("manufactors",
      ListType(ManufactorType),
      arguments = IntIdList :: Nil,
      description = Some("Returns a list of all available manufactors"),
      resolve = c => carFetcher.deferSeq(c.arg("ids")) // c.ctx.dao.getManufactors(c.arg[Seq[Int]]("ids"))
    ))
  )
  val schema = Schema(QueryType)

  // Using a cache and de-dpulication Fetcher
  val carFetcher = Fetcher(
    (ctx: CarEnvironment, ids: Seq[Int]) => ctx.dao.getManufactors(ids)
  )

  val Resolver = DeferredResolver.fetchers(carFetcher)

}
