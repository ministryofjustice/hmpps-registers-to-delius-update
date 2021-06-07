package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Service
class ProbationService(@Qualifier("probationApiWebClient") private val webClient: WebClient) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getCourtInformation(courtId: String): CourtFromProbationSystem? {
    return webClient.get()
      .uri("/secure/courts/code/$courtId")
      .retrieve()
      .bodyToMono(CourtFromProbationSystem::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()
  }

  fun insertCourt(newCourt: CourtForProbationSystem): CourtFromProbationSystem {
    log.debug("Inserting new court information in probation system with {}", newCourt)
    return webClient.post()
      .uri("/secure/courts")
      .bodyValue(newCourt)
      .retrieve()
      .bodyToMono(CourtFromProbationSystem::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenConflict(it) }
      .block()!!
  }

  fun updateCourt(updatedCourt: CourtForProbationSystem): CourtFromProbationSystem {
    log.debug("Updating court information in probation system with {}", updatedCourt)
    return webClient.put()
      .uri("/secure/courts/code/${updatedCourt.code}")
      .bodyValue(updatedCourt)
      .retrieve()
      .bodyToMono(CourtFromProbationSystem::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun <T> emptyWhenConflict(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, HttpStatus.CONFLICT)
  fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, NOT_FOUND)
  fun <T> emptyWhen(exception: WebClientResponseException, statusCode: HttpStatus): Mono<T> =
    if (exception.rawStatusCode == statusCode.value()) Mono.empty() else Mono.error(exception)

  data class CourtForProbationSystem(
    val code: String,
    val courtName: String,
    val active: Boolean = false,
    val courtTypeCode: String? = "MAG",
    val buildingName: String? = null,
    val street: String? = null,
    val locality: String? = null,
    val town: String? = null,
    val county: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val telephoneNumber: String? = null,
    val fax: String? = null,
    val probationAreaCode: String = "N01"
  ) {
    constructor(courtDto: CourtDto) :
      this(
        courtDto.courtId,
        courtDto.courtName,
        courtDto.active,
        courtDto.type.courtType,
        courtDto.mainBuilding?.buildingName,
        courtDto.mainBuilding?.street,
        courtDto.mainBuilding?.locality,
        courtDto.mainBuilding?.town,
        courtDto.mainBuilding?.postcode,
        courtDto.mainBuilding?.county,
        courtDto.mainBuilding?.country,
        courtDto.mainBuilding?.contacts?.find { it.type == "TEL" }?.detail,
        courtDto.mainBuilding?.contacts?.find { it.type == "FAX" }?.detail
      )
  }

  data class CourtFromProbationSystem(
    val code: String,
    val courtName: String,
    val selectable: Boolean = false,
    val buildingName: String? = null,
    val street: String? = null,
    val locality: String? = null,
    val town: String? = null,
    val county: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val telephoneNumber: String? = null,
    val fax: String? = null,
    val probationArea: ProbationArea,
    val courtType: CourtType
  )
  data class ProbationArea(
    val code: String,
    val description: String
  )
  data class CourtType(
    val code: String,
    val description: String
  )
}
