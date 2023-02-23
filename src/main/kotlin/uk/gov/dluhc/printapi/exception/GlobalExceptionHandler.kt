package uk.gov.dluhc.printapi.exception

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import uk.gov.dluhc.printapi.config.ApiRequestErrorAttributes
import javax.servlet.RequestDispatcher

@ControllerAdvice
class GlobalExceptionHandler(
    private var errorAttributes: ApiRequestErrorAttributes
) : ResponseEntityExceptionHandler() {

    @ExceptionHandler(value = [CertificateNotFoundException::class, ExplainerDocumentNotFoundException::class])
    fun handleResourceNotFound(e: RuntimeException, request: WebRequest): ResponseEntity<Any?>? {
        return handleExceptionInternal(e, e.message, HttpHeaders(), NOT_FOUND, request)
    }

    @ExceptionHandler(
        value = [
            GenerateTemporaryCertificateValidationException::class,
            GenerateAnonymousElectorDocumentValidationException::class
        ]
    )
    fun handleRequestValidationException(e: RuntimeException, request: WebRequest): ResponseEntity<Any> {
        return populateErrorResponseAndHandleExceptionInternal(e, BAD_REQUEST, request)
    }

    override fun handleHttpMessageNotReadable(
        e: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        return populateErrorResponseAndHandleExceptionInternal(e, status, request)
    }

    override fun handleMethodArgumentNotValid(
        e: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        return populateErrorResponseAndHandleExceptionInternal(e, status, request)
    }

    private fun populateErrorResponseAndHandleExceptionInternal(
        exception: Exception,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status.value(), RequestAttributes.SCOPE_REQUEST)
        val body = errorAttributes.getErrorResponse(request)
        return handleExceptionInternal(exception, body, HttpHeaders(), status, request)
    }
}
