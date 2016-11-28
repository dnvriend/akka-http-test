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

package com.github.dnvriend.component.webservices.weather

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.github.dnvriend.component.webservices.generic.HttpClient
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

case class Wind(speed: Double, deg: Double)
case class Main(temp: Double, temp_min: Double, temp_max: Double, pressure: Double, sea_level: Option[Double], grnd_level: Option[Double], humidity: Int)
case class Cloud(all: Int)
case class Weather(id: Int, main: String, description: String, icon: String)
case class Sys(message: Double, country: String, sunrise: Long, sunset: Long)
case class Coord(lon: Double, lat: Double)
case class WeatherResult(coord: Coord, sys: Sys, weather: List[Weather], base: String, main: Main, wind: Wind, clouds: Cloud, dt: Long, id: Int, name: String, cod: Int)

trait Marshallers extends DefaultJsonProtocol {
  implicit val windJsonFormat = jsonFormat2(Wind)
  implicit val mainJsonFormat = jsonFormat7(Main)
  implicit val cloudJsonFormat = jsonFormat1(Cloud)
  implicit val weatherJsonFormat = jsonFormat4(Weather)
  implicit val sysJsonFormat = jsonFormat4(Sys)
  implicit val coordJsonFormat = jsonFormat2(Coord)
  implicit val weatherResultJsonFormat = jsonFormat11(WeatherResult)
}

case class GetWeatherRequest(zip: String, country: String)

trait OpenWeatherApi {
  def getWeather(zip: String, country: String): Future[Option[WeatherResult]]

  def getWeather[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(GetWeatherRequest, T), (Option[WeatherResult], T), NotUsed]
}

object OpenWeatherApi {
  import spray.json._
  def apply()(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, log: LoggingAdapter) = new OpenWeatherApiImpl

  def mapResponseToWeatherResult(json: String)(implicit reader: JsonReader[WeatherResult]): Option[WeatherResult] =
    Try(json.parseJson.convertTo[WeatherResult]).toOption

  def responseToString(resp: HttpResponse)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Future[String] =
    HttpClient.responseToString(resp)

  def getWeatherRequestFlow[T]: Flow[(GetWeatherRequest, T), (HttpRequest, T), NotUsed] =
    Flow[(GetWeatherRequest, T)].map { case (request, id) => (HttpClient.mkGetRequest(s"/data/2.5/weather?zip=${request.zip},${request.country}"), id) }

  def mapResponseToWeatherResultFlow[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, reader: JsonReader[WeatherResult]): Flow[(Try[HttpResponse], T), (Option[WeatherResult], T), NotUsed] =
    HttpClient.responseToString[T].map { case (json, id) => (mapResponseToWeatherResult(json), id) }
}

class OpenWeatherApiImpl()(implicit val system: ActorSystem, val ec: ExecutionContext, val mat: Materializer, val log: LoggingAdapter) extends OpenWeatherApi with Marshallers {
  import OpenWeatherApi._

  private val client = HttpClient("weather")

  override def getWeather(zip: String, country: String): Future[Option[WeatherResult]] =
    client.get(s"/data/2.5/weather?zip=$zip,$country").
      flatMap(responseToString)
      .map(mapResponseToWeatherResult)

  override def getWeather[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(GetWeatherRequest, T), (Option[WeatherResult], T), NotUsed] =
    getWeatherRequestFlow[T]
      .via(client.cachedHostConnectionFlow[T])
      .via(mapResponseToWeatherResultFlow[T])
}
