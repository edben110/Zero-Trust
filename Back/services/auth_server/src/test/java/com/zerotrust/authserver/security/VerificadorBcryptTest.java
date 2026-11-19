package com.zerotrust.authserver.security;

import com.zerotrust.authserver.domain.HashSecreto;
import com.zerotrust.authserver.domain.SecretoCliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import static org.assertj.core.api.Assertions.assertThat;

class VerificadorBcryptTest {

    private final VerificadorBcrypt verificador = new VerificadorBcrypt(10);

    @Test
    @DisplayName("Debe aceptar el secreto correcto y rechazar uno incorrecto")
    void verificaSecreto() {
        // Arrange
        HashSecreto hash = new HashSecreto(BCrypt.hashpw("secreto", BCrypt.gensalt(10)));

        // Act y Assert
        assertThat(verificador.coincide(new SecretoCliente("secreto"), hash)).isTrue();
        assertThat(verificador.coincide(new SecretoCliente("otro"), hash)).isFalse();
    }

    @Test
    @DisplayName("Debe verificar hashes con prefijo $2y$")
    void verificaPrefijo2y() {
        HashSecreto hash = new HashSecreto(BCrypt.hashpw("secreto", BCrypt.gensalt("$2y", 10)));

        assertThat(verificador.coincide(new SecretoCliente("secreto"), hash)).isTrue();
    }

    @Test
    @DisplayName("El hash ficticio se calcula una vez, con el coste común, y es siempre la misma instancia")
    void hashFicticioUnico() {
        HashSecreto primero = verificador.hashFicticio();

        assertThat(verificador.hashFicticio()).isSameAs(primero);
        assertThat(primero.coste()).isEqualTo(10);
        assertThat(verificador.coincide(new SecretoCliente("cualquiera"), primero)).isFalse();
    }
}
