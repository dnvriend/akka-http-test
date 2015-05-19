package com.github.dnvriend

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

trait TimeClient extends Marshallers {
  implicit def system: ActorSystem
  implicit def ec: ExecutionContextExecutor
  implicit def materializer: FlowMaterializer
  implicit def log: LoggingAdapter

  lazy val openWeatherApiFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection("api.openweathermap.org", 80)

  def weatherRequest(request: HttpRequest): Future[HttpResponse] =
    Source.single(request)
      .log("request")
      .via(openWeatherApiFlow)
      .log("response")
      .runWith(Sink.head)

  def getDate: Future[WeatherResult] = {
    weatherRequest(RequestBuilding.Get("/data/2.5/weather?zip=1313,nl")).flatMap { (response: HttpResponse) =>
      response.status match {
        case OK => Unmarshal(response.entity).to[WeatherResult]
      }
    }
  }
}

object SimpleClient extends App with TimeClient with CoreServices {
  println(Await.result(getDate, 10.seconds))
  system.shutdown()
}
