package com.braxisltd.http

import java.nio.charset.Charset

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import io.generators.core.Generators
import org.apache.commons.codec.Charsets
import org.apache.http.HttpStatus
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class HttpClientTest extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  import Unmarshallers._
  import org.scalatest.time.SpanSugar._

  import scala.concurrent.ExecutionContext.Implicits.global

  val server = new WireMockServer(0)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    server.start()
  }

  override protected def afterAll(): Unit = {
    server.stop()
    super.afterAll()
  }

  "HttpClient" should "return byte array" in new Fixture {
    val response = alpha.next().getBytes
    val future = HttpClient().forUrl(stubGetSuccess(response)).get()
    awaitEither(future).right.get.entity[Array[Byte]].toList should be(response.toList)
  }

  it should "return string for utf-8" in new Fixture {
    val response = "!@€#£$%^&*?'`~"
    val future = HttpClient().forUrl(stubGetSuccess(response, Charsets.UTF_8)).get()
    awaitEither(future).right.map(_.entity[String]) should be(Right(response))
  }

  it should "return string for iso-8859-15" in new Fixture {
    val response = "!@€#£$%^&*?'`~"
    val future = HttpClient().forUrl(stubGetSuccess(response, Charset.forName("ISO-8859-15"))).get()
    awaitEither(future).right.map(_.entity[String]) should be(Right(response))
  }

  def awaitEither[T](future: Future[T], waitFor: Duration = 1.second): Either[Throwable, T] = {
    Await.result(
      future
          .map(Right.apply[Throwable, T])
          .recover {
            case t: Throwable => Left[Throwable, T](t)
          },
      waitFor
    )
  }

  trait Fixture {

    import WireMock._

    val alpha = Generators.alphabetic10

    def url(path: String) = s"http://localhost:${server.port()}$path"

    def stubGetSuccess(response: Array[Byte]): String = {
      val path = s"/${alpha.next()}"
      server.stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withBody(response)))
      url(path)
    }

    def stubGetSuccess(response: String, charset: Charset): String = {
      val path = s"/${alpha.next()}"
      val body = response.getBytes(charset)
      server.stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withHeader("Content-Type", s"text/plain; charset=${charset.displayName()}").withBody(body)))
      url(path)
    }

    def stubGetSuccessWithDelay(response: Array[Byte], timeout: Duration): String = {
      val path = s"/${alpha.next()}"
      server.stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withFixedDelay(timeout.toMillis.toInt).withBody(response)))
      url(path)
    }
  }

}
