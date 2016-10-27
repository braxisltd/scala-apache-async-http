package com.braxisltd.http

import com.braxisltd.http.HttpClient.Callback
import com.braxisltd.http.Unmarshallers.Unmarshaller
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.util.EntityUtils

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

class HttpClient private()(implicit executionContext: ExecutionContext) {

  val client = HttpAsyncClients.createDefault()
  client.start()

  def forUrl(url: String)(implicit executionContext: ExecutionContext) = new CallableHttpClient(url, Nil)

  class CallableHttpClient private[http](url: String, parameters: List[(String, String)])(implicit executionContext: ExecutionContext) {
    def withParameter(name: String, value: String): CallableHttpClient = {
      new CallableHttpClient(url, (name, value) :: parameters)
    }
    
    def get(): Future[Response] = {
      val promise = Promise[Response]()
      val uri = parameters.foldLeft(new URIBuilder(url)) {
        (requestBuilder, parameter) =>
          val (name, value) = parameter
          requestBuilder.addParameter(name, value)
      }
      client.execute(new HttpGet(uri.build()), new Callback(promise))
      promise.future
    }
  }

}

object HttpClient {
  def apply()(implicit executionContext: ExecutionContext) = new HttpClient()

  class Callback(promise: Promise[Response]) extends FutureCallback[HttpResponse] {
    override def cancelled(): Unit = promise.failure(CallCancelledException)

    override def completed(result: HttpResponse): Unit = {
      promise.complete(Success(new Response(result)))
    }

    override def failed(ex: Exception): Unit = promise.failure(ex)
  }

  object CallCancelledException extends Exception

}

class Response(httpResponse: HttpResponse) {
  def entity[T](implicit unmarshaller: Unmarshaller[T]): T = unmarshaller(httpResponse)

  lazy val status = httpResponse.getStatusLine.getStatusCode
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


