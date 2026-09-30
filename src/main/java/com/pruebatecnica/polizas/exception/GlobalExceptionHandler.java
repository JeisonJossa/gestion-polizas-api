package com.pruebatecnica.polizas.exception;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.pruebatecnica.polizas.controller.Respuestas;
import com.pruebatecnica.polizas.dto.ErrorDetalle;
import com.pruebatecnica.polizas.dto.RespuestaApi;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Traduce las excepciones a respuestas con la forma común del API: resultado -1, un mensaje general y la lista de
 * errores con su código. El código HTTP sigue diciendo qué tipo de error fue.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final HttpStatusCode UNPROCESSABLE = HttpStatusCode.valueOf(422);

	private final Respuestas respuestas;

	public GlobalExceptionHandler(Respuestas respuestas) {
		this.respuestas = respuestas;
	}

	@ExceptionHandler(NoAutorizadoException.class)
	ResponseEntity<RespuestaApi<Void>> noAutorizado(NoAutorizadoException e, HttpServletRequest request) {
		return responder(request, HttpStatus.UNAUTHORIZED, "No autorizado.", "NO_AUTORIZADO", e.getMessage());
	}

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ResponseEntity<RespuestaApi<Void>> noEncontrado(RecursoNoEncontradoException e, HttpServletRequest request) {
		return responder(request, HttpStatus.NOT_FOUND, "El recurso no existe.", "NO_ENCONTRADO", e.getMessage());
	}

	@ExceptionHandler(ProcesoInvalidoException.class)
	ResponseEntity<RespuestaApi<Void>> procesoInvalido(ProcesoInvalidoException e, HttpServletRequest request) {
		return responder(request, UNPROCESSABLE, "El tipoProceso no corresponde a la operación.", "PROCESO_INVALIDO",
				e.getMessage());
	}

	@ExceptionHandler(ReglaNegocioException.class)
	ResponseEntity<RespuestaApi<Void>> reglaNegocio(ReglaNegocioException e, HttpServletRequest request) {
		return responder(request, UNPROCESSABLE, "La operación no cumple una regla de negocio.", "REGLA_DE_NEGOCIO",
				e.getMessage());
	}

	@ExceptionHandler(EstadoInvalidoException.class)
	ResponseEntity<RespuestaApi<Void>> estadoInvalido(EstadoInvalidoException e, HttpServletRequest request) {
		return responder(request, HttpStatus.CONFLICT, "El estado actual no permite la operación.", "ESTADO_INVALIDO",
				e.getMessage());
	}

	/** Dos operaciones crearon a la vez el mismo número de endoso: la llave de POLIZA rechazó la segunda. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<RespuestaApi<Void>> conflicto(DataIntegrityViolationException e, HttpServletRequest request) {
		log.warn("Conflicto al guardar un endoso: {}", e.getMostSpecificCause().getMessage());
		return responder(request, HttpStatus.CONFLICT, "La póliza cambió por otra operación al mismo tiempo.",
				"CONFLICTO", "Consulte la póliza e intente de nuevo.");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<RespuestaApi<Void>> datosInvalidos(MethodArgumentNotValidException e, HttpServletRequest request) {
		List<ErrorDetalle> errores = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.sorted()
				.map(detalle -> new ErrorDetalle("DATOS_INVALIDOS", detalle))
				.toList();
		return ResponseEntity.badRequest()
				.body(respuestas.error(request, "La petición tiene datos inválidos.", errores));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<RespuestaApi<Void>> parametroInvalido(MethodArgumentTypeMismatchException e,
			HttpServletRequest request) {
		String detalle = "El parámetro '" + e.getName() + "' no acepta el valor '" + e.getValue() + "'.";
		Class<?> tipo = e.getRequiredType();
		if (tipo != null && tipo.isEnum()) {
			detalle += " Valores permitidos: " + Arrays.stream(tipo.getEnumConstants()).map(String::valueOf)
					.collect(Collectors.joining(", ")) + ".";
		}
		return responder(request, HttpStatus.BAD_REQUEST, "La petición tiene datos inválidos.", "DATOS_INVALIDOS",
				detalle);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<RespuestaApi<Void>> cuerpoIlegible(HttpMessageNotReadableException e, HttpServletRequest request) {
		return responder(request, HttpStatus.BAD_REQUEST, "La petición tiene datos inválidos.", "DATOS_INVALIDOS",
				"El cuerpo de la petición no es un JSON válido o tiene un valor no permitido.");
	}

	/** Errores estándar de Spring MVC (ruta inexistente, método no permitido...) y cualquier otro inesperado. */
	@ExceptionHandler(Exception.class)
	ResponseEntity<RespuestaApi<Void>> otro(Exception e, HttpServletRequest request) {
		if (e instanceof ErrorResponse error) {
			String detalle = error.getBody().getDetail();
			return responder(request, error.getStatusCode(), "La solicitud no es válida.", "SOLICITUD_INVALIDA",
					detalle != null ? detalle : e.getMessage());
		}
		log.error("Error inesperado", e);
		return responder(request, HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado.", "ERROR_INTERNO",
				"Revise el log con el idTransaccion de esta respuesta.");
	}

	private ResponseEntity<RespuestaApi<Void>> responder(HttpServletRequest request, HttpStatusCode estado,
			String mensaje, String codigo, String detalle) {
		return ResponseEntity.status(estado)
				.body(respuestas.error(request, mensaje, List.of(new ErrorDetalle(codigo, detalle))));
	}
}
