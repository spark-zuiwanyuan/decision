package com.stratio.decision.api

import com.stratio.decision.api.kafka.KafkaProducer
import com.stratio.decision.commons.constants.ReplyCode._
import com.stratio.decision.commons.messages.StratioStreamingMessage
import org.junit.runner.RunWith
import org.mockito.Matchers.anyString
import org.mockito.Mockito
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

@RunWith(classOf[JUnitRunner])
class StreamingAPIAsyncOperationTest extends FunSpec
with GivenWhenThen
with ShouldMatchers
with MockitoSugar {
  val kafkaProducerMock = mock[KafkaProducer]
  val stratioStreamingAPIAsyncOperation = new StreamingAPIAsyncOperation(kafkaProducerMock)
  val stratioStreamingMessage = new StratioStreamingMessage()

  describe("The Decision API Async Operation") {
    it("should throw no exceptions when the engine returns an OK return code") {
      Given("an OK engine response")
      val errorCode = OK.getCode
      val engineResponse = s"""{"errorCode":$errorCode}"""
      When("we perform the async operation")
      Mockito.doNothing().when(kafkaProducerMock).send(anyString(), anyString())
      Then("we should not get a StratioAPISecurityException")
      try {
        stratioStreamingAPIAsyncOperation.performAsyncOperation(stratioStreamingMessage)
      } catch {
        case _: Throwable => fail()
      }
    }
  }
}
