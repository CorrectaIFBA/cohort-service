package io.correcta.cohort.controllers

import io.correcta.cohort.controllers.impl.CohortInvitationCodesControllerImpl
import io.correcta.cohort.controllers.impl.CohortInvitationsControllerImpl
import io.correcta.cohort.controllers.impl.CohortMembersControllerImpl
import io.correcta.cohort.controllers.impl.CohortsControllerImpl
import io.correcta.cohort.controllers.impl.InternalCohortMembershipsControllerImpl
import io.correcta.cohort.controllers.impl.MyCohortInvitationsControllerImpl
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.CohortState
import io.correcta.cohort.models.request.ChangeCohortStatusRequest
import io.correcta.cohort.models.request.CohortFilterRequest
import io.correcta.cohort.models.request.CreateCohortInvitationRequest
import io.correcta.cohort.models.request.CreateCohortRequest
import io.correcta.cohort.models.request.CreateInvitationCodeRequest
import io.correcta.cohort.models.request.PaginationRequest
import io.correcta.cohort.models.request.UpdateCohortRequest
import io.correcta.cohort.models.response.GetCohortMembershipResponse
import io.correcta.cohort.service.CohortInvitationCodeService
import io.correcta.cohort.service.CohortInvitationService
import io.correcta.cohort.service.CohortMembershipService
import io.correcta.cohort.service.CohortService
import io.correcta.cohort.support.Fixtures
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import java.time.Instant
import kotlin.test.assertEquals

class ControllersTest {

  private val pagination = PaginationRequest()

  @Test
  fun `delegates cohort calls`() {
    val service = mock<CohortService>()
    val controller = CohortsControllerImpl(service)
    val filter = CohortFilterRequest()
    val create = CreateCohortRequest("Turma")
    val update = UpdateCohortRequest("Turma", "turma")
    val status = ChangeCohortStatusRequest(CohortState.ACTIVE)

    controller.getCohorts(filter, pagination)
    controller.getCohort("cohort-1", "turma")
    controller.getCohortByPublicCode("abc123")
    controller.createCohort(create)
    controller.updateCohort("cohort-1", update)
    controller.changeCohortStatus("cohort-1", status)
    controller.deleteCohort("cohort-1")

    verify(service).getCohorts(filter, pagination)
    verify(service).getCohort("cohort-1", "turma")
    verify(service).getCohortByPublicCode("abc123")
    verify(service).createCohort(create)
    verify(service).updateCohort("cohort-1", update)
    verify(service).changeCohortStatus("cohort-1", status)
    verify(service).deleteCohort("cohort-1")
  }

  @Test
  fun `delegates invitation code calls`() {
    val service = mock<CohortInvitationCodeService>()
    val controller = CohortInvitationCodesControllerImpl(service)
    val request = CreateInvitationCodeRequest(Instant.now())

    controller.generateInvitationCode("cohort-1", request)
    controller.generateInvitationCode("cohort-2", null)
    controller.getInvitationCode("cohort-1")
    controller.revokeInvitationCode("cohort-1")
    controller.joinCohort("CODE")

    verify(service).generateInvitationCode("cohort-1", request)
    verify(service).generateInvitationCode("cohort-2", CreateInvitationCodeRequest())
    verify(service).getInvitationCode("cohort-1")
    verify(service).revokeInvitationCode("cohort-1")
    verify(service).joinCohort("CODE")
  }

  @Test
  fun `delegates invitation calls`() {
    val service = mock<CohortInvitationService>()
    val controller = CohortInvitationsControllerImpl(service)
    val request = CreateCohortInvitationRequest("student", CohortRole.STUDENT)
    val membership = GetCohortMembershipResponse.fromMembership(Fixtures.membership())

    controller.sendInvitation("cohort-1", request)
    controller.getInvitations("cohort-1", pagination)
    controller.searchCandidates("cohort-1", "ana", null)
    controller.revokeInvitation("cohort-1", "student")

    verify(service).sendInvitation("cohort-1", request)
    verify(service).getInvitations("cohort-1", pagination)
    verify(service).searchCandidates("cohort-1", "ana", null)
    verify(service).revokeInvitation("cohort-1", "student")

    whenever(service.acceptInvitation("cohort-1")).doReturn(membership to true)
    assertEquals(HttpStatus.CREATED, controller.acceptInvitation("cohort-1").statusCode)

    whenever(service.acceptInvitation("cohort-1")).doReturn(membership to false)
    val repeated = controller.acceptInvitation("cohort-1")
    assertEquals(HttpStatus.OK, repeated.statusCode)
    assertEquals(membership, repeated.body)
  }

  @Test
  fun `delegates membership calls`() {
    val service = mock<CohortMembershipService>()
    val members = CohortMembersControllerImpl(service)
    val internal = InternalCohortMembershipsControllerImpl(service)

    members.getMembers("cohort-1", pagination)
    members.getFaculty("cohort-1", pagination)
    members.removeMember("cohort-1", "student")
    internal.getCurrentMembership("cohort-1", "student")

    verify(service).getMembers("cohort-1", pagination)
    verify(service).getFaculty("cohort-1", pagination)
    verify(service).removeMember("cohort-1", "student")
    verify(service).getCurrentMembership("cohort-1", "student")
  }

  @Test
  fun `delegates my invitation calls`() {
    val service = mock<CohortInvitationService>()

    MyCohortInvitationsControllerImpl(service).getMyInvitations()

    verify(service).getMyInvitations()
  }
}