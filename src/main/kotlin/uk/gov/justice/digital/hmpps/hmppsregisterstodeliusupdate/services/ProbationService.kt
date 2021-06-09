package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
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

  private val courts = object : ParameterizedTypeReference<List<CourtFromProbationSystem>>() {
  }

  fun getCourtInformation(courtId: String): CourtFromProbationSystem? {
    return webClient.get()
      .uri("/secure/courts/code/$courtId")
      .retrieve()
      .bodyToMono(CourtFromProbationSystem::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()
  }

  fun getAllCourts(): List<CourtFromProbationSystem> {
    return webClient.get()
      .uri("/secure/courts")
      .retrieve()
      .bodyToMono(courts)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun insertCourt(newCourt: CourtDataToSync): CourtFromProbationSystem {
    log.debug("Inserting new court information in probation system with {}", newCourt)
    return webClient.post()
      .uri("/secure/courts")
      .bodyValue(newCourt)
      .retrieve()
      .bodyToMono(CourtFromProbationSystem::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenConflict(it) }
      .block()!!
  }

  fun updateCourt(updatedCourt: CourtDataToSync): CourtFromProbationSystem {
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

  data class CourtDataToSync(
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
        courtDto.mainBuilding?.county,
        courtDto.mainBuilding?.postcode,
        courtDto.mainBuilding?.country,
        courtDto.mainBuilding?.contacts?.find { it.type == "TEL" }?.detail,
        courtDto.mainBuilding?.contacts?.find { it.type == "FAX" }?.detail
      )
    constructor(courtFromProbation: CourtFromProbationSystem) :
      this(
        courtFromProbation.code,
        courtFromProbation.courtName,
        courtFromProbation.selectable,
        courtFromProbation.courtType.code,
        courtFromProbation.buildingName,
        courtFromProbation.street,
        courtFromProbation.locality,
        courtFromProbation.town,
        courtFromProbation.county,
        courtFromProbation.postcode,
        courtFromProbation.country,
        courtFromProbation.telephoneNumber,
        courtFromProbation.fax,
        courtFromProbation.probationArea.code
      )

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as CourtDataToSync

      if (code != other.code) return false
      if (courtName != other.courtName) return false
      if (active != other.active) return false
      if (courtTypeCode != other.courtTypeCode) return false
      if (buildingName != other.buildingName) return false
      if (street != other.street) return false
      if (locality != other.locality) return false
      if (town != other.town) return false
      if (county != other.county) return false
      if (postcode != other.postcode) return false
      if (country != other.country) return false
      if (telephoneNumber != other.telephoneNumber) return false
      if (fax != other.fax) return false
      if (probationAreaCode != other.probationAreaCode) return false

      return true
    }

    override fun hashCode(): Int {
      var result = code.hashCode()
      result = 31 * result + courtName.hashCode()
      result = 31 * result + active.hashCode()
      result = 31 * result + (courtTypeCode?.hashCode() ?: 0)
      result = 31 * result + (buildingName?.hashCode() ?: 0)
      result = 31 * result + (street?.hashCode() ?: 0)
      result = 31 * result + (locality?.hashCode() ?: 0)
      result = 31 * result + (town?.hashCode() ?: 0)
      result = 31 * result + (county?.hashCode() ?: 0)
      result = 31 * result + (postcode?.hashCode() ?: 0)
      result = 31 * result + (country?.hashCode() ?: 0)
      result = 31 * result + (telephoneNumber?.hashCode() ?: 0)
      result = 31 * result + (fax?.hashCode() ?: 0)
      result = 31 * result + probationAreaCode.hashCode()
      return result
    }
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
