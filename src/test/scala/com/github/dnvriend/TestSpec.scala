package com.github.dnvriend

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorFlowMaterializer
import org.scalatest.{Matchers, BeforeAndAfterAll, FlatSpec}
import org.scalatest.concurrent.ScalaFutures

class TestSpec extends FlatSpec with Matchers with BeforeAndAfterAll with ScalaFutures {
  implicit val system = ActorSystem("Test")
  implicit val ec = system.dispatcher
  implicit val log = Logging(system, this.getClass)
  implicit val materializer = ActorFlowMaterializer()

  override protected def afterAll(): Unit = {
    system.shutdown()
    system.awaitTermination()
  }
}
