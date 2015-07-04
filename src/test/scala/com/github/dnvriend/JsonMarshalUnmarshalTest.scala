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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.github.dnvriend.weatherclient.WeatherResult
import spray.json._

class JsonMarshalUnmarshalTest extends TestSpec {
  val personJson = """{"name":"John Doe","age":25}"""

  object TestMarshallers extends Marshallers with DefaultJsonProtocol with SprayJsonSupport

  import TestMarshallers._

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

  "Person" should "be marshalled using spray-json" in {
    Person("John Doe", 25).toJson.compactPrint shouldBe personJson
  }

  it should "be unmarshalled using spray-json" in {
    personJson.parseJson.convertTo[Person] shouldBe Person("John Doe", 25)
  }

  it should "be marshalled / unmarshalled to / from RequestEntity using akka.http.marshalling.Marshal" in {
    val person = Person("John Doe", 25)
    val entity = Marshal(person).to[RequestEntity].futureValue
    Unmarshal(entity).to[Person].futureValue shouldBe person
  }
}
