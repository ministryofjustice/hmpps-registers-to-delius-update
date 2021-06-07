package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.model.CourtUpdate
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.ProbationService.CourtToProbationSystem

@Service
class CourtRegisterUpdateService(
  private val courtRegisterService: CourtRegisterService,
  private val probationService: ProbationService
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun updateCourtDetails(court: CourtUpdate) {
    log.info("About to update court $court")
    courtRegisterService.getCourtInfoFromRegister(court.courtId)?.run {
      val dataPayload = CourtToProbationSystem(this)
      probationService.getCourtInformation(courtId)?.let {
        probationService.updateCourt(dataPayload)
      } ?: probationService.insertCourt(dataPayload)
    } ?: log.info("Attempt to get court failed {}", court)
  }
}
