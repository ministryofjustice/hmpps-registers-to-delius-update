package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.integration.endtoend

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.RequestMethod.GET
import com.github.tomakehurst.wiremock.http.RequestMethod.POST
import com.github.tomakehurst.wiremock.http.RequestMethod.PUT
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.helpers.courtRegisterUpdateMessage
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.CourtRegisterApiExtension.Companion.courtRegisterApi
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.ProbationApiExtension.Companion.probationApi

class CourtRegisterTest : SqsIntegrationTestBase() {

  @Test
  fun `will consume a COURT_REGISTER_UPDATE message for missing court`() {
    courtRegisterApi.stubCourtGetFail("SHFCC", HttpStatus.NOT_FOUND.value())

    val message = courtRegisterUpdateMessage()

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(queueUrl, message)

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    await untilCallTo { courtRegisterApi.requestCount(GET, "/courts/id/SHFCC") } matches { it == 1 }

    courtRegisterApi.verify(
      WireMock.getRequestedFor(WireMock.urlEqualTo("/courts/id/SHFCC"))
    )
  }

  @Test
  fun `will consume a COURT_REGISTER_UPDATE message for court update`() {
    courtRegisterApi.stubCourtGet("SHFCC")
    probationApi.stubCourtGet("SHFCC")
    hmppsAuth.stubGrantToken()
    probationApi.stubCourtPut("SHFCC")

    val message = courtRegisterUpdateMessage()

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(queueUrl, message)

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    await untilCallTo { probationApi.requestCount(PUT, "/secure/courts/code/SHFCC") } matches { it == 1 }

    probationApi.verify(
      WireMock.putRequestedFor(WireMock.urlEqualTo("/secure/courts/code/SHFCC"))
        .withHeader("Authorization", WireMock.equalTo("Bearer ABCDE"))
    )
  }

  @Test
  fun `will consume a COURT_REGISTER_UPDATE message for a court insert`() {
    courtRegisterApi.stubCourtGet("SHFCC")
    probationApi.stubCourtGetFail("SHFCC", HttpStatus.NOT_FOUND.value())
    hmppsAuth.stubGrantToken()
    probationApi.stubCourtPost()

    val message = courtRegisterUpdateMessage()

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(queueUrl, message)

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    await untilCallTo { probationApi.requestCount(POST, "/secure/courts") } matches { it == 1 }

    probationApi.verify(
      postRequestedFor(WireMock.urlEqualTo("/secure/courts"))
        .withHeader("Authorization", WireMock.equalTo("Bearer ABCDE"))
    )
  }

  private fun WireMockServer.requestCount(methodType: RequestMethod, url: String): Int =
    findAll(RequestPatternBuilder(methodType, WireMock.urlEqualTo(url))).count()

  private fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    val queueAttributes = awsSqsClient.getQueueAttributes(queueUrl, listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }
}
