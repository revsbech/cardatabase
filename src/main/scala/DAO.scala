import slick.jdbc.H2Profile.api._
import scala.concurrent.Future
import Model._
class DAO(db: Database) {
  def getManufactor(id: Int): Future[Option[Manufactor]] = db.run(
    //Links.filter(_.id === id).result.headOption
    DBSchema.Manufactors.filter(_.id === id).result.headOption
  )

  def getManufactors(ids: Seq[Int]) = db.run(
    DBSchema.Manufactors.filter(_.id inSet ids).result
  )

}
