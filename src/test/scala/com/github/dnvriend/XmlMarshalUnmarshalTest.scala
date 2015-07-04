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

import akka.http.scaladsl.marshalling.{ Marshal, Marshaller, Marshalling }
import akka.http.scaladsl.model.HttpCharset
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.unmarshalling.{ Unmarshal, Unmarshaller }

import scala.xml.NodeSeq

class XmlMarshalUnmarshalTest extends TestSpec {

  case class Person(name: String, age: Int)

  val personXml =
    <person>
      <name>John Doe</name>
      <age>25</age>
    </person>

  implicit val personUnmarshaller = Unmarshaller.strict[NodeSeq, Person] { xml ⇒
    Person((xml \\ "name").text, (xml \\ "age").text.toInt)
  }

  val opaquePersonMarshalling = Marshalling.Opaque(() ⇒ personXml)
  val openCharsetPersonMarshalling = Marshalling.WithOpenCharset(`text/xml`, (charset: HttpCharset) ⇒ personXml)
  val fixedCharsetPersonMarshalling = Marshalling.WithFixedCharset(`text/xml`, `UTF-8`, () ⇒ personXml)

  val opaquePersonMarshaller = Marshaller.opaque[Person, NodeSeq] { person ⇒ personXml }
  val withFixedCharsetPersonMarshaller = Marshaller.withFixedCharset[Person, NodeSeq](`text/xml`, `UTF-8`) { person ⇒ personXml }
  val withOpenCharsetCharsetPersonMarshaller = Marshaller.withOpenCharset[Person, NodeSeq](`text/xml`) { (person, charset) ⇒ personXml }

  implicit val personMarshaller = Marshaller.oneOf[Person, NodeSeq](opaquePersonMarshaller, withFixedCharsetPersonMarshaller, withOpenCharsetCharsetPersonMarshaller)

  "personXml" should "be unmarshalled" in {
    Unmarshal(personXml).to[Person].futureValue shouldBe Person("John Doe", 25)
  }

  "Person" should "be marshalled" in {
    Marshal(Person("John Doe", 25)).to[NodeSeq].futureValue shouldBe personXml
  }
}
