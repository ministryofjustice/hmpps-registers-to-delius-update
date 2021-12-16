package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.listeners

import com.google.gson.Gson
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.helpers.courtRegisterInsertMessage
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.helpers.courtRegisterUpdateMessage
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.model.CourtUpdate
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.CourtRegisterUpdateService
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.ProbationService

internal class HMPPSRegisterListenerTest {
  private val courtRegisterUpdateService: CourtRegisterUpdateService = mock()
  private val probationService: ProbationService = mock()
  private val gson: Gson = Gson()
  private val listener: HMPPSRegisterListener =
    HMPPSRegisterListener(courtRegisterUpdateService = courtRegisterUpdateService, gson = gson)

  @Test
  internal fun `will call service for a court update`() {
    listener.onRegisterChange(courtRegisterUpdateMessage())

    verify(courtRegisterUpdateService).updateCourtDetails(CourtUpdate("SHFCC"))
  }

  @Test
  internal fun `will not call service for events we don't understand`() {
    listener.onRegisterChange(courtRegisterInsertMessage())

    verifyNoMoreInteractions(courtRegisterUpdateService)
    verifyNoMoreInteractions(probationService)
  }
}
