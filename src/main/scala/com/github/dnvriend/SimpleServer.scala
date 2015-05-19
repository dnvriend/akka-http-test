package com.github.dnvriend

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._

import scala.concurrent.ExecutionContextExecutor

case class Person(name: String, age: Int)
case class WeatherResult(coord: Coord, sys: Sys, weather: List[Weather], base: String, main: Main, wind: Wind, clouds: Cloud, dt: Long, id: Int, name: String, cod: Int)
case class Wind(speed: Double, deg: Double)
case class Main(temp: Double, temp_min: Double, temp_max: Double, pressure: Double, sea_level: Double, grnd_level: Double, humidity: Int)
case class Cloud(all: Int)
case class Weather(id: Int, main: String, description: String, icon: String)
case class Sys(message: Double, country: String, sunrise: Long, sunset: Long)
case class Coord(lon: Double, lat: Double)
case class Ping(timestamp: String)

trait Service extends Marshallers {
  implicit def system: ActorSystem
  implicit def ec: ExecutionContextExecutor
  implicit def materializer: FlowMaterializer
  implicit def log: LoggingAdapter

  def routes: Flow[HttpRequest, HttpResponse, Unit] =
    logRequestResult("akka-http-test") {
      path("") {
        redirect("person", StatusCodes.PermanentRedirect)
      } ~
      pathPrefix("person") {
        complete {
          Person("John Doe", 25)
        }
      } ~
      pathPrefix("ping") {
        complete {
          Ping(TimeUtil.timestamp)
        }
      }
    }
}

object SimpleServer extends App with Service with CoreServices {
  Http().bindAndHandle(routes, "0.0.0.0", 8080)
}
