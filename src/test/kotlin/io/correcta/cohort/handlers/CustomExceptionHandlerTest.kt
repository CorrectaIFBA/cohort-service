package io.correcta.cohort.handlers

import io.correcta.cohort.exceptions.BadRequestException
import io.correcta.cohort.exceptions.ConflictException
import io.correcta.cohort.exceptions.ForbiddenException
import io.correcta.cohort.exceptions.InternalServerErrorException
import io.correcta.cohort.exceptions.NotFoundException
import io.correcta.cohort.exceptions.ServiceUnavailableException
import io.correcta.cohort.exceptions.UnauthorizedException
import io.correcta.cohort.models.response.error.ErrorResponse
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.servlet.resource.NoResourceFoundException
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CustomExceptionHandlerTest {

  private val handler = CustomExceptionHandler()

  @Test
  fun `lists validation errors by field`() {
    val bindingResult = BeanPropertyBindingResult(Any(), "request")
    bindingResult.addError(FieldError("request", "name", "O nome é obrigatório"))
    bindingResult.addError(FieldError("request", "slug", null, false, null, null, null))
    val parameter = MethodParameter(Any::class.java.getMethod("toString"), -1)

    val response = handler.handleMethodArgumentNotValidException(MethodArgumentNotValidException(parameter, bindingResult))

    assertEquals(400, response.statusCode.value())
    assertEquals("A validação dos parâmetros da requisição falhou.", response.body!!.message)
    assertEquals(mapOf("name" to "O nome é obrigatório", "slug" to "Valor inválido"), response.body!!.errors)
  }

  @Test
  fun `rejects unreadable bodies`() {
    val response = handler.handleHttpMessageNotReadableException(
      HttpMessageNotReadableException("bad json", MockHttpInputMessage(ByteArray(0))),
    )

    assertError(response, 400, "O corpo da requisição é inválido ou está malformado.")
  }

  @Test
  fun `keeps the message of custom exceptions`() {
    assertError(handler.handleBadRequestException(BadRequestException("bad")), 400, "bad")
    assertError(handler.handleUnauthorizedException(UnauthorizedException("unauthorized")), 401, "unauthorized")
    assertError(handler.handleForbiddenException(ForbiddenException("forbidden")), 403, "forbidden")
    assertError(handler.handleNotFoundException(NotFoundException("missing")), 404, "missing")
    assertError(handler.handleConflictException(ConflictException("conflict")), 409, "conflict")
    assertError(handler.handleServiceUnavailableException(ServiceUnavailableException("down")), 503, "down")
  }

  @Test
  fun `hides the message of other exceptions`() {
    assertError(handler.handleAuthorizationDeniedException(), 403, "Você não tem permissão para acessar este recurso.")
    assertError(
      handler.handleNoResourceFoundException(NoResourceFoundException(HttpMethod.GET, "/missing")),
      404,
      "O recurso solicitado não foi encontrado.",
    )
    assertError(
      handler.handleDataIntegrityViolationException(DataIntegrityViolationException("duplicate key")),
      409,
      "A requisição não pôde ser concluída devido a um conflito com o estado atual do recurso.",
    )
    assertError(
      handler.handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, listOf(MediaType.APPLICATION_JSON))),
      415,
      "O tipo de conteúdo 'text/plain' não é suportado por este endpoint.",
    )
    assertError(
      handler.handleInternalServerErrorException(InternalServerErrorException("secret")),
      500,
      "Ocorreu um erro interno no servidor. Por favor, tente novamente mais tarde.",
    )
    assertError(
      handler.handleGenericException(IllegalStateException("secret")),
      500,
      "Ocorreu um erro inesperado. Por favor, tente novamente mais tarde.",
    )
  }

  @Test
  fun `lists the supported methods`() {
    val response = handler.handleMethodNotSupportedException(HttpRequestMethodNotSupportedException("POST", listOf("GET", "PUT")))

    assertError(response, 405, "O método 'POST' não é suportado por este endpoint.")
    assertEquals("GET, PUT", response.headers.getFirst("Allow"))
  }

  @Test
  fun `omits the Allow header when no method is supported`() {
    val response = handler.handleMethodNotSupportedException(HttpRequestMethodNotSupportedException("POST"))

    assertError(response, 405, "O método 'POST' não é suportado por este endpoint.")
    assertNull(response.headers.getFirst("Allow"))
  }

  private fun assertError(response: org.springframework.http.ResponseEntity<ErrorResponse>, status: Int, message: String) {
    assertEquals(status, response.statusCode.value())
    assertEquals(message, response.body!!.message)
  }
}