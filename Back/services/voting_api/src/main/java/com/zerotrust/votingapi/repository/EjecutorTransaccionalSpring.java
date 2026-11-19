package com.zerotrust.votingapi.repository;

import com.zerotrust.votingapi.port.EjecutorTransaccional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

public class EjecutorTransaccionalSpring implements EjecutorTransaccional {

    private final TransactionTemplate plantilla;

    public EjecutorTransaccionalSpring(PlatformTransactionManager gestor) {
        this.plantilla = new TransactionTemplate(gestor);
    }

    // TransactionTemplate revierte ante cualquier RuntimeException o Error.
    @Override
    public <T> T ejecutar(Supplier<T> accion) {
        return plantilla.execute(estado -> accion.get());
    }
}
