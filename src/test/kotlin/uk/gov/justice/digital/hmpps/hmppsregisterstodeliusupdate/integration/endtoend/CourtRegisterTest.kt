package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.integration.endtoend

import com.amazonaws.services.sqs.AmazonSQS
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.RequestMethod.GET
import com.github.tomakehurst.wiremock.http.RequestMethod.POST
import com.github.tomakehurst.wiremock.http.RequestMethod.PUT
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.nhaarman.mockitokotlin2.any
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.helpers.courtRegisterUpdateMessage
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.CourtRegisterApiExtension
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.ProbationApiExtension

@ExtendWith(ProbationApiExtension::class, CourtRegisterApiExtension::class, HmppsAuthApiExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CourtRegisterTest {
  @Qualifier("awsSqsClient")
  @Autowired
  internal lateinit var awsSqsClient: AmazonSQS

  @Value("\${sqs.queue.name}")
  lateinit var queueName: String

  @Test fun `will consume a COURT_REGISTER_UPDATE message for missing court`() {
    CourtRegisterApiExtension.courtRegisterApi.stubCourtGetFail("SHFCC", HttpStatus.NOT_FOUND.value())

    val message = courtRegisterUpdateMessage()

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(queueName.queueUrl(), message)

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    await untilCallTo { CourtRegisterApiExtension.courtRegisterApi.requestCount(GET, "/courts/id/SHFCC") } matches { it == 1 }

    CourtRegisterApiExtension.courtRegisterApi.verify(
      WireMock.getRequestedFor(WireMock.urlEqualTo("/courts/id/SHFCC"))
    )
    ProbationApiExtension.probationApi.verify(0, WireMock.putRequestedFor(any()))
    ProbationApiExtension.probationApi.verify(0, WireMock.postRequestedFor(any()))
  }

  @Test
  fun `will consume a COURT_REGISTER_UPDATE message for court update`() {
    CourtRegisterApiExtension.courtRegisterApi.stubCourtGet("SHFCC")
    ProbationApiExtension.probationApi.stubCourtGet("SHFCC")
    HmppsAuthApiExtension.hmppsAuth.stubGrantToken()
    ProbationApiExtension.probationApi.stubCourtPut("SHFCC")

    val message = courtRegisterUpdateMessage()

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(queueName.queueUrl(), message)

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    await untilCallTo { ProbationApiExtension.probationApi.requestCount(PUT, "/secure/courts/code/SHFCC") } matches { it == 1 }

    CourtRegisterApiExtension.courtRegisterApi.verify(
      WireMock.getRequestedFor(WireMock.urlEqualTo("/courts/id/SHFCC"))
    )
    ProbationApiExtension.probationApi.verify(
      WireMock.getRequestedFor(WireMock.urlEqualTo("/secure/courts/code/SHFCC"))
        .withHeader("Authorization", WireMock.equalTo("Bearer ABCDE"))
    )
    ProbationApiExtension.probationApi.verify(
      WireMock.putRequestedFor(WireMock.urlEqualTo("/secure/courts/code/SHFCC"))
        .withHeader("Authorization", WireMock.equalTo("Bearer ABCDE"))
    )
  }

  @Test
  fun `will consume a COURT_REGISTER_UPDATE message for a court insert`() {
    CourtRegisterApiExtension.courtRegisterApi.stubCourtGet("SHFCC")
    ProbationApiExtension.probationApi.stubCourtGetFail("SHFCC", HttpStatus.NOT_FOUND.value())
    HmppsAuthApiExtension.hmppsAuth.stubGrantToken()
    ProbationApiExtension.probationApi.stubCourtPost()

    val message = courtRegisterUpdateMessage()

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(queueName.queueUrl(), message)

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    await untilCallTo { ProbationApiExtension.probationApi.requestCount(POST, "/secure/courts") } matches { it == 1 }

    CourtRegisterApiExtension.courtRegisterApi.verify(
      WireMock.getRequestedFor(WireMock.urlEqualTo("/courts/id/SHFCC"))
    )
    ProbationApiExtension.probationApi.verify(
      WireMock.getRequestedFor(WireMock.urlEqualTo("/secure/courts/code/SHFCC"))
        .withHeader("Authorization", WireMock.equalTo("Bearer ABCDE"))
    )
    ProbationApiExtension.probationApi.verify(
      WireMock.postRequestedFor(WireMock.urlEqualTo("/secure/courts"))
        .withHeader("Authorization", WireMock.equalTo("Bearer ABCDE"))
    )
  }

  fun WireMockServer.requestCount(methodType: RequestMethod, url: String): Int {
    return this.findAll(RequestPatternBuilder(methodType, WireMock.urlEqualTo(url))).count()
  }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    val queueAttributes = awsSqsClient.getQueueAttributes(queueName.queueUrl(), listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  fun String.queueUrl(): String = awsSqsClient.getQueueUrl(this).queueUrl
}
