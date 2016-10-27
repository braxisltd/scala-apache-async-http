package com.braxisltd.http

import com.braxisltd.http.HttpClient.Callback
import com.braxisltd.http.Unmarshallers.Unmarshaller
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.util.EntityUtils

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

class HttpClient private()(implicit executionContext: ExecutionContext) {

  val client = HttpAsyncClients.createDefault()
  client.start()

  def forUrl(url: String)(implicit executionContext: ExecutionContext) = new CallableHttpClient(url)

  class CallableHttpClient private[http](url: String)(implicit executionContext: ExecutionContext) {

    def get[T]()(implicit unmarshaller: Unmarshaller[T]): Future[T] = {
      val promise = Promise[T]()
      val req = new HttpGet(url)
      client.execute(req, new Callback(unmarshaller, promise))
      promise.future
    }
  }

}

object HttpClient {
  def apply()(implicit executionContext: ExecutionContext) = new HttpClient()

  class Callback[T](unmarshaller: Unmarshaller[T], promise: Promise[T]) extends FutureCallback[HttpResponse] {
    override def cancelled(): Unit = promise.failure(CallCancelledException)

    override def completed(result: HttpResponse): Unit = {
      promise.complete(Success(unmarshaller(result)))
    }

    override def failed(ex: Exception): Unit = promise.failure(ex)
  }

  object CallCancelledException extends Exception

}

object Unmarshallers {
  type Unmarshaller[T] = HttpResponse => T

  implicit val bytesUnmarshaller: Unmarshaller[Array[Byte]] = {
    result =>
      Iterator
          .continually(result.getEntity.getContent.read())
          .takeWhile {_ != -1}
          .map(_.toByte)
          .toArray
  }

  implicit val StringUnmarshaller: Unmarshaller[String] = {
    result =>
      EntityUtils.toString(result.getEntity)
  }
}


