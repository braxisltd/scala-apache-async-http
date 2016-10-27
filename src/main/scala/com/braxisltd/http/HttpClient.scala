package com.braxisltd.http

import com.braxisltd.http.Unmarshallers.Unmarshaller
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.util.EntityUtils

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

class HttpClient private(url: String)(implicit executionContext: ExecutionContext) {

  val client = HttpAsyncClients.createDefault()

  def closeAfter[T](future: Future[T]): Future[T] = {
    future.onComplete {
      case _ => client.close()
    }
    future
  }

  def get[T]()(implicit unmarshaller: Unmarshaller[T]): Future[T] = {
    val promise = Promise[T]()
    client.start()
    val req = new HttpGet(url)
    client.execute(
      req,
      new FutureCallback[HttpResponse] {
        override def cancelled(): Unit = ???

        override def completed(result: HttpResponse): Unit = {
          promise.complete(Success(unmarshaller(result)))
        }

        override def failed(ex: Exception): Unit = ???
      }
    )
    closeAfter(promise.future)
  }
}

object HttpClient {
  def forUrl(url: String)(implicit executionContext: ExecutionContext) = new HttpClient(url)
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


