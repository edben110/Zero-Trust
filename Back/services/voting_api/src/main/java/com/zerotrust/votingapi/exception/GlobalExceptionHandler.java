package com.zerotrust.votingapi.exception;

import com.zerotrust.votingapi.domain.OperacionSeguridadNoDisponibleException;
import com.zerotrust.votingapi.domain.ServicioNoDisponibleException;
import com.zerotrust.votingapi.domain.SolicitudSeguridadInvalidaException;
import com.zerotrust.votingapi.domain.SolicitudVotoInvalidaException;
import com.zerotrust.votingapi.domain.VotanteNoHabilitadoException;
import com.zerotrust.votingapi.domain.VotanteFueraDelPadronException;
import com.zerotrust.votingapi.domain.VotanteYaInscritoException;
import com.zerotrust.votingapi.domain.VotoDuplicadoException;
import com.zerotrust.votingapi.domain.RegionIncorrectaException;
import com.zerotrust.votingapi.domain.VotoAnuladoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SolicitudVotoInvalidaException.class)
    public ResponseEntity<ErrorResponse> solicitudInvalida(SolicitudVotoInvalidaException error) {
        return ResponseEntity.badRequest().body(new ErrorResponse("invalid_request", error.campos()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ErrorResponse> cuerpoIlegible() {
        return ResponseEntity.badRequest().body(new ErrorResponse("invalid_request", List.of()));
    }

    @ExceptionHandler(VotanteNoHabilitadoException.class)
    public ResponseEntity<ErrorResponse> noHabilitado() {
        LOG.warn("Voto rechazado: votante no habilitado");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.de("voter_not_eligible"));
    }

    // POST /voter-id: se avisa antes de votar y no se entrega el código.
    @ExceptionHandler(VotanteFueraDelPadronException.class)
    public ResponseEntity<ErrorResponse> fueraDelPadron() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.con("voter_not_registered",
                "El titular de este documento no se encuentra habilitado en el padrón. "
                        + "Por favor pídale al administrador que lo añada."));
    }

    @ExceptionHandler(VotanteYaInscritoException.class)
    public ResponseEntity<ErrorResponse> yaInscrito() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.con("already_registered",
                "Este documento ya está inscrito en el padrón de esta elección."));
    }

    @ExceptionHandler(VotoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> votoDuplicado() {
        LOG.warn("Voto rechazado: el votante ya votó en esta elección");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.con("already_voted",
                "Ya se registró un voto para este documento en esta elección"));
    }

    @ExceptionHandler(RegionIncorrectaException.class)
    public ResponseEntity<ErrorResponse> regionIncorrecta() {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.con("wrong_region", "error esa no es la region que ud designo"));
    }

    @ExceptionHandler(VotoAnuladoException.class)
    public ResponseEntity<ErrorResponse> votoAnulado() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.con("vote_invalidated",
                        "El voto quedó anulado: seleccionó dos veces una región distinta de la registrada."));
    }

    // Base de datos caída o transacción fallida: no se registró nada y la persona puede reintentar.
    // Se registra solo la clase de la excepción: el mensaje de JDBC puede incluir parámetros.
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<ErrorResponse> baseDeDatosNoDisponible(Exception error) {
        LOG.warn("Voto no procesado: base de datos no disponible ({})", error.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse.de("validation_unavailable"));
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> noDisponible(ServicioNoDisponibleException error) {
        LOG.warn("Voto no procesado: {}", error.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse.de("validation_unavailable"));
    }

    // Panel de seguridad: el BFF muestra "error" al administrador, por eso viaja el mensaje y no un código.
    @ExceptionHandler(SolicitudSeguridadInvalidaException.class)
    public ResponseEntity<ErrorResponse> solicitudSeguridadInvalida(SolicitudSeguridadInvalidaException error) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(error.getMessage()));
    }

    @ExceptionHandler(OperacionSeguridadNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> operacionSeguridadNoDisponible(OperacionSeguridadNoDisponibleException error) {
        Throwable causa = error.getCause() == null ? null : error.getCause().getCause();
        LOG.warn("Operación de seguridad no completada: {} ({})", error.getMessage(),
                causa == null ? "sin causa" : causa.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse.de(error.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoSoportado() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ErrorResponse.de("method_not_allowed"));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Void> rutaDesconocida() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> errorInterno(Exception error) {
        LOG.error("Error interno: {}", error.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.de("server_error"));
    }
}
