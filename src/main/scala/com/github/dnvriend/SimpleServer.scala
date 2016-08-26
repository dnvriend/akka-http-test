/*
 * Copyright 2016 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dnvriend

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl._
import akka.http.scaladsl.common.{ CsvEntityStreamingSupport, EntityStreamingSupport, JsonEntityStreamingSupport }
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{ Directives, Route }
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.stream.scaladsl.{ Flow, Source }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.util.ByteString
import com.github.dnvriend.domain.Person
import com.github.dnvriend.util.TimeUtil
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ ExecutionContext, Future }

class PersonDao {
  def persons(numberOfPersons: Int): Source[Person, NotUsed] =
    Source.repeat(Person("foo", 1)).zipWith(Source.fromIterator(() => Iterator from 0)) {
      case (p, i) => p.copy(
        name = if (i % 10 == 0) "baz-" + i else if (i % 2 == 0) "foo-" + i else "bar-" + i,
        age = i,
        married = i % 2 == 0
      )
    }.take(numberOfPersons)

  def listOfPersons(numberOfPersons: Int): Seq[Person] = (0 to numberOfPersons).map { i =>
    Person(
      name = if (i % 10 == 0) "baz-" + i else if (i % 2 == 0) "foo-" + i else "bar-" + i,
      age = i,
      married = i % 2 == 0
    )
  }

  def personAsync: Future[Person] = Future.successful(personSync)

  def personSync: Person = Person("John Doe", 25)
}

object CsvStreamingRoute extends Directives with SprayJsonSupport with DefaultJsonProtocol {
  implicit val personAsCsv = Marshaller.strict[Person, ByteString] { person =>
    Marshalling.WithFixedContentType(ContentTypes.`text/csv(UTF-8)`, () => {
      ByteString(List(person.name.replace(",", "."), person.age, person.married).mkString(","))
    })
  }

  implicit val csvStreamingSupport: CsvEntityStreamingSupport = EntityStreamingSupport.csv()

  def route(dao: PersonDao): Route =
    path("stream" / IntNumber) { numberOfPersons =>
      pathEnd {
        get {
          complete(dao.persons(numberOfPersons))
        }
      }
    }
}

object JsonStreamingRoute extends Directives with SprayJsonSupport with DefaultJsonProtocol {
  val start = ByteString.empty
  val sep = ByteString("\n")
  val end = ByteString.empty

  implicit val personJsonFormat = jsonFormat3(Person)

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()
    .withFramingRenderer(Flow[ByteString].intersperse(start, sep, end))
    .withParallelMarshalling(parallelism = 8, unordered = true)

  def route(dao: PersonDao): Route =
    path("stream" / IntNumber) { numberOfPersons =>
      pathEnd {
        get {
          complete(dao.persons(numberOfPersons))
        }
      }
    }
}

object SimpleServerRestRoutes extends Directives {
  def routes(dao: PersonDao)(implicit
    um1: FromRequestUnmarshaller[Person],
    m1: ToResponseMarshaller[Person],
    m3: ToResponseMarshaller[Seq[Person]],
    m4: ToResponseMarshaller[Ping],
    log: LoggingAdapter): Route =

    logRequestResult("akka-http-test") {
      pathPrefix("person") {
        path("sync") {
          get {
            complete(dao.personSync)
          }
        } ~
          path("async") {
            get {
              complete(dao.personAsync)
            }
          } ~
          pathEnd {
            get {
              complete(dao.personSync)
            }
          } ~
          (post & entity(as[Person])) { person =>
            complete(StatusCodes.Created)
          }
      } ~ pathPrefix("persons") {
        pathPrefix("strict" / IntNumber) { numberOfPersons =>
          pathEnd {
            get {
              complete(dao.listOfPersons(numberOfPersons))
            }
          }
        } ~ JsonStreamingRoute.route(dao) ~ CsvStreamingRoute.route(dao)
      } ~ pathPrefix("ping") {
        get {
          complete(Ping(TimeUtil.timestamp))
        }
      }
    }
}

object SimpleServer extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val log: LoggingAdapter = Logging(system, this.getClass)

  sys.addShutdownHook {
    system.terminate()
  }

  import Marshallers._
  Http().bindAndHandle(SimpleServerRestRoutes.routes(new PersonDao), "0.0.0.0", 8080)
}
