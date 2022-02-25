import akka.http.scaladsl.model.DateTime
import sangria.execution.deferred.HasId

package object Model {
  trait IntIdentifiable {
    def id: Int
  }

  object IntIdentifiable {
    implicit def hasId[T <: IntIdentifiable]: HasId[T, Int] = HasId(_.id)
  }

  case class Car(id: Int, model: String, year: Int, manufactorId: Int) extends IntIdentifiable
  case class Manufactor(id: Int, name: String, createdAt: DateTime) extends IntIdentifiable
  case class CarCreateData(model: String, year: Int, manufactorId: Int)

}
