package com.twitter.finatra.httpclient

import com.twitter.finagle.http.{Fields, Message, Method, Request, RequestProxy}
import com.twitter.io.StreamIO
import java.nio.charset.StandardCharsets.UTF_8

/**
 * Provides a class for building <code>finagle.http.Request</code> objects
 */
@deprecated("Use com.twitter.finatra.http.request.RequestBuilder directly.", "02-16-2020")
object RequestBuilder {

  @deprecated("Use com.twitter.finatra.http.request.RequestBuilder.get directly.", "02-16-2020")
  def get(url: String): RequestBuilder = {
    create(Method.Get, url)
  }

  @deprecated("Use com.twitter.finatra.http.request.RequestBuilder.post directly.", "02-16-2020")
  def post(url: String): RequestBuilder = {
    create(Method.Post, url)
  }

  @deprecated("Use com.twitter.finatra.http.request.RequestBuilder.put directly.", "02-16-2020")
  def put(url: String): RequestBuilder = {
    create(Method.Put, url)
  }

  @deprecated("Use com.twitter.finatra.http.request.RequestBuilder.patch directly.", "02-16-2020")
  def patch(url: String): RequestBuilder = {
    create(Method.Patch, url)
  }

  @deprecated("Use com.twitter.finatra.http.request.RequestBuilder.delete directly.", "02-16-2020")
  def delete(url: String): RequestBuilder = {
    create(Method.Delete, url)
  }

  @deprecated("Use com.twitter.finatra.http.request.RequestBuilder.head directly.", "02-16-2020")
  def head(url: String): RequestBuilder = {
    create(Method.Head, url)
  }

  @deprecated("Use com.twitter.finatra.http.request.RequestBuilder.trace directly.", "02-16-2020")
  def trace(url: String): RequestBuilder = {
    create(Method.Trace, url)
  }

  @deprecated("Use com.twitter.finatra.http.request.RequestBuilder.connect directly.", "02-16-2020")
  def connect(url: String): RequestBuilder = {
    create(Method.Connect, url)
  }

  @deprecated("Use com.twitter.finatra.http.request.RequestBuilder.options directly.", "02-16-2020")
  def options(url: String): RequestBuilder = {
    create(Method.Options, url)
  }

  @deprecated("Use com.twitter.finatra.http.request.RequestBuilder.create directly.", "02-16-2020")
  def create(method: Method, url: String): RequestBuilder = {
    new RequestBuilder(Request(method, url))
  }

}

/**
 * RequestBuilder is a finagle.http.Request with a builder API for common mutations
 */
@deprecated("Use com.twitter.finatra.http.request.RequestBuilder directly.", "02-16-2020")
class RequestBuilder(override val request: Request) extends RequestProxy {

  def headers(headers: Map[String, String]): RequestBuilder = {
    for {
      (key, value) <- headers
    } {
      request.headerMap.set(key, value)
    }
    this
  }

  def headers(elems: (String, String)*): RequestBuilder = {
    headers(elems.toMap)
  }

  def headers(elems: Iterable[(String, String)]): RequestBuilder = {
    headers(elems.toMap)
  }

  def header(key: String, value: AnyRef): RequestBuilder = {
    request.headerMap.set(key, value.toString)
    this
  }

  def chunked: RequestBuilder = {
    request.setChunked(true)
    this
  }

  def body(string: String, contentType: String = Message.ContentTypeJson): RequestBuilder = {
    request.setContentString(string)
    request.headerMap.set(Fields.ContentLength, string.getBytes(UTF_8).length.toString)
    request.headerMap.set(Fields.ContentType, contentType)
    this
  }

  def bodyFromResource(
    resource: String,
    contentType: String = Message.ContentTypeJson
  ): RequestBuilder = {
    val bodyStream = getClass.getResourceAsStream(resource)
    body(StreamIO.buffer(bodyStream).toString(), contentType)
  }
}
