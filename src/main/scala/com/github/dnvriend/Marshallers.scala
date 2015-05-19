package com.github.dnvriend

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait Marshallers extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val personJsonFormat = jsonFormat2(Person)
  implicit val pingJsonFormat = jsonFormat1(Ping)

  implicit val windJsonFormat = jsonFormat2(Wind)
  implicit val mainJsonFormat = jsonFormat7(Main)
  implicit val cloudJsonFormat = jsonFormat1(Cloud)
  implicit val weatherJsonFormat = jsonFormat4(Weather)
  implicit val sysJsonFormat = jsonFormat4(Sys)
  implicit val coordJsonFormat = jsonFormat2(Coord)
  implicit val weatherResultJsonFormat = jsonFormat11(WeatherResult)
}