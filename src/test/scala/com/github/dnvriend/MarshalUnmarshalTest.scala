package com.github.dnvriend

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json._

class MarshalUnmarshalTest extends TestSpec {

  case class Person(firstName: String, lastName: String, age: Int, married: Option[Boolean] = None)

  object Marshallers extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val personJsonFormat = jsonFormat4(Person)
  }

  val personJson = """{"firstName":"John","lastName":"Doe","age":35}"""

  val personJsonMarried = """{"firstName":"John","lastName":"Doe","age":35,"married":true}"""

  import Marshallers._

  "Person" should "be marshalled" in {
    Person("John", "Doe", 35).toJson.compactPrint shouldBe personJson
  }

  it should "be unmarshalled" in {
    personJsonMarried.parseJson.convertTo[Person] shouldBe Person("John", "Doe", 35, Option(true))
  }
}
