package com.twitter.streaming

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.{EmbeddedHttpServer, StreamingJsonTestHelper}
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.server.FeatureTest
import com.twitter.io.Buf
import com.twitter.util.{Duration, Future}

object StreamingServerFeatureTest {
  val TweetMsgPrefix: String = "msg: "
  val TweetLocation: String = "US"

  /* Response Implicit Utils */
  implicit class RichResponse(val self: Response) extends AnyVal {
    def getReaderStrings: Future[Seq[String]] = {

      def readString(acc: String): Future[Seq[String]] = {
        self.reader.read().flatMap {
          case Some(Buf.Utf8(str)) =>
            println("Read:\t" + str.replace("\n", "\\n"))
            readString(acc + str)
          case None =>
            Future.value(acc.split("\n").toSeq)
        }
      }

      readString("")
    }
  }
}

class StreamingServerFeatureTest extends FeatureTest {
  import StreamingServerFeatureTest._

  // our response stream has a delay between messages so 5 seconds might cut it close
  // in slow CI environments like Travis so we are bumping timeouts to 10 seconds.
  override protected def defaultAwaitTimeout: Duration = 10.seconds

  override val server = new EmbeddedHttpServer(new StreamingServer, streamResponse = true)

  lazy val streamingJsonHelper =
    new StreamingJsonTestHelper(server.mapper)

  test("streamingRequest#post via Reader") {
    val readerStrings = verifyStreamingEndpoint("/tweets/streaming/reader")
    val readerString = await(readerStrings.map(_.head))
    assert(readerString.equals(TweetMsgPrefix + "1"))
  }

  test("streamingRequest with resource management") {
    val readerStrings = verifyStreamingEndpoint("/tweets/streaming/reader_with_resource_management")
    val readerString = await(readerStrings.map(_.head))
    assert(readerString.equals(TweetMsgPrefix + "1"))
  }

  test("streamingRequest#post via AsyncStream") {
    val readerStrings = verifyStreamingEndpoint("/tweets/streaming/asyncstream")
    val readerString = await(readerStrings.map(_.head))
    assert(readerString.equals(TweetMsgPrefix + "1"))
  }

  test("streaming#post json") {
    verifyStreamingEndpoint("/tweets")
  }

  /* -----------------------------------------------------------------------------------------------
   *                                              Utils
   * ---------------------------------------------------------------------------------------------*/
  private def verifyStreamingEndpoint(url: String): Future[Seq[String]] = {
    val request = RequestBuilder.post(url).chunked
    streamTweets(request)
    val response = server.httpRequest(request)
    response.getReaderStrings
  }

  private def streamTweets(request: Request): Future[Unit] = {
    val tweets = for (i <- 1 to 100) yield {
      Tweet(text = TweetMsgPrefix + s"$i", location = Some(TweetLocation))
    }
    // Write to request in separate thread
    pool {
      streamingJsonHelper.writeJsonArray(request, tweets, delayMs = 25)
    }
  }
}
