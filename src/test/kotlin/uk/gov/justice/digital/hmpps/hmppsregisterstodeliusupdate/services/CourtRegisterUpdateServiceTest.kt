package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.config.GsonConfig
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.model.CourtUpdate

class CourtRegisterUpdateServiceTest {

  private val courtRegisterService: CourtRegisterService = mock()
  private val probationService: ProbationService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val service: CourtRegisterUpdateService = CourtRegisterUpdateService(courtRegisterService, probationService, telemetryClient, GsonConfig().gson())

  @Test
  fun `should perform no update - court not found`() {
    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(null)

    val stats = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(stats.courts).hasSize(0)

    verifyNoMoreInteractions(probationService)
  }

  @Test
  fun `should perform an update`() {
    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(courtRegisterData())
    whenever(probationService.getCourtInformation(eq("SHFCC"))).thenReturn(
      ProbationService.CourtFromProbationSystem(
        "SHFCC", "Sheffield Crown Court",
        true, "Main Building", "Law Street", "Kelham Island", "Sheffield", "South Yorkshire",
        "S1 5TT", "England", "0114 1232311", "0114 1232312",
        courtType = ProbationService.CourtType("CRN", "Crown Court"),
        probationArea = ProbationService.ProbationArea("N02", "North East")
      )
    )
    whenever(probationService.updateCourt(probationUpdateCourtData())).thenReturn(
      ProbationService.CourtFromProbationSystem(
        "SHFCC", "Sheffield Crown Court",
        true, "Main Building", courtType = ProbationService.CourtType("CRN", "Crown Court"),
        probationArea = ProbationService.ProbationArea("N02", "North East")
      )
    )

    val stats = service.updateCourtDetails(CourtUpdate("SHFCC"))
    verify(probationService).updateCourt(probationUpdateCourtData())

    assertThat(stats.courts).hasSize(1)
    val courtStats = stats.courts["SHFCC"]!!
    assertThat(courtStats.differences).isEqualTo("not equal: value differences={buildingName=(Main Building, Main Sheffield Court Building)}")
  }

  @Test
  fun `should perform an insert`() {
    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(courtRegisterData())
    whenever(probationService.getCourtInformation(eq("SHFCC"))).thenReturn(null)
    whenever(probationService.insertCourt(probationInsertCourtData())).thenReturn(
      ProbationService.CourtFromProbationSystem(
        "SHFCC", "Sheffield Crown Court",
        true, "CRT", courtType = ProbationService.CourtType("CRN", "Crown Court"),
        probationArea = ProbationService.ProbationArea("N02", "North East")
      )
    )

    service.updateCourtDetails(CourtUpdate("SHFCC"))
    verify(probationService).insertCourt(probationInsertCourtData())
  }

  private fun probationUpdateCourtData() = ProbationService.CourtDataToSync(
    "SHFCC",
    "Sheffield Crown Court",
    true,
    "CRN",
    "Main Sheffield Court Building",
    "Law Street",
    "Kelham Island",
    "Sheffield",
    "South Yorkshire",
    "S1 5TT",
    "England",
    "0114 1232311",
    "0114 1232312",
    "N02"
  )
  private fun probationInsertCourtData() = ProbationService.CourtDataToSync(
    "SHFCC",
    "Sheffield Crown Court",
    true,
    "CRN",
    "Main Sheffield Court Building",
    "Law Street",
    "Kelham Island",
    "Sheffield",
    "South Yorkshire",
    "S1 5TT",
    "England",
    "0114 1232311",
    "0114 1232312",
    "N01"
  )

  private fun courtRegisterData() = CourtDto(
    "SHFCC",
    "Sheffield Crown Court",
    "Sheffield Crown Court in Sheffield",
    CourtTypeDto("CRN", "Crown Court"),
    true,
    listOf(
      BuildingDto(
        1L,
        "SHFCC",
        null,
        "Main Sheffield Court Building",
        "Law Street",
        "Kelham Island",
        "Sheffield",
        "South Yorkshire",
        "S1 5TT",
        "England",
        listOf(
          ContactDto(1L, "SHFCC", 1L, "TEL", "0114 1232311"),
          ContactDto(2L, "SHFCC", 1L, "FAX", "0114 1232312")
        )
      )
    )
  )
}
