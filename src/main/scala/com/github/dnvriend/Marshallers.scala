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
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.github.dnvriend.domain.Person
import spray.json.{ DefaultJsonProtocol, _ }

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

/**
 * See: http://liddellj.com/using-media-type-parameters-to-version-an-http-api/
 *
 * As a rule, when requesting `application/json` or `application/xml` you should return the latest version and should
 * be the same as the latest vendor media type.
 *
 * When a client requests a representation, using the vendor specific media type which includes a version, the API should
 * return that representation
 */
object MediaVersionTypes {
  val `application/vnd.acme.v1+json` = MediaType.custom("application/vnd.acme.v1+json", MediaType.Encoding.Fixed(HttpCharsets.`UTF-8`))
  val `application/vnd.acme.v2+json` = MediaType.custom("application/vnd.acme.v2+json", MediaType.Encoding.Fixed(HttpCharsets.`UTF-8`))
  val `application/vnd.acme.v1+xml` = MediaType.custom("application/vnd.acme.v1+xml", MediaType.Encoding.Fixed(HttpCharsets.`UTF-8`))
  val `application/vnd.acme.v2+xml` = MediaType.custom("application/vnd.acme.v2+xml", MediaType.Encoding.Fixed(HttpCharsets.`UTF-8`))
}

// marshalling containers (Value Objects)
case class Ping(timestamp: String)
case class PersonV1(name: String, age: Int)
case class PersonV2(name: String, age: Int, married: Boolean)

trait Marshallers extends DefaultJsonProtocol with SprayJsonSupport with ScalaXmlSupport {
  implicit def ec: ExecutionContext

  implicit val personJsonFormatV1 = jsonFormat2(PersonV1)
  implicit val personJsonFormatV2 = jsonFormat3(PersonV2)

  implicit val pingJsonFormat = jsonFormat1(Ping)

  def marshalPersonXmlV2(person: PersonV2): NodeSeq =
    <person>
      <name>{ person.name }</name>
      <age>{ person.age }</age>
      <married>{ person.married }</married>
    </person>

  def marshalPersonsXmlV2(persons: Iterable[PersonV2]) =
    <persons>
      { persons.map(marshalPersonXmlV2) }
    </persons>

  def marshalPersonXmlV1(person: PersonV1): NodeSeq =
    <person>
      <name>{ person.name }</name>
      <age>{ person.age }</age>
    </person>

  def marshalPersonsXmlV1(persons: Iterable[PersonV1]) =
    <persons>
      { persons.map(marshalPersonXmlV1) }
    </persons>

  implicit def personsXmlFormatV1 = Marshaller.opaque[Iterable[PersonV1], NodeSeq](marshalPersonsXmlV1)

  implicit def personXmlFormatV1 = Marshaller.opaque[PersonV1, NodeSeq](marshalPersonXmlV1)

  implicit def personsXmlFormatV2 = Marshaller.opaque[Iterable[PersonV2], NodeSeq](marshalPersonsXmlV2)

  implicit def personXmlFormatV2 = Marshaller.opaque[PersonV2, NodeSeq](marshalPersonXmlV2)

