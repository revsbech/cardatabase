import slick.jdbc.H2Profile.api._
import akka.http.scaladsl.model.DateTime
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps
import Model._
import java.sql.Timestamp

object DBSchema {

  //and at the begining of the class' body:
  implicit val dateTimeColumnType = MappedColumnType.base[DateTime, Timestamp](
    dt => new Timestamp(dt.clicks),
    ts => DateTime(ts.getTime)
  )

  class ManufactorsTable(tag: Tag) extends Table[Manufactor](tag, "manufactor"){

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
    Manufactors forceInsertAll Seq (
      Manufactor(1, "Ford", DateTime(1926,10,1) ),
      Manufactor(2, "GM", DateTime(1942,10,1)),
      Manufactor(3, "Triumph", DateTime(1962,10,1))
    )
  )

  def createDatabase: DAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new DAO(db)

  }

}