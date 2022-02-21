import slick.jdbc.H2Profile.api._
import scala.concurrent.Future
import Model._
class DAO(db: Database) {

  def getManufactors(ids: Seq[Int]) = db.run(
    DBSchema.Manufactors.filter(_.id inSet ids).result
  )

}
