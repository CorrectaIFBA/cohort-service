package io.correcta.cohort

import io.correcta.cohort.clients.IdentityClient
import io.correcta.cohort.mailer.requests.SendTransactionalMailRequest
import io.correcta.cohort.models.request.clients.ProfileBatchLookupRequest
import io.correcta.cohort.models.response.clients.InstitutionalProfileResponse
import io.correcta.cohort.models.response.clients.ProfileBatchLookupResponse
import io.correcta.cohort.security.models.IdentityHeaders
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CohortFlowTests(
  @Autowired val mockMvc: MockMvc,
) {

  @MockitoBean
  lateinit var identityClient: IdentityClient

  @MockitoBean
  lateinit var rabbitTemplate: RabbitTemplate

  @BeforeEach
  fun mockIdentity() {
    `when`(identityClient.getProfile(anyString())).thenAnswer { profile(it.getArgument(0)) }
    `when`(identityClient.getProfiles(anyOf(ProfileBatchLookupRequest::class.java))).thenAnswer {
      val request = it.getArgument<ProfileBatchLookupRequest>(0)
      ProfileBatchLookupResponse(request.profileIds.map(::profile), emptyList())
    }
    `when`(identityClient.searchProfiles(anyString(), any(), anyInt())).thenAnswer {
      listOf(profile("student-invite"), profile("student-free"))
    }
  }

  @Test
  fun `enforces cohort status transitions and draft-only deletion`() {
    val cohortId = createActiveCohort("faculty-status")

    mockMvc.patch("/api/v1/cohorts/$cohortId/status") {
      asProfile("faculty-status", "FACULTY")
      json("""{"status": "DRAFT"}""")
    }.andExpect { status { isConflict() } }

    mockMvc.delete("/api/v1/cohorts/$cohortId") {
      asProfile("faculty-status", "FACULTY")
    }.andExpect { status { isConflict() } }
  }

  @Test
  fun `joins through invitation code until it is revoked`() {
    val cohortId = createActiveCohort("faculty-code")

    val code = mockMvc.post("/api/v1/cohorts/$cohortId/invitation-code") {
      asProfile("faculty-code", "FACULTY")
    }.andExpect {
      status { isCreated() }
      jsonPath("$.code") { value(Matchers.matchesPattern("[23456789ABCDEFGHJKMNPQRSTVWXYZ]{8}")) }
    }.extract("code")

    mockMvc.post("/api/v1/cohort-invitation-codes/${code.lowercase().chunked(4).joinToString("-")}/join") {
      asProfile("student-code-1", "STUDENT")
    }.andExpect {
      status { isCreated() }
      jsonPath("$.role") { value("STUDENT") }
    }

    mockMvc.post("/api/v1/cohort-invitation-codes/$code/join") {
      asProfile("student-code-1", "STUDENT")
    }.andExpect { status { isConflict() } }

    mockMvc.get("/api/v1/cohorts/$cohortId/members") {
      asProfile("student-code-1", "STUDENT")
    }.andExpect {
      status { isOk() }
      jsonPath("$.totalElements") { value(2) }
      jsonPath("$.content[0].displayName") { exists() }
    }

    mockMvc.get("/api/v1/cohorts/$cohortId/invitation-code") {
      asProfile("faculty-code", "FACULTY")
    }.andExpect {
      status { isOk() }
      jsonPath("$.code") { value(code) }
    }

    mockMvc.get("/api/v1/cohorts/$cohortId/invitation-code") {
      asProfile("student-code-1", "STUDENT")
    }.andExpect { status { isForbidden() } }

    mockMvc.delete("/api/v1/cohorts/$cohortId/invitation-code") {
      asProfile("faculty-code", "FACULTY")
    }.andExpect { status { isNoContent() } }

    mockMvc.post("/api/v1/cohort-invitation-codes/$code/join") {
      asProfile("student-code-2", "STUDENT")
    }.andExpect { status { isNotFound() } }
  }

  @Test
  fun `accepts direct invitations idempotently and protects the owner`() {
    val cohortId = createActiveCohort("faculty-invite")

    mockMvc.post("/api/v1/cohorts/$cohortId/invitations") {
      asProfile("faculty-invite", "FACULTY")
      json("""{"profileId": "student-invite", "role": "FACULTY"}""")
    }.andExpect { status { isBadRequest() } }

    mockMvc.post("/api/v1/cohorts/$cohortId/invitations") {
      asProfile("faculty-invite", "FACULTY")
      json("""{"profileId": "student-invite", "role": "STUDENT"}""")
    }.andExpect {
      status { isCreated() }
      jsonPath("$.status") { value("PENDING") }
    }

    mockMvc.post("/api/v1/cohorts/$cohortId/invitations") {
      asProfile("faculty-invite", "FACULTY")
      json("""{"profileId": "student-invite", "role": "STUDENT"}""")
    }.andExpect { status { isConflict() } }

    mockMvc.get("/api/v1/cohorts/$cohortId/invitations/candidates?q=s") {
      asProfile("faculty-invite", "FACULTY")
    }.andExpect { status { isBadRequest() } }

    mockMvc.get("/api/v1/cohorts/$cohortId/invitations/candidates?q=student&role=STUDENT") {
      asProfile("student-free", "STUDENT")
    }.andExpect { status { isNotFound() } }

    mockMvc.get("/api/v1/cohorts/$cohortId/invitations/candidates?q=student&role=STUDENT") {
      asProfile("faculty-invite", "FACULTY")
    }.andExpect {
      status { isOk() }
      jsonPath("$[0].profileId") { value("student-invite") }
      jsonPath("$[0].status") { value("INVITED") }
      jsonPath("$[1].status") { value("AVAILABLE") }
    }

    verify(rabbitTemplate).convertAndSend(
      anyString(),
      anyString(),
      argThat<Any> { (it as SendTransactionalMailRequest).to.single().email == "student-invite@example.edu" },
    )

    mockMvc.get("/api/v1/cohort-invitations") {
      asProfile("student-invite", "STUDENT")
    }.andExpect {
      status { isOk() }
      jsonPath("$.length()") { value(1) }
      jsonPath("$[0].cohort.id") { value(cohortId) }
      jsonPath("$[0].role") { value("STUDENT") }
      jsonPath("$[0].invitedByName") { value("Perfil faculty-invite") }
    }

    mockMvc.get("/api/v1/cohort-invitations") {
      asProfile("student-other", "STUDENT")
    }.andExpect { jsonPath("$.length()") { value(0) } }

    mockMvc.post("/api/v1/cohorts/$cohortId/invitations/accept") {
      asProfile("student-invite", "STUDENT")
    }.andExpect { status { isCreated() } }

    mockMvc.get("/api/v1/cohort-invitations") {
      asProfile("student-invite", "STUDENT")
    }.andExpect { jsonPath("$.length()") { value(0) } }

    mockMvc.post("/api/v1/cohorts/$cohortId/invitations/accept") {
      asProfile("student-invite", "STUDENT")
    }.andExpect { status { isOk() } }

    mockMvc.get("/api/v1/cohorts/$cohortId/faculty") {
      asProfile("student-invite", "STUDENT")
    }.andExpect { jsonPath("$.totalElements") { value(1) } }

    mockMvc.delete("/api/v1/cohorts/$cohortId/members/faculty-invite") {
      asProfile("faculty-invite", "FACULTY")
    }.andExpect { status { isConflict() } }

    mockMvc.delete("/api/v1/cohorts/$cohortId/members/student-invite") {
      asProfile("faculty-invite", "FACULTY")
    }.andExpect { status { isNoContent() } }
  }

  @Test
  fun `hides the cohort from students who are not current members`() {
    val cohortId = createActiveCohort("faculty-hidden")
    val slug = mockMvc.get("/api/v1/cohorts") {
      asProfile("faculty-hidden", "FACULTY")
    }.andReturn().response.contentAsString.let { Regex("\"slug\":\"([^\"]+)\"").find(it)!!.groupValues[1] }

    mockMvc.post("/api/v1/cohorts/$cohortId/invitations") {
      asProfile("faculty-hidden", "FACULTY")
      json("""{"profileId": "student-invited", "role": "STUDENT"}""")
    }.andExpect { status { isCreated() } }

    mockMvc.post("/api/v1/cohorts/$cohortId/invitations") {
      asProfile("faculty-hidden", "FACULTY")
      json("""{"profileId": "student-removed", "role": "STUDENT"}""")
    }.andExpect { status { isCreated() } }
    mockMvc.post("/api/v1/cohorts/$cohortId/invitations/accept") {
      asProfile("student-removed", "STUDENT")
    }.andExpect { status { isCreated() } }
    mockMvc.delete("/api/v1/cohorts/$cohortId/members/student-removed") {
      asProfile("faculty-hidden", "FACULTY")
    }.andExpect { status { isNoContent() } }

    val publicCode = mockMvc.get("/api/v1/cohorts") {
      asProfile("faculty-hidden", "FACULTY")
    }.andReturn().response.contentAsString.let { Regex("\"publicCode\":\"([^\"]+)\"").find(it)!!.groupValues[1] }

    mockMvc.get("/api/v1/cohorts/by-code/$publicCode") {
      asProfile("faculty-hidden", "FACULTY")
    }.andExpect {
      status { isOk() }
      jsonPath("$.id") { value(cohortId) }
    }

    for (outsider in listOf("student-invited", "student-removed", "student-stranger")) {
      mockMvc.get("/api/v1/cohorts/$cohortId/$slug") {
        asProfile(outsider, "STUDENT")
      }.andExpect { status { isNotFound() } }

      mockMvc.get("/api/v1/cohorts/by-code/$publicCode") {
        asProfile(outsider, "STUDENT")
      }.andExpect { status { isNotFound() } }

      mockMvc.get("/api/v1/cohorts") {
        asProfile(outsider, "STUDENT")
      }.andExpect { jsonPath("$.totalElements") { value(0) } }
    }
  }

  @Test
  fun `lets staff read every cohort without managing it`() {
    val cohortId = createActiveCohort("faculty-staffview")
    mockMvc.post("/api/v1/cohorts/$cohortId/invitation-code") {
      asProfile("faculty-staffview", "FACULTY")
    }.andExpect { status { isCreated() } }

    mockMvc.get("/api/v1/cohorts?search=staffview") {
      asProfile("staff-a", "STAFF")
    }.andExpect {
      status { isOk() }
      jsonPath("$.totalElements") { value(1) }
      jsonPath("$.content[0].id") { value(cohortId) }
    }

    mockMvc.get("/api/v1/cohorts?memberProfileId=faculty-staffview") {
      asProfile("staff-a", "STAFF")
    }.andExpect { jsonPath("$.content[?(@.id == '$cohortId')]") { exists() } }

    mockMvc.get("/api/v1/cohorts?memberProfileId=nobody") {
      asProfile("staff-a", "STAFF")
    }.andExpect { jsonPath("$.totalElements") { value(0) } }

    mockMvc.get("/api/v1/cohorts/$cohortId/turma-faculty-staffview") {
      asProfile("staff-a", "STAFF")
    }.andExpect { status { isOk() } }

    mockMvc.get("/api/v1/cohorts/$cohortId/members") {
      asProfile("staff-a", "STAFF")
    }.andExpect {
      status { isOk() }
      jsonPath("$.totalElements") { value(1) }
    }

    mockMvc.get("/api/v1/cohorts/$cohortId/invitation-code") {
      asProfile("staff-a", "STAFF")
    }.andExpect { status { isNotFound() } }

    mockMvc.patch("/api/v1/cohorts/$cohortId/status") {
      asProfile("staff-a", "STAFF")
      json("""{"status": "ARCHIVED"}""")
    }.andExpect { status { isForbidden() } }

    mockMvc.get("/api/v1/cohorts?search=staffview") {
      asProfile("student-staffview", "STUDENT")
    }.andExpect { jsonPath("$.totalElements") { value(0) } }
  }

  @Test
  fun `exposes current memberships to internal callers without authentication`() {
    val cohortId = createActiveCohort("faculty-internal")

    mockMvc.get("/api/v1/internal/cohorts/$cohortId/members/faculty-internal")
      .andExpect {
        status { isOk() }
        jsonPath("$.cohortStatus") { value("ACTIVE") }
        jsonPath("$.role") { value("FACULTY") }
      }

    mockMvc.get("/api/v1/internal/cohorts/$cohortId/members/student-outsider")
      .andExpect { status { isNotFound() } }
  }

  private fun createActiveCohort(ownerProfileId: String): String {
    val cohortId = mockMvc.post("/api/v1/cohorts") {
      asProfile(ownerProfileId, "FACULTY")
      json("""{"name": "Turma $ownerProfileId"}""")
    }.andExpect { status { isCreated() } }.extract("id")

    mockMvc.patch("/api/v1/cohorts/$cohortId/status") {
      asProfile(ownerProfileId, "FACULTY")
      json("""{"status": "ACTIVE"}""")
    }.andExpect { status { isOk() } }

    return cohortId
  }

  private fun MockHttpServletRequestDsl.asProfile(profileId: String, profileType: String) {
    header(IdentityHeaders.PROFILE_ID, profileId)
    header(IdentityHeaders.PROFILE_TYPE, profileType)
  }

  private fun MockHttpServletRequestDsl.json(body: String) {
    contentType = MediaType.APPLICATION_JSON
    content = body
  }

  private fun ResultActionsDsl.extract(field: String): String {
    val body = andReturn().response.contentAsString
    return Regex("\"$field\":\"([^\"]+)\"").find(body)!!.groupValues[1]
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> anyOf(type: Class<T>): T {
    any(type)
    return null as T
  }

  private fun profile(profileId: String): InstitutionalProfileResponse = InstitutionalProfileResponse(
    profileId = profileId,
    type = if (profileId.startsWith("faculty")) "FACULTY" else "STUDENT",
    state = "ACTIVE",
    profileActive = true,
    credentialActive = true,
    displayName = "Perfil $profileId",
    email = "$profileId@example.edu",
    login = "$profileId@example.edu",
  )
}