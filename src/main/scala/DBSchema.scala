import akka.http.scaladsl.model.DateTime
import Model.{Car, Manufactor}
import slick.jdbc.H2Profile.MappedColumnType
import slick.jdbc.H2Profile.api._
import java.sql.Timestamp
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps

object DBSchema {

  //and at the begining of the class' body:
  implicit val dateTimeColumnType = MappedColumnType.base[DateTime, Timestamp](
    dt => new Timestamp(dt.clicks),
    ts => DateTime(ts.getTime)
  )

  /**
   * CarsTable
   *
   * @param tag
   */
  class CarTable(tag: Tag) extends Table[Car](tag, "car") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def model = column[String]("model")

    def year = column[Int]("year")

    def manufactorId = column[Int]("manufactor_id")

    def manufactorFK = foreignKey("manufactor_FK", manufactorId, Manufactors)(_.id)

    def * = (id, model, year, manufactorId).mapTo[Car]
  }

  val Cars = TableQuery[CarTable]

  /**
   * ManufactorsTable
   *
   * @param tag
   */
  class ManufactorsTable(tag: Tag) extends Table[Manufactor](tag, "manufactor") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def createdAt = column[DateTime]("created_at")

    def * = (id, name, createdAt).mapTo[Manufactor]
  }

  val Manufactors = TableQuery[ManufactorsTable]

  /**
   * Load schema and populate sample data withing this Sequence od DBActions
   */
  val databaseSetup = DBIO.seq(
    Manufactors.schema.create,

    Manufactors forceInsertAll Seq(
      Manufactor(1, "Ford", DateTime(1926, 10, 1)),
      Manufactor(2, "GM", DateTime(1942, 10, 1)),
      Manufactor(3, "Triumph", DateTime(1962, 10, 1))
    ),

    Cars.schema.create,
    Cars forceInsertAll Seq(
      Car(1, "Fiesta", 2016, 1),
      Car(2, "F150", 1967, 1),
      Car(3, "Spitfire MKIII", 1967, 3)
    )

  )

  def createDatabase: DAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new DAO(db)

  }

}
