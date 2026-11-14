package com.zerotrust.votingapi.adapter.web;

import com.zerotrust.votingapi.domain.Catalogo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogoController {

    private final CatalogoResponse respuesta;

    // El catálogo es inmutable desde el arranque: la respuesta se construye una sola vez.
    public CatalogoController(Catalogo catalogo) {
        this.respuesta = CatalogoResponse.de(catalogo);
    }

    @GetMapping("/catalog")
    public CatalogoResponse catalogo() {
        return respuesta;
    }
}
