package com.zerotrust.authserver.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.authserver.controller.RespuestaJson;
import com.zerotrust.authserver.domain.ClientId;
import com.zerotrust.authserver.domain.CodigoErrorOAuth;
import com.zerotrust.authserver.domain.RegistroClientes;
import com.zerotrust.authserver.port.RegistroEventos;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String RUTA_TOKEN = "/token";
    private static final String SERVER_ERROR = "server_error";
    private static final Map<CodigoErrorOAuth, HttpStatus> ESTADOS = Map.of(
            CodigoErrorOAuth.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
            CodigoErrorOAuth.INVALID_CLIENT, HttpStatus.UNAUTHORIZED,
            CodigoErrorOAuth.INVALID_SCOPE, HttpStatus.BAD_REQUEST,
            CodigoErrorOAuth.UNSUPPORTED_GRANT_TYPE, HttpStatus.BAD_REQUEST);

    private final RegistroClientes registro;
    private final RegistroEventos eventos;
    private final ObjectMapper json;

    public GlobalExceptionHandler(RegistroClientes registro, RegistroEventos eventos, ObjectMapper json) {
        this.registro = registro;
        this.eventos = eventos;
        this.json = json;
    }

    @ExceptionHandler(ErrorOAuthException.class)
    public ResponseEntity<byte[]> errorOAuth(ErrorOAuthException error, HttpServletRequest request) {
        registrarRechazo(request, error.codigo().texto(), error.clientIdCrudo());
        ResponseEntity.BodyBuilder respuesta = RespuestaJson.sinCache(ESTADOS.get(error.codigo()));
        if (error instanceof ClienteInvalidoException invalido && invalido.porBasic()) {
            respuesta.header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"auth_server\"");
        }
        return respuesta.body(cuerpo(error.codigo().texto()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<byte[]> metodoNoSoportado(HttpServletRequest request) {
        registrarRechazo(request, CodigoErrorOAuth.INVALID_REQUEST.texto(), Optional.empty());
        ResponseEntity.BodyBuilder respuesta = RespuestaJson.sinCache(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.ALLOW, esToken(request) ? "POST" : "GET");
        // Una respuesta a HEAD nunca lleva cuerpo.
        if (HttpMethod.HEAD.matches(request.getMethod())) {
            return respuesta.build();
        }
        return respuesta.body(cuerpo(CodigoErrorOAuth.INVALID_REQUEST.texto()));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Void> rutaDesconocida() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<byte[]> errorInterno(Exception error, HttpServletRequest request) {
        if (esToken(request)) {
            registrarRechazo(request, SERVER_ERROR, Optional.empty());
        } else {
            LOG.error("Error interno en {}: {}", request.getRequestURI(), error.getClass().getSimpleName());
        }
        return RespuestaJson.sinCache(HttpStatus.INTERNAL_SERVER_ERROR).body(cuerpo(SERVER_ERROR));
    }

    private void registrarRechazo(HttpServletRequest request, String codigo, Optional<String> clientIdCrudo) {
        if (!esToken(request)) {
            return;
        }
        Optional<ClientId> registrado = clientIdCrudo.flatMap(ClientId::intentar)
                .filter(id -> registro.buscar(id).isPresent());
        registrado.ifPresentOrElse(
                id -> eventos.rechazoClienteRegistrado(codigo, id),
                () -> eventos.rechazoClienteDesconocido(codigo));
    }

    private static boolean esToken(HttpServletRequest request) {
        return RUTA_TOKEN.equals(request.getRequestURI());
    }

    private byte[] cuerpo(String codigo) {
        return RespuestaJson.serializar(json, new ErrorResponse(codigo));
    }
}
