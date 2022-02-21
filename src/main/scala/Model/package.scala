
package object Model {
  trait Identifiable {
    def id: String
  }
  trait IntIdentifiable {
    def id: Int
  }

  case class Picture(width: Int, height: Int, url: Option[String])

  case class Product(id: String, name: String, description: String) extends Identifiable {
    def picture(size: Int): Picture =
      Picture(width = size, height = size, url = Some(s"//cdn.com/$size/$id.jpg"))
  }

  case class Car(id: Int, model: String, year: Int, manufactor: Manufactor) extends IntIdentifiable
  case class Manufactor(id: Int, name: String) extends IntIdentifiable

}
