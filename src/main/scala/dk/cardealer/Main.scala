package dk.cardealer

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK, PermanentRedirect}
import akka.http.scaladsl.server.Directives.{complete, get, optionalHeaderValueByName, path, redirect}
import akka.http.scaladsl.server.Route
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.http.akka.circe.CirceHttpSupport
import sangria.slowlog.SlowLog
import sangria.marshalling.circe._

import scala.concurrent.Await
import scala.util.{Failure, Success}

object Main extends App with CirceHttpSupport {
  implicit val system: ActorSystem = ActorSystem("sangria-server")

  import system.dispatcher

  import scala.concurrent.duration._

  scala.sys.addShutdownHook(() -> shutdown())

  // Initialize DataAccessObject and CarDealerEnvironment
  val carEnv = CarEnvironment(DBSchema.createDatabase)

  val route: Route =
    optionalHeaderValueByName("X-Apollo-Tracing") { tracing =>
      path("graphql") {
        graphQLPlayground ~
          prepareGraphQLRequest {
            case Success(req) =>
              val middleware = if (tracing.isDefined) SlowLog.apolloTracing :: Nil else Nil
              val graphQLResponse = Executor.execute(
                schema = dk.cardealer.graphql.CarSchema.schema,
                queryAst = req.query,
                userContext = carEnv,
                variables = req.variables,
                operationName = req.operationName,
                middleware = middleware,
                deferredResolver = dk.cardealer.graphql.CarSchema.Resolver
              ).map(OK -> _)
                .recover {
                  case error: QueryAnalysisError => BadRequest -> error.resolveError
                  case error: ErrorWithResolver => InternalServerError -> error.resolveError
                }
              complete(graphQLResponse)
            case Failure(preparationError) => complete(BadRequest, formatError(preparationError))
          }
      }
    } ~
      (get & path("/health")) {
        complete("All good!")
      } ~
      (get & pathEndOrSingleSlash) {
        redirect("/graphql", PermanentRedirect)
      }

  val PORT = sys.props.get("http.port").fold(8080)(_.toInt)
  val INTERFACE = "0.0.0.0"
  Http().newServerAt(INTERFACE, PORT).bindFlow(route)


  def shutdown(): Unit = {
    system.terminate()
    Await.result(system.whenTerminated, 30.seconds)
  }

}
