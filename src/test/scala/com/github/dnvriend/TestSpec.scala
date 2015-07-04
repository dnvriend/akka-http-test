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

import java.io.InputStream
import java.util.UUID

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.util.Timeout
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.io.Source
import scala.util.Try

class TestSpec extends FlatSpec with Matchers with ScalaFutures with TryValues with OptionValues with BeforeAndAfterAll {
  implicit val timeout: Timeout = Timeout(10.seconds)
  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  implicit val log: LoggingAdapter = Logging(system, this.getClass)
  implicit val pc: PatienceConfig = PatienceConfig(timeout = 10.seconds)

  def randomId: String = UUID.randomUUID.toString
  val id: String = randomId

  def withInputStream(fileName: String)(f: InputStream ⇒ Unit): Unit = {
    val is = fromClasspathAsStream(fileName)
    try { f(is) } finally { Try(is.close()) }
  }

  implicit class FutureToTry[T](f: Future[T]) {
    def toTry: Try[T] = Try(f.futureValue)
  }

  implicit class MustBeWord[T](self: T) {
    def mustBe(pf: PartialFunction[T, Unit]): Unit =
      if (!pf.isDefinedAt(self)) throw new TestFailedException("Unexpected: " + self, 0)
  }

  def streamToString(is: InputStream): String =
    Source.fromInputStream(is).mkString

  def fromClasspathAsString(fileName: String): String =
    streamToString(fromClasspathAsStream(fileName))

  def fromClasspathAsStream(fileName: String): InputStream =
    getClass.getClassLoader.getResourceAsStream(fileName)

  override protected def afterAll(): Unit = {
    system.shutdown()
    system.awaitTermination()
  }
}
