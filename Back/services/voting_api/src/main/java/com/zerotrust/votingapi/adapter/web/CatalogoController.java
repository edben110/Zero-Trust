package com.zerotrust.votingapi.adapter.web;

import com.zerotrust.votingapi.adapter.config.RepositorioCatalogo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogoController {

    private final RepositorioCatalogo repositorio;

    public CatalogoController(RepositorioCatalogo repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping("/catalog")
    public CatalogoResponse catalogo() {
        return CatalogoResponse.de(repositorio.actual());
    }
}
