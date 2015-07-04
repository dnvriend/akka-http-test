/*
 * Copyright 2015 Dennis Vriend
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
package weatherclient

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.stream.Materializer
import com.github.dnvriend.generic.HttpClient

import scala.concurrent.{ Await, ExecutionContext, Future }

case class Wind(speed: Double, deg: Double)
case class Main(temp: Double, temp_min: Double, temp_max: Double, pressure: Double, sea_level: Option[Double], grnd_level: Option[Double], humidity: Int)
case class Cloud(all: Int)
case class Weather(id: Int, main: String, description: String, icon: String)
case class Sys(message: Double, country: String, sunrise: Long, sunset: Long)
case class Coord(lon: Double, lat: Double)
case class WeatherResult(coord: Coord, sys: Sys, weather: List[Weather], base: String, main: Main, wind: Wind, clouds: Cloud, dt: Long, id: Int, name: String, cod: Int)

trait OpenWeatherApi {
  def getWeather: Future[WeatherResult]
}

object WeatherClient {
  def apply()(implicit system: ActorSystem, mat: Materializer) = new OpenWeatherApiImpl
}

object OpenWeatherClientApp extends App with CoreServices {
  import scala.concurrent.duration._
  val result = Await.result(WeatherClient().getWeather, 10.seconds)
  println(result)
}

class OpenWeatherApiImpl()(implicit val system: ActorSystem, val mat: Materializer) extends OpenWeatherApi with Marshallers with HttpClient {
  override def name: String = "weather"

  override implicit val ec: ExecutionContext = system.dispatcher

  override implicit val log: LoggingAdapter = Logging(system, this.getClass)

  override def getWeather: Future[WeatherResult] =
    get("/data/2.5/weather?zip=1313,nl") map to[WeatherResult]
}
