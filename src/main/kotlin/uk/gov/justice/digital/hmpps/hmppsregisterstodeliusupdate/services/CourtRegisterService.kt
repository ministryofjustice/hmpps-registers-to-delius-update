package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Service
class CourtRegisterService(@Qualifier("courtRegisterApiWebClient") private val webClient: WebClient) {

  private val courtsType = object : ParameterizedTypeReference<List<CourtDto>>() {}

  fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, NOT_FOUND)
  fun <T> emptyWhen(exception: WebClientResponseException, statusCode: HttpStatus): Mono<T> =
    if (exception.rawStatusCode == statusCode.value()) Mono.empty() else Mono.error(exception)

  fun getCourtInfoFromRegister(courtId: String): CourtDto? {
    return webClient.get()
      .uri("/courts/id/$courtId")
      .retrieve()
      .bodyToMono(CourtDto::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()
  }

  fun getAllActiveCourts(): List<CourtDto> {
    return webClient.get()
      .uri("/courts")
      .retrieve()
      .bodyToMono(courtsType)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }
}

data class CourtDto(
  val courtId: String,
  val courtName: String,
  val courtDescription: String?,
  val type: CourtTypeDto,
  val active: Boolean,
  val buildings: List<BuildingDto> = listOf()
) {
  val mainBuilding: BuildingDto?
    get() = this.buildings.firstOrNull { it.isMainBuilding }
}

data class CourtTypeDto(
  val courtType: String,
  val courtName: String
)

data class BuildingDto(
  val id: Long,
  val courtId: String,
  val subCode: String?, // null indicates main building
  val buildingName: String?,
  val street: String?,
  val locality: String?,
  val town: String?,
  val county: String?,
  val postcode: String?,
  val country: String?,
  val contacts: List<ContactDto> = listOf(),

) {
  val isMainBuilding: Boolean
    get() = this.subCode == null
}

data class ContactDto(
  val id: Long,
  val courtId: String,
  val buildingId: Long,
  val type: String,
  val detail: String
)
