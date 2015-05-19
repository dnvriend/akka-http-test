package com.github.dnvriend

import akka.http.scaladsl.marshalling.{Marshal, Marshaller, Marshalling}
import akka.http.scaladsl.model.HttpCharset
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}

import scala.concurrent.Future
import scala.xml.NodeSeq

class XmlMarshalUnmarshalTest extends TestSpec {

  case class Person(name: String, age: Int)

  val personXml =
    <person>
      <name>John Doe</name>
      <age>25</age>
    </person>

  implicit val personUnmarshaller = Unmarshaller[NodeSeq, Person] { xml =>
    Future(Person((xml \\ "name").text, (xml \\ "age").text.toInt))
  }

  val opaquePersonMarshalling = Marshalling.Opaque(() => personXml)
  val openCharsetPersonMarshalling = Marshalling.WithOpenCharset(`text/xml`, (charset: HttpCharset) => personXml)
  val fixedCharsetPersonMarshalling = Marshalling.WithFixedCharset(`text/xml`, `UTF-8`, () => personXml)

  val opaquePersonMarshaller = Marshaller.opaque[Person, NodeSeq] { person => personXml }
  val withFixedCharsetPersonMarshaller = Marshaller.withFixedCharset[Person, NodeSeq](`text/xml`, `UTF-8`) { person => personXml }
  val withOpenCharsetCharsetPersonMarshaller = Marshaller.withOpenCharset[Person, NodeSeq](`text/xml`) { (person, charset) => personXml }

//  implicit val personMarshaller = Marshaller.strict[Person, NodeSeq] { person =>
//    Marshalling.Opaque(() => personXml),
//  }

//  implicit val personMarshaller = Marshaller.opaque[Person, NodeSeq] { person => personXml }

//  implicit val personMarshaller = Marshaller[Person, NodeSeq] { person =>
//    Future(List(opaquePersonMarshalling, openCharsetPersonMarshalling, fixedCharsetPersonMarshalling))
//  }

  implicit val personMarshaller = Marshaller.oneOf[Person, NodeSeq](opaquePersonMarshaller, withFixedCharsetPersonMarshaller, withOpenCharsetCharsetPersonMarshaller)

  "personXml" should "be unmarshalled" in {
    Unmarshal(personXml).to[Person].futureValue shouldBe Person("John Doe", 25)
  }

  "Person" should "be marshalled" in {
    Marshal(Person("John Doe", 25)).to[NodeSeq].futureValue shouldBe personXml
  }
}
