package com.github.dnvriend

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

trait Marshallers extends DefaultJsonProtocol with SprayJsonSupport with ScalaXmlSupport {
  implicit def ec: ExecutionContext

  implicit val personJsonFormat = jsonFormat2(Person)
  implicit val pingJsonFormat = jsonFormat1(Ping)

  implicit val windJsonFormat = jsonFormat2(Wind)
  implicit val mainJsonFormat = jsonFormat7(Main)
  implicit val cloudJsonFormat = jsonFormat1(Cloud)
  implicit val weatherJsonFormat = jsonFormat4(Weather)
  implicit val sysJsonFormat = jsonFormat4(Sys)
  implicit val coordJsonFormat = jsonFormat2(Coord)
  implicit val weatherResultJsonFormat = jsonFormat11(WeatherResult)

  implicit val personMarshaller = Marshaller.strict[Person, NodeSeq] { person =>
    Marshalling.WithFixedCharset(`text/xml`, `UTF-8`, () =>
      <person>
        <name>{ person.name }</name>
        <age>{ person.age }</age>
      </person>
    )
  }
}