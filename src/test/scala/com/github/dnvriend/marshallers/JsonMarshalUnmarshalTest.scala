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

package com.github.dnvriend.marshallers

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.github.dnvriend.{ Marshallers, PersonV1, TestSpec }
import spray.json._

class JsonMarshalUnmarshalTest extends TestSpec with Marshallers {
  val personJson = """{"name":"John Doe","age":25}"""
  "Person" should "be marshalled using spray-json" in {
    PersonV1("John Doe", 25).toJson.compactPrint shouldBe personJson
  }

  it should "be unmarshalled using spray-json" in {
    personJson.parseJson.convertTo[PersonV1] shouldBe PersonV1("John Doe", 25)
  }

  it should "be marshalled / unmarshalled to / from RequestEntity using akka.http.marshalling.Marshal" in {
    val personV1 = PersonV1("John Doe", 25)
    val entity = Marshal(personV1).to[RequestEntity].futureValue
    Unmarshal(entity).to[PersonV1].futureValue shouldBe personV1
  }
}