  /**
   * From the Iterable[Person] value-object convert to a version and then marshal, wrap in an entity;
   * communicate with the VO in the API
   */
  implicit def personsMarshaller: ToResponseMarshaller[Iterable[Person]] = Marshaller.oneOf(
    Marshaller.withFixedCharset(MediaTypes.`application/json`, HttpCharsets.`UTF-8`) { persons ⇒
      HttpResponse(entity =
        HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), persons.map(person ⇒ PersonV2(person.name, person.age, person.married)).toJson.compactPrint)
      )
    },
    Marshaller.withFixedCharset(MediaVersionTypes.`application/vnd.acme.v1+json`, HttpCharsets.`UTF-8`) { persons ⇒
      HttpResponse(entity =
        HttpEntity(ContentType(MediaVersionTypes.`application/vnd.acme.v1+json`, HttpCharsets.`UTF-8`), persons.map(person ⇒ PersonV1(person.name, person.age)).toJson.compactPrint)
      )
    },
    Marshaller.withFixedCharset(MediaVersionTypes.`application/vnd.acme.v2+json`, HttpCharsets.`UTF-8`) { persons ⇒
      HttpResponse(entity =
        HttpEntity(ContentType(MediaVersionTypes.`application/vnd.acme.v2+json`, HttpCharsets.`UTF-8`), persons.map(person ⇒ PersonV2(person.name, person.age, person.married)).toJson.compactPrint)
      )
    },
    Marshaller.withFixedCharset(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`) { persons ⇒
      HttpResponse(entity =
        HttpEntity.CloseDelimited(ContentType(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`),
          Source(Marshal(persons.map(person ⇒ PersonV2(person.name, person.age, person.married))).to[NodeSeq])
            .map(ns ⇒ ByteString(ns.toString()))
        )
      )
    },
    Marshaller.withFixedCharset(MediaVersionTypes.`application/vnd.acme.v1+xml`, HttpCharsets.`UTF-8`) { persons ⇒
      HttpResponse(entity =
        HttpEntity.CloseDelimited(ContentType(MediaVersionTypes.`application/vnd.acme.v1+xml`, HttpCharsets.`UTF-8`),
          Source(Marshal(persons.map(person ⇒ PersonV1(person.name, person.age))).to[NodeSeq])
            .map(ns ⇒ ByteString(ns.toString()))
        )
      )
    },
    Marshaller.withFixedCharset(MediaVersionTypes.`application/vnd.acme.v2+xml`, HttpCharsets.`UTF-8`) { persons ⇒
      HttpResponse(entity =
        HttpEntity.CloseDelimited(ContentType(MediaVersionTypes.`application/vnd.acme.v2+xml`, HttpCharsets.`UTF-8`),
          Source(Marshal(persons.map(person ⇒ PersonV2(person.name, person.age, person.married))).to[NodeSeq])
            .map(ns ⇒ ByteString(ns.toString()))
        )
      )
    }
  )

  /**
   * From the Person value-object convert to a version and then marshal, wrap in an entity;
   * communicate with the VO in the API
   */
  implicit def personMarshaller: ToResponseMarshaller[Person] = Marshaller.oneOf(
    Marshaller.withFixedCharset(MediaTypes.`application/json`, HttpCharsets.`UTF-8`) { person ⇒
      HttpResponse(entity =
        HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), PersonV2(person.name, person.age, person.married).toJson.compactPrint)
      )
    },
    Marshaller.withFixedCharset(MediaVersionTypes.`application/vnd.acme.v1+json`, HttpCharsets.`UTF-8`) { person ⇒
      HttpResponse(entity =
        HttpEntity(ContentType(MediaVersionTypes.`application/vnd.acme.v1+json`, HttpCharsets.`UTF-8`), PersonV1(person.name, person.age).toJson.compactPrint)
      )
    },
    Marshaller.withFixedCharset(MediaVersionTypes.`application/vnd.acme.v2+json`, HttpCharsets.`UTF-8`) { person ⇒
      HttpResponse(entity =
        HttpEntity(ContentType(MediaVersionTypes.`application/vnd.acme.v2+json`, HttpCharsets.`UTF-8`), PersonV2(person.name, person.age, person.married).toJson.compactPrint)
      )
    },
    Marshaller.withFixedCharset(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`) { person ⇒
      HttpResponse(entity =
        HttpEntity.CloseDelimited(ContentType(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`),
          Source(Marshal(PersonV2(person.name, person.age, person.married)).to[NodeSeq])
            .map(ns ⇒ ByteString(ns.toString()))
        )
      )
    },
    Marshaller.withFixedCharset(MediaVersionTypes.`application/vnd.acme.v1+xml`, HttpCharsets.`UTF-8`) { person ⇒
      HttpResponse(entity =
        HttpEntity.CloseDelimited(ContentType(MediaVersionTypes.`application/vnd.acme.v1+xml`, HttpCharsets.`UTF-8`),
          Source(Marshal(PersonV1(person.name, person.age)).to[NodeSeq])
            .map(ns ⇒ ByteString(ns.toString()))
        )
      )
    },
    Marshaller.withFixedCharset(MediaVersionTypes.`application/vnd.acme.v2+xml`, HttpCharsets.`UTF-8`) { person ⇒
      HttpResponse(entity =
        HttpEntity.CloseDelimited(ContentType(MediaVersionTypes.`application/vnd.acme.v2+xml`, HttpCharsets.`UTF-8`),
          Source(Marshal(PersonV2(person.name, person.age, person.married)).to[NodeSeq])
            .map(ns ⇒ ByteString(ns.toString()))
        )
      )
    }
  )

}
