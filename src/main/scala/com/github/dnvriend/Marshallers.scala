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
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import com.github.dnvriend.weatherclient._
import spray.json.DefaultJsonProtocol

import scala.xml.NodeSeq

trait Marshallers extends DefaultJsonProtocol with SprayJsonSupport with ScalaXmlSupport {

  implicit val personJsonFormat = jsonFormat2(Person)
  implicit val pingJsonFormat = jsonFormat1(Ping)

  implicit val windJsonFormat = jsonFormat2(Wind)
  implicit val mainJsonFormat = jsonFormat7(Main)
  implicit val cloudJsonFormat = jsonFormat1(Cloud)
  implicit val weatherJsonFormat = jsonFormat4(Weather)
  implicit val sysJsonFormat = jsonFormat4(Sys)
  implicit val coordJsonFormat = jsonFormat2(Coord)
  implicit val weatherResultJsonFormat = jsonFormat11(WeatherResult)

  implicit val personMarshaller = Marshaller.strict[Person, NodeSeq] { person ⇒
    Marshalling.WithFixedCharset(`text/xml`, `UTF-8`, () ⇒
      <person>
        <name>{ person.name }</name>
        <age>{ person.age }</age>
      </person>
    )
  }
}
