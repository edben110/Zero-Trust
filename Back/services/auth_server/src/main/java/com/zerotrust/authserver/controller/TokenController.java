package com.zerotrust.authserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.authserver.domain.ClientId;
import com.zerotrust.authserver.domain.TokenEmitido;
import com.zerotrust.authserver.dto.response.RespuestaToken;
import com.zerotrust.authserver.port.RegistroEventos;
import com.zerotrust.authserver.service.PeticionToken;
import com.zerotrust.authserver.service.ServicioEmisionToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
public class TokenController {

    private final InterpretePeticionToken interprete;
    private final ServicioEmisionToken servicio;
    private final RegistroEventos eventos;
    private final ObjectMapper json;

    public TokenController(InterpretePeticionToken interprete, ServicioEmisionToken servicio, RegistroEventos eventos,
                           ObjectMapper json) {
        this.interprete = interprete;
        this.servicio = servicio;
        this.eventos = eventos;
        this.json = json;
    }

    // Sin "consumes": un Content-Type incorrecto debe ser invalid_request (400), no el 415 de Spring.
    @PostMapping("/token")
    public ResponseEntity<byte[]> token(HttpServletRequest request) throws IOException {
        // Se lee el cuerpo crudo: getParameter mezclaría la query string, que se ignora (RF-29).
        byte[] cuerpo = request.getInputStream().readAllBytes();
        List<String> autorizaciones = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        PeticionToken peticion = interprete.interpretar(request.getContentType(), autorizaciones, cuerpo);
        TokenEmitido token = servicio.emitir(peticion);
        eventos.emisionCorrecta(new ClientId(peticion.clientIdCrudo()));
        RespuestaToken respuesta = new RespuestaToken(token.accessToken(), "Bearer", token.expiresIn());
        return RespuestaJson.sinCache(HttpStatus.OK).body(RespuestaJson.serializar(json, respuesta));
    }
}
