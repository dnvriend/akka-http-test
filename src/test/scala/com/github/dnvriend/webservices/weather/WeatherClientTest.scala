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

package com.github.dnvriend.webservices.weather

import akka.stream.scaladsl.Source
import com.github.dnvriend.TestSpec

class WeatherClientTest extends TestSpec with Marshallers {
  import spray.json._

  val weatherJson = """{
       "coord":{"lon":5.21,"lat":52.37},
       "sys":{"message":0.2418,"country":"NL","sunrise":1431833997,"sunset":1431891067},
       "weather":[{"id":802,"main":"Clouds","description":"scattered clouds","icon":"03d"}],
       "base":"stations",
       "main":{"temp":287.345,"temp_min":287.345,"temp_max":287.345,"pressure":1036.1,"sea_level":1037,"grnd_level":1036.1,"humidity":72},
       "wind":{"speed":6.17,"deg":276.5},
       "clouds":{"all":32},
       "dt":1431869166,
       "id":0,
       "name":"Kruidenwijk, Staatsliedenwijk",
       "cod":200
       }"""

  "WeatherResult" should "be unmarshalled" in {
    weatherJson.parseJson.convertTo[WeatherResult]
  }

  "Weatherclient" should "get weather result" in {
    OpenWeatherApi().getWeather("1313", "nl").futureValue.value mustBe {
      case WeatherResult(_, Sys(_, "NL", _, _), _, "stations", _, _, _, _, _, "Almere Stad", _) ⇒
    }
  }

  "OpenWeatherApi cached connection" should "GetWeatherResult by zip and country" in {
    Source((1 to 50).map(i ⇒ (GetWeatherRequest("1313", "nl"), i)))
      .via(OpenWeatherApi().getWeather)
      .runFold(0) { case (c, e) ⇒ println(e); c + 1 }
      .futureValue shouldBe 50
  }
}
