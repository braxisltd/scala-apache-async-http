package com.braxisltd.http

import com.braxisltd.http.HttpClient.{CallCancelledException, Callback}
import io.generators.core.Generators._
import org.apache.http.HttpResponse
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Promise

class CallbackTest extends FlatSpec with Matchers with MockitoSugar with ScalaFutures {

  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  val timeout = 100.millis

  trait Fixture {
    val unmarshalled = alphabetic10.next()
    val httpResponse = mock[HttpResponse]
    val unmarshaller: HttpResponse => String = {
      case resp if resp == httpResponse => unmarshalled
    }
    val promise = Promise[String]()
    val future = promise.future
    val callback = new Callback[String](unmarshaller, promise)
  }

  "Callback" should "complete the promise upon complete" in new Fixture {
    callback.completed(httpResponse)
    whenReady(future) {
      _ should be(unmarshalled)
    }
  }

  it should "fail the future if the callback fails" in new Fixture {
    val failureMessage = alphabetic10.next()
    val exception = new Exception(failureMessage)
    callback.failed(exception)
    val recoveredWithFailureMessage = future.recover { case e => e.getMessage }
    whenReady(recoveredWithFailureMessage) {
      _ should be (failureMessage)
    }
  }

  it should "fail the future if the callback is cancelled" in new Fixture {
    callback.cancelled()
    val recoveredWithFailureMessage = future.recover { case e => e.getClass.getName }
    whenReady(recoveredWithFailureMessage) {
      _ should be (CallCancelledException.getClass.getName)
    }
  }
}
