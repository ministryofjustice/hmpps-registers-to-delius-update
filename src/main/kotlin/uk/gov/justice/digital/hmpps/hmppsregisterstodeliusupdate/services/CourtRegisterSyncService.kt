package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.ProbationService.CourtFromProbationSystem

@Service
class CourtRegisterSyncService(
  private val courtRegisterUpdateService: CourtRegisterUpdateService,
  private val courtRegisterService: CourtRegisterService,
  private val probationService: ProbationService
) {

  fun sync(): SyncStatistics {
    return syncAllCourts(probationService.getAllCourts(), courtRegisterService.getAllActiveCourts())
  }

  private fun syncAllCourts(
    probationCourts: List<CourtFromProbationSystem>,
    courtRegisterCourts: List<CourtDto>
  ): SyncStatistics {

    // get all active / inactive courts from Probation (DELIUS)
    val allCourtsHeldInProbation = probationCourts.map { ProbationService.CourtDataToSync(it) }.associateBy { it.code }

    // get all the courts from the register
    val allRegisteredCourts: MutableList<ProbationService.CourtDataToSync> = mutableListOf()
    courtRegisterCourts.forEach {
      allRegisteredCourts.add(ProbationService.CourtDataToSync(it))
    }
    val courtRegisterMap = allRegisteredCourts.associateBy { it.code }

    val stats = SyncStatistics()
    // matches
    courtRegisterMap.filter { c -> allCourtsHeldInProbation[c.key] != null }
      .forEach {
        // Ensure the probation Area is set todo - evaluate the value from the
        val courtFromRegisterWithProbationArea = it.value.copy(probationAreaCode = allCourtsHeldInProbation[it.key]!!.probationAreaCode)
        courtRegisterUpdateService.syncCourt(allCourtsHeldInProbation[it.key], courtFromRegisterWithProbationArea, stats)
      }

    // new
    courtRegisterMap.filter { c -> allCourtsHeldInProbation[c.key] == null }
      .forEach { courtRegisterUpdateService.syncCourt(null, it.value, stats) }

    // not there / inactive
    allCourtsHeldInProbation.filter { c -> c.value.active && courtRegisterMap[c.key] == null }
      .forEach {
        courtRegisterUpdateService.syncCourt(
          allCourtsHeldInProbation[it.key],
          it.value.copy(active = false),
          stats
        )
      }

    return stats
  }
}
