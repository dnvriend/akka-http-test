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
import akka.actor.{ Actor, ActorSystem, Props }
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.util.Timeout
import com.github.dnvriend.domain.Person
import spray.json.{ DefaultJsonProtocol, _ }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

final case class PersonWithId(id: Long, name: String, age: Int, married: Boolean)

object LowLevelServer extends App with DefaultJsonProtocol {
  // setting up some machinery
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val log: LoggingAdapter = Logging(system, this.getClass)
  implicit val timeout: Timeout = Timeout(10.seconds)
  implicit val personJsonFormat = jsonFormat3(Person)
  implicit val personWithIdJsonFormat = jsonFormat4(PersonWithId)

  val personDb = system.actorOf(Props[PersonDb])

  def debug(t: Any)(implicit log: LoggingAdapter = null): Unit =
    if (Option(log).isEmpty) println(t) else log.debug(t.toString)

  def http200Okay(req: HttpRequest): HttpResponse =
    HttpResponse(StatusCodes.OK)

  def http200AsyncOkay(req: HttpRequest): Future[HttpResponse] =
    Future(http200Okay(req))

  val http200OkayFlow: Flow[HttpRequest, HttpResponse, NotUsed] = Flow[HttpRequest].map { req =>
    HttpResponse(StatusCodes.OK)
  }

  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().bind(interface = "localhost", port = 8080)

  val binding: Future[Http.ServerBinding] = serverSource.to(Sink.foreach { conn =>
    //    conn.handleWith(http200OkayFlow)
    //    conn.handleWithSyncHandler(http200Okay)
    //    conn.handleWithAsyncHandler(http200AsyncOkay, 8)
    conn.handleWithAsyncHandler(personRequestHandler)
  }).run()

  def personRequestHandler(req: HttpRequest): Future[HttpResponse] = req match {
    case HttpRequest(HttpMethods.GET, Uri.Path("/api/person"), _, _, _) =>
      (personDb ? "findAll").mapTo[List[PersonWithId]]
        .map(xs => HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, xs.toJson.compactPrint)))
    case HttpRequest(HttpMethods.POST, Uri.Path("/api/person"), _, ent, _) => for {
      strictEntity <- ent.toStrict(1.second)
      person <- (personDb ? strictEntity.data.utf8String.parseJson.convertTo[Person]).mapTo[PersonWithId]
    } yield HttpResponse(StatusCodes.OK, entity = person.toJson.compactPrint)
    case req =>
      req.discardEntityBytes()
      Future.successful(HttpResponse(StatusCodes.NotFound))
  }
}

class PersonDb extends Actor {
  override def receive: Receive = database(0, Map.empty)
  def database(id: Int, people: Map[Int, PersonWithId]): Receive = {
    case person: Person =>
      val personWithId = PersonWithId(id + 1, person.name, person.age, person.married)
      context.become(database(id + 1, people + (id + 1 -> personWithId)))
      sender() ! personWithId

    case "findAll" =>
      sender() ! people.values.toList.sortBy(_.id)
  }
}