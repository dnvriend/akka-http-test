# akka-http-test #

[![Join the chat at https://gitter.im/dnvriend/akka-http-test](https://badges.gitter.im/dnvriend/akka-http-test.svg)](https://gitter.im/dnvriend/akka-http-test?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/dnvriend/akka-http-test.svg?branch=master)](https://travis-ci.org/dnvriend/akka-http-test)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

A study project how akka-http works. The code below is a bit compacted, so please use it for reference only how
the (new) API must be used. It will not compile/work correctly when you just copy/paste it. Check out the working 
source code for correct usage.

## Dependencies ##
To use akka-http we need the following dependencies:

* `reactive-streams`:
  * Provides the new abstraction for async and non-blocking pipeline processing,
  * It has automatic support for back-pressure,
  * Standardized API it's called: [reactive-streams.org](http://www.reactive-streams.org) and as of May 2015 is v1.0,
* `akka-stream-experimental`:   
  * Provided a standard API and DSL for creating composable stream transformations based upon the [reactive-streams.org](http://www.reactive-streams.org) standard.
* `akka-http-core-experimental`:
  * Sits on top of `akka-io`,
  * Performs TCP <-> HTTP translation,
  * Cleanly separated layer of stream transformations provided by Akka Extension,
  * Implements HTTP 'essentials', no higher-level features (like file serving)
* `akka-http-scala-experimental`:
  * Provides higher-level server- and client-side APIs
  * 'Unmarshalling' custom types from HttpEntities,
  * 'Marshalling' custom types to HttpEntities
  * (De)compression (GZip / Deflate),
  * Routing DSL
* `akka-http-spray-json-experimental:`:
  * Provides spray-json support

# Web Service Clients (RPC)
Akka-Http has a client API and as such RPC's can be created. Take a look at the package `com.github.dnvriend.webservices`, I have created
some example RPC style web service clients for `eetnu`, `iens`, `postcode`, `openWeatherApi`, based on the generic `com.github.dnvriend.webservices.generic.HttpClient`
client that supports Http and SSL connections with basic authentication or one legged OAuth1 with consumerKey and consumerSecret configuration
from `application.conf`. The RPC clients also support for single RPC without cached connections and the streaming cached connection style where
you can stream data to your clients. For usage please see the tests for the RPC clients. Good stuff :)

# Web Server
A new HTTP server can be launched using the `Http()` class. The `bindAndHandle()` method is a convenience method which starts 
a new HTTP server at the given endpoint and uses the given 'handler' `Flow` for processing  all incoming connections. 

Note that there is no backpressure being applied to the 'connections' `Source`, i.e. all connections are being accepted 
at maximum rate, which, depending on the applications, might present a DoS risk!

```scala
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.stream.scaladsl._

def routes: Flow[HttpRequest, HttpResponse, Unit]

Http().bindAndHandle(routes, "0.0.0.0", 8080)
```

# Routes
First some Akka Stream parley:

* `Stream`: a continually moving flow of elements, 
* `Element`: the processing unit of streams,
* `Processing stage`: building blocks that build up a `Flow` or `FlowGraph` for example `map()`, `filter()`, `transform()`, `junction()` etc,
* `Source`: a processing stage with exactly one output, emitting data elements when downstream processing stages are ready to receive them,
* `Sink`: a processing stage with exactly one input, requesting and accepting data elements
* `Flow`: a processing stage with exactly one input and output, which connects its up- and downstream by moving/transforming the data elements flowing through it,
* `Runnable flow`: A flow that has both ends attached to a `Source` and `Sink` respectively,
* `Materialized flow`: An instantiation / incarnation / materialization of the abstract processing-flow template. 

The abstractions above (Flow, Source, Sink, Processing stage) are used to create a processing-stream `template` or `blueprint`. When the template has a `Source` connected to a `Sink` with optionally some `processing stages` between them, such a `template` is called a `Runnable Flow`. 

The materializer for `akka-stream` is the [ActorMaterializer](http://doc.akka.io/api/akka-stream-and-http-experimental/1.0/#akka.stream.ActorMaterializer) 
which takes the list of transformations comprising a [akka.stream.scaladsl.Flow](http://doc.akka.io/api/akka-stream-and-http-experimental/1.0/#akka.stream.javadsl.Flow) 
and materializes them in the form of [org.reactivestreams.Processor](https://github.com/reactive-streams/reactive-streams-jvm/blob/master/api/src/main/java/org/reactivestreams/Processor.java) 
instances, in which every stage is converted into one actor.

In akka-http parley, a 'Route' is a `Flow[HttpRequest, HttpResponse, Unit]` so it is a processing stage that transforms 
`HttpRequest` elements to `HttpResponse` elements. 

## Streams everywhere
The following `reactive-streams` are defined in `akka-http`:

* Requests on one HTTP connection,
* Responses on one HTTP connection,
* Chunks of a chunked message,
* Bytes of a message entity. 

# Route Directives
Akka http uses the route directives we know (and love) from Spray:

```scala
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl._

def routes: Flow[HttpRequest, HttpResponse, Unit] =
  logRequestResult("akka-http-test") {
    path("") {
      redirect("person", StatusCodes.PermanentRedirect)
    } ~
    pathPrefix("person") {
      complete {
        Person("John Doe", 25)
      }
    } ~
    pathPrefix("ping") {
      complete {
        Ping(TimeUtil.timestamp)
      }
    }
  }
```

# Spray-Json
I'm glad to see that `akka-http-spray-json-experimental` basically has the same API as spray:

```scala
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json.DefaultJsonProtocol._
import spray.json._

case class Person(name: String, age: Int)

val personJson = """{"name":"John Doe","age":25}"""

implicit val personJsonFormat = jsonFormat2(Person)

Person("John Doe", 25).toJson.compactPrint shouldBe personJson

personJson.parseJson.convertTo[Person] shouldBe Person("John Doe", 25)

val person = Person("John Doe", 25)
val entity = Marshal(person).to[RequestEntity].futureValue
Unmarshal(entity).to[Person].futureValue shouldBe person
```

# Custom Marshalling/Unmarshalling
Akka http has a cleaner API for custom types compared to Spray's. Out of the box it has support to marshal to/from basic types (Byte/String/NodeSeq) and 
so we can marshal/unmarshal from/to case classes from any line format. The API uses the [Marshal](http://doc.akka.io/api/akka-stream-and-http-experimental/1.0/#akka.http.scaladsl.marshalling.Marshal) 
object to do the marshalling and the [Unmarshal](http://doc.akka.io/api/akka-stream-and-http-experimental/1.0/#akka.http.scaladsl.unmarshalling.Unmarshal)
object to to the unmarshal process. Both interfaces return Futures that contain the outcome. 

The `Unmarshal` class uses an [Unmarshaller](http://doc.akka.io/api/akka-stream-and-http-experimental/1.0/#akka.http.scaladsl.unmarshalling.Unmarshaller) 
that defines how an encoding like eg `XML` can be converted from eg. a `NodeSeq` to a custom type, like eg. a `Person`. 

To `Marshal` class uses [Marshaller](http://doc.akka.io/api/akka-stream-and-http-experimental/1.0/#akka.http.javadsl.server.Marshaller)s
to do the heavy lifting. There are three kinds of marshallers, they all do the same, but one is not interested in the [MediaType](http://doc.akka.io/api/akka-stream-and-http-experimental/1.0/#akka.http.javadsl.model.MediaType) 
, the `opaque` marshaller, then there is the `withOpenCharset` marshaller, that is only interested in the mediatype, and forwards the received [HttpCharset](http://doc.akka.io/api/akka-stream-and-http-experimental/1.0/#akka.http.scaladsl.model.HttpCharset) 
to the `marshal function` so that the responsibility for handling the character encoding is up to the developer, 
and the last one, the `withFixedCharset` will handle only HttpCharsets that match the marshaller configured one. 
 
An example XML marshaller/unmarshaller:

```scala
import akka.http.scaladsl.marshalling.{ Marshal, Marshaller, Marshalling }
import akka.http.scaladsl.model.HttpCharset
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.unmarshalling.{ Unmarshal, Unmarshaller }

import scala.xml.NodeSeq

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
```

# Vendor specific media types
Versioning an API can be tricky. The key is choosing a strategy on how to do versioning. I have found and tried the following stragegies as
blogged by [Jim Liddell's blog](https://web.archive.org/web/20151001093016/http://liddellj.com/using-media-type-parameters-to-version-an-http-api/), which is great by the way!

1. 'The URL is king' in which the URL is encoded in the URL eg. `http://localhost:8080/api/v1/person`. The downside of this strategy is that
the location of a resource may not change, and when we request another representation, the url does change eg. to `http://localhost:8080/api/v2/person`.
2. Using a version request parameter like: `http://localhost:8080/api/person?version=1`. The downside of this stragegy lies in the fact that resource urls 
must be as lean as possible, and the only exception is for filtering, sorting, searching and paging, as stated by [Vinay Sahni](http://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api)
in his great blog 'Best Practices for Designing a Pragmatic RESTful API'.
3. Both bloggers and I agree that using request headers for versioning, and therefor relying on vendor specific media types is a great way to keep the
resource urls clean, the location does not change and in code the versioning is only a presentation responsibility, easilly resolved by an
in scope mashaller.

When you run the example, you can try the following requests:

```bash
# The latest version in JSON
curl -H "Accept: application/json" localhost:8080/person
# The latest version in XML
curl -H "Accept: application/xml" localhost:8080/person
# Vendor specific header for JSON v1
curl -H "Accept: application/vnd.acme.v1+json" localhost:8080/person
# Vendor specific header for JSON v2
curl -H "Accept: application/vnd.acme.v2+json" localhost:8080/person
# Vendor specific header for XML v1
curl -H "Accept: application/vnd.acme.v1+xml" localhost:8080/person
# Vendor specific header for XML v2
curl -H "Accept: application/vnd.acme.v2+xml" localhost:8080/person
```

Please take a look at the [Marshallers](https://github.com/dnvriend/akka-http-test/blob/master/src/main/scala/com/github/dnvriend/Marshallers.scala)
trait for an example how you could implement this strategy and the [MarshallersTest](https://github.com/dnvriend/akka-http-test/blob/master/src/test/scala/com/github/dnvriend/MarshallersTest.scala)
how to test the routes using the `Accept` header and leveraging the media types.

# Video
- [Parleys - Dr. Roland Kuhn - Akka HTTP - The Reactive Web Toolkit](https://www.parleys.com/tutorial/akka-http-reactive-web-toolkit)
- [Parleys - Mathias Doenitz - Akka HTTP - Unrest your actors](https://www.parleys.com/tutorial/akka-http-un-rest-your-actors)
- [Youtube - Mathias Doenitz - Akka HTTP — The What, Why and How](https://www.youtube.com/watch?v=y_slPbktLr0)
- [Youtube -  Mathias Doenitz - Spray & Akka HTTP](https://www.youtube.com/watch?v=o5PUDI4qi10)
- [Youtube -  Mathias Doenitz - Spray on Akka](https://www.youtube.com/watch?v=7MqD7_YvZ8Q)
- [Parleys - Dr. Roland Kuhn - Go Reactive: Blueprint for Future Applications ](https://www.parleys.com/tutorial/go-reactive-blueprint-future-applications)
- [Parleys - Dr. Roland Kuhn - Distributed in space and time](https://www.parleys.com/tutorial/roland-kuhn-distributed-space-time-1)
- [Parleys - Mirco Dotta - Akka Streams](https://www.parleys.com/tutorial/akka-streams)

# Slides
- [Slides - Akka HTTP — The What, Why and How](http://spray.io/nescala2015/#/)
- [Slides - Reactive Streams & Akka HTTP](http://spray.io/msug/#/)

# Northeast Scala Symposium 2015
- [Northeast Scala Symposium 2015](https://newcircle.com/s/post/1702/northeast_scala_symposium_2015_videos?utm_campaign=YouTube_Channel&utm_source=youtube&utm_medium=social&utm_content=Watch%20the%2013%20talks%20from%20NE%20Scala%202015%3A)


# Projects that use akka-http
- [GitHub - Example of (micro)service written in Scala & akka-http](https://github.com/theiterators/akka-http-microservice)
