package com.zerotrust.votingapi.controller;

import com.zerotrust.votingapi.config.RepositorioCatalogo;
import com.zerotrust.votingapi.dto.response.CatalogoResponse;
import com.zerotrust.votingapi.mapper.MapperCatalogo;
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
        return MapperCatalogo.aResponse(repositorio.actual());
    }
}
