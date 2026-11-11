package io.correcta.cohort.service

import feign.FeignException
import io.correcta.cohort.clients.IdentityClient
import io.correcta.cohort.exceptions.ServiceUnavailableException
import io.correcta.cohort.models.request.clients.ProfileBatchLookupRequest
import io.correcta.cohort.models.response.clients.ProfileBatchLookupResponse
import io.correcta.cohort.support.Fixtures
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IdentityProfileServiceTest {

  private val identityClient = mock<IdentityClient>()
  private val service = IdentityProfileService(identityClient)

  @Test
  fun `finds one profile`() {
    whenever(identityClient.getProfile("student")).doReturn(Fixtures.profile())

    assertEquals("student", service.findProfile("student")!!.profileId)
  }

  @Test
  fun `returns null for unknown profiles`() {
    whenever(identityClient.getProfile("missing")).doThrow(mock<FeignException.NotFound>())

    assertNull(service.findProfile("missing"))
  }

  @Test
  fun `fails closed when identity-service fails`() {
    whenever(identityClient.getProfile(any())).doThrow(mock<FeignException.InternalServerError>())
    whenever(identityClient.getProfiles(any())).doThrow(mock<FeignException.BadGateway>())
    whenever(identityClient.searchProfiles(any(), anyOrNull(), any())).doThrow(mock<FeignException.ServiceUnavailable>())

    assertThrows<ServiceUnavailableException> { service.findProfile("student") }
    assertThrows<ServiceUnavailableException> { service.findProfiles(listOf("student")) }
    assertThrows<ServiceUnavailableException> { service.searchProfiles("stu", null, 10) }
  }

  @Test
  fun `finds profiles in batch without duplicates`() {
    whenever(identityClient.getProfiles(ProfileBatchLookupRequest(listOf("a", "b"))))
      .doReturn(ProfileBatchLookupResponse(listOf(Fixtures.profile("a")), listOf("b")))

    assertEquals(setOf("a"), service.findProfiles(listOf("a", "b", "a")).keys)
  }

  @Test
  fun `skips the batch call without profile IDs`() {
    assertEquals(emptyMap(), service.findProfiles(emptyList()))
    verifyNoInteractions(identityClient)
  }

  @Test
  fun `searches profiles`() {
    whenever(identityClient.searchProfiles("stu", "STUDENT", 10)).doReturn(listOf(Fixtures.profile()))

    assertEquals(1, service.searchProfiles("stu", "STUDENT", 10).size)
  }
}