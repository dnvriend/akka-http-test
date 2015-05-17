akka-http-test
==============
Akka http test

# Web Server
A new HTTP server can be launched using the `Http()` class. The `bindAndHandle()` method is a convenience method which starts 
a new HTTP server at the given endpoint and uses the given 'handler' `Flow` for processing  all incoming connections. 

Note that there is no backpressure being applied to the 'connections' `Source`, i.e. all connections are being accepted 
at maximum rate, which, depending on the applications, might present a DoS risk!

# Routes
First some Akka Stream parley:

* `Stream`: a continually moving flow of elements, 
* `Element`: the processing unit of streams,
* `Processing stage`: building blocks that build up a `Flow` or `FlowGraph` for example `map()`, `filter()`, `transform()`, `junction()` etc,
* `Source`: a processing stage with exactly one output, emitting data elements when downstream processing stages are ready to receive them,
* `Sink`: a processing stage with exactly one input, requesting and accepting data elements
* `Flow`: a processing stage with exactly one input and output, which connects its up- and downstream by moving/transforming the data elements flowing through it,
* `Runnable flow`: A flow that has both ends attached to a `Source` and `Sink` respectively,
* `Materialized flow`: A runnable flow that ran

In akka-http parley, a 'Route' is a `Flow[HttpRequest, HttpResponse, Unit]` so it is a processing stage that transforms 
`HttpRequest` elements to `HttpResponse` elements. 

# Spray-Json
I'm glad to see that `akka-http-spray-json-experimental` basically has the same API as spray:

```scala
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
```