package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.config.GsonConfig

class CourtRegisterSyncServiceTest {

  private val courtRegisterService: CourtRegisterService = mock()
  private val probationService: ProbationService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private lateinit var service: CourtRegisterSyncService

  @BeforeEach
  fun before() {
    val courtRegisterUpdateService =
      CourtRegisterUpdateService(courtRegisterService, probationService, telemetryClient, GsonConfig().gson())

    service = CourtRegisterSyncService(courtRegisterUpdateService, courtRegisterService, probationService)
  }

  @Test
  fun `inserts and updates courts`() {
    val courtRegisterData = listOf(
      generateCourtRegisterEntry("SHFCC", "Sheffield Crown Court"),
      generateCourtRegisterEntry("SHFC1", "Sheffield Crown Court 1"),
      generateCourtRegisterEntry("SHFC2", "Sheffield Crown Court 2")
    )

    whenever(courtRegisterService.getAllActiveCourts()).thenReturn(courtRegisterData)

    whenever(probationService.getAllCourts()).thenReturn(
      listOf(
        courtFromProbationSystem("SHFCC", "Sheffield Crown Court"),
        courtFromProbationSystem("SHFC3", "Sheffield Crown Court 3")
      )
    )
    val stats = service.sync()
    assertThat(stats.courts).hasSize(3) // Nothing for SHFCC as there are no changes

    assertThat(stats.courts["SHFC1"]?.updateType).isEqualTo(CourtDifferences.UpdateType.INSERT)
    assertThat(stats.courts["SHFC2"]?.updateType).isEqualTo(CourtDifferences.UpdateType.INSERT)
    assertThat(stats.courts["SHFC3"]?.updateType).isEqualTo(CourtDifferences.UpdateType.UPDATE)

    assertThat(stats.courts["SHFC1"]?.differences).contains("not equal: only on right")
    assertThat(stats.courts["SHFC2"]?.differences).contains("not equal: only on right")
    assertThat(stats.courts["SHFC3"]?.differences).contains("value differences={active=(true, false)}")
  }

  private fun courtFromProbationSystem(courtId: String, name: String) = ProbationService.CourtFromProbationSystem(
    courtId, name, true,
    "Main Sheffield Court Building", "Law Street", "Kelham Island", "Sheffield",
    "South Yorkshire", "S1 5TT", "England", "0114 1232311", "0114 1232312",
    ProbationService.ProbationArea("N02", "North East"), ProbationService.CourtType("CRN", "Crown Court")
  )

  private fun generateCourtRegisterEntry(courtId: String, name: String) = CourtDto(
    courtId,
    name,
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
