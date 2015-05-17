# akka-http-test

A study project how akka-http works.

# Dependencies
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

Dependencies in `build.sbt`:

```scala
libraryDependencies ++= {
  val akkaVersion       = "2.3.11"
  val akkaStreamVersion = "1.0-RC2"
  Seq(
    "com.typesafe.akka" %% "akka-actor"                           % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-scala-experimental"         % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-testkit-scala-experimental" % akkaStreamVersion
  )
}
```

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
* `Materialized flow`: A

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
    pathPrefix("person") {
      complete {
        Person("John", "Doe", TimeUtil.timestamp)
      }
    }
  }
```

# Spray-Json
I'm glad to see that `akka-http-spray-json-experimental` basically has the same API as spray:

```scala
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json._

case class Person(firstName: String, lastName: String, age: Int, married: Option[Boolean] = None)

object Marshallers extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val personJsonFormat = jsonFormat4(Person)
}

val personJson = """{"firstName":"John","lastName":"Doe","age":35}"""

val personJsonMarried = """{"firstName":"John","lastName":"Doe","age":35,"married":true}"""

import Marshallers._

Person("John", "Doe", 35).toJson.compactPrint shouldBe personJson

personJsonMarried.parseJson.convertTo[Person] shouldBe Person("John", "Doe", 35, Option(true))
```

# Video
- [YouTube - Akka HTTP — The What, Why and How](https://www.youtube.com/watch?v=y_slPbktLr0) - [Slides](http://spray.io/nescala2015/#/)

# Northeast Scala Symposium 2015
- [Northeast Scala Symposium 2015](https://newcircle.com/s/post/1702/northeast_scala_symposium_2015_videos?utm_campaign=YouTube_Channel&utm_source=youtube&utm_medium=social&utm_content=Watch%20the%2013%20talks%20from%20NE%20Scala%202015%3A)


# Projects that use akka-http
- [GitHub - Example of (micro)service written in Scala & akka-http](https://github.com/theiterators/akka-http-microservice)