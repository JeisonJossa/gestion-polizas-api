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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.pruebatecnica.polizas.dto.ErrorResponse;

/** Traduce las excepciones a respuestas con un código y un mensaje claros. */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final HttpStatusCode UNPROCESSABLE = HttpStatusCode.valueOf(422);

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException e) {
		return responder(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage());
	}

	@ExceptionHandler(ReglaNegocioException.class)
	ResponseEntity<ErrorResponse> reglaNegocio(ReglaNegocioException e) {
		return responder(UNPROCESSABLE, "REGLA_DE_NEGOCIO", e.getMessage());
	}

	@ExceptionHandler(EstadoInvalidoException.class)
	ResponseEntity<ErrorResponse> estadoInvalido(EstadoInvalidoException e) {
		return responder(HttpStatus.CONFLICT, "ESTADO_INVALIDO", e.getMessage());
	}

	/** Dos operaciones crearon a la vez el mismo número de endoso: la llave de POLIZA rechazó la segunda. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ErrorResponse> conflicto(DataIntegrityViolationException e) {
		log.warn("Conflicto al guardar un endoso: {}", e.getMostSpecificCause().getMessage());
		return responder(HttpStatus.CONFLICT, "CONFLICTO",
				"La póliza cambió por otra operación al mismo tiempo; consúltela e intente de nuevo.");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> datosInvalidos(MethodArgumentNotValidException e) {
		List<String> detalles = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.sorted()
				.toList();
		return ResponseEntity.badRequest()
				.body(new ErrorResponse("DATOS_INVALIDOS", "La petición tiene datos inválidos.", detalles));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErrorResponse> parametroInvalido(MethodArgumentTypeMismatchException e) {
		String mensaje = "El parámetro '" + e.getName() + "' no acepta el valor '" + e.getValue() + "'.";
		Class<?> tipo = e.getRequiredType();
		if (tipo != null && tipo.isEnum()) {
			mensaje += " Valores permitidos: " + Arrays.stream(tipo.getEnumConstants()).map(String::valueOf)
					.collect(Collectors.joining(", ")) + ".";
		}
		return responder(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", mensaje);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException e) {
		return responder(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS",
				"El cuerpo de la petición no es un JSON válido o tiene un valor no permitido.");
	}

	/** Errores estándar de Spring MVC (ruta inexistente, método no permitido...) y cualquier otro inesperado. */
	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> otro(Exception e) {
		if (e instanceof org.springframework.web.ErrorResponse error) {
			String detalle = error.getBody().getDetail();
			return responder(error.getStatusCode(), "SOLICITUD_INVALIDA", detalle != null ? detalle : e.getMessage());
		}
		log.error("Error inesperado", e);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrió un error inesperado.");
	}

	private ResponseEntity<ErrorResponse> responder(HttpStatusCode estado, String codigo, String mensaje) {
		return ResponseEntity.status(estado).body(new ErrorResponse(codigo, mensaje));
	}
}
