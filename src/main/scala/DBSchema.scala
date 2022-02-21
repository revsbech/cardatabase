import slick.jdbc.H2Profile.api._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps
import Model._

object DBSchema {

  class ManufactorsTable(tag: Tag) extends Table[Manufactor](tag, "manufactor"){

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")

    def * = (id, name).mapTo[Manufactor]

  }

  val Manufactors = TableQuery[ManufactorsTable]

  /**
   * Load schema and populate sample data withing this Sequence od DBActions
   */
  val databaseSetup = DBIO.seq(
    Manufactors.schema.create,
    Manufactors forceInsertAll Seq (
      Manufactor(1, "Ford"),
      Manufactor(2, "GM"),
      Manufactor(3, "Triumph")
    )
  )

  def createDatabase: DAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new DAO(db)

  }

}