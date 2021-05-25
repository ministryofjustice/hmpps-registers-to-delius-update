package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.model.CourtUpdate
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.ProbationService.CourtForProbationSystem

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
      val dataPayload = CourtForProbationSystem(this)
      probationService.updateCourt(dataPayload)
    }
  }
}
