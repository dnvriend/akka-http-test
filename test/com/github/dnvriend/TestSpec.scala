/*
 * Copyright 2016 Dennis Vriend
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

import akka.actor.{ ActorRef, ActorSystem, PoisonPill }
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestProbe
import akka.util.Timeout
import org.scalatest._
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.BindingKey
import play.api.test.WsTestClient

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag
import scala.util.Try

class TestSpec extends FlatSpec
    with Matchers
    with GivenWhenThen
    with OptionValues
    with TryValues
    with ScalaFutures
    with WsTestClient
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Eventually
    with GuiceOneServerPerSuite {

  def getComponent[A: ClassTag] = app.injector.instanceOf[A]

  def getAnnotatedComponent[A](name: String)(implicit ct: ClassTag[A]): A =
    app.injector.instanceOf[A](BindingKey(ct.runtimeClass.asInstanceOf[Class[A]]).qualifiedWith(name))

  // set the port number of the HTTP server
  override lazy val port: Int = 8080
  implicit val timeout: Timeout = 10.seconds
  implicit val pc: PatienceConfig = PatienceConfig(timeout = 30.seconds, interval = 300.millis)
  implicit val system: ActorSystem = getComponent[ActorSystem]
  implicit val ec: ExecutionContext = getComponent[ExecutionContext]
  implicit val mat: Materializer = getComponent[Materializer]

  // ================================== Supporting Operations ====================================
  implicit class PimpedByteArray(self: Array[Byte]) {
    def getString: String = new String(self)
  }

  implicit class PimpedFuture[T](self: Future[T]) {
    def toTry: Try[T] = Try(self.futureValue)
  }

  implicit class SourceOps[A](src: Source[A, _]) {
    def testProbe(f: TestSubscriber.Probe[A] => Unit): Unit =
      f(src.runWith(TestSink.probe(system)))
  }

  def killActors(actors: ActorRef*): Unit = {
    val tp = TestProbe()
    actors.foreach { (actor: ActorRef) =>
      tp watch actor
      actor ! PoisonPill
      tp.expectTerminated(actor)
    }
  }

  override protected def beforeEach(): Unit = {
  }
}