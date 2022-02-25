import Model.{Car, CarCreateData, Manufactor}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Future
class DAO(db: Database) {

  def getManufactors(ids: Seq[Int]): Future[Seq[Manufactor]] = db.run(
    DBSchema.Manufactors.filter(_.id inSet ids).result
  )

  def getCars(ids: Seq[Int]): Future[Seq[Car]] = db.run(
    DBSchema.Cars.filter(_.id inSet ids).result
  )

  def getCarsByManufactorIds(ids: Seq[Int]): Future[Seq[Car]] = db.run(
    DBSchema.Cars.filter(_.manufactorId inSet ids).result
  )

  def createCar(data: CarCreateData): Future[Car] = {
    val newCar = Car(0, data.model, data.year, data.manufactorId)

    val insertAndReturnCarQuery = (DBSchema.Cars returning DBSchema.Cars.map(_.id)) into {
      (car, id) => car.copy(id = id)
    }

    db.run {
      insertAndReturnCarQuery += newCar
    }

  }

}
