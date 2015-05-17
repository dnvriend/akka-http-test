package com.github.dnvriend

import java.io.IOException

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.javadsl.model.ResponseEntity
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration._

trait TimeClient extends Marshallers {
  implicit def system: ActorSystem
  implicit def ec: ExecutionContextExecutor
  implicit def materializer: FlowMaterializer
  implicit def log: LoggingAdapter

  lazy val dateJsonTestConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection("api.openweathermap.org", 80)

  def header: HttpHeader = HttpHeader.parse("content-type", "application/json") match {
    case ParsingResult.Ok(header, errors) => header
    case _ => throw new RuntimeException("???")
  }

  def fixContentType = Flow[HttpResponse].map { x => x.copy(headers = x.headers.filterNot(_.lowercaseName() == "content-type") :+ header) }

  def dateRequest(request: HttpRequest): Future[HttpResponse] =
    Source.single(request)
      .log("request")
      .via(dateJsonTestConnectionFlow)
      .log("response")
      .via(fixContentType)
      .runWith(Sink.head)

  def getDate: Future[Either[String, String]] = {
    dateRequest(RequestBuilding.Get("/data/2.5/weather?zip=1313,nl")).flatMap { (response: HttpResponse) =>
      response.status match {
        case OK => Unmarshal(response.entity).to[String].map(Right(_))
        case BadRequest => Future.successful(Left(s"Incorrect Json format"))
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"Request failed with status code ${response.status} and entity $entity"
          log.error(error)
          Future.failed(new IOException(error))
        }
      }
    }
  }
}

object SimpleClient extends App with TimeClient with CoreServices {
  Await.ready(getDate.map(println), 10.seconds)
  system.shutdown()
}
