package com.zerotrust.votingapi.security;

import com.zerotrust.votingapi.domain.Cedula;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.VoterId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("DerivadorVoterIdHmac")
class DerivadorVoterIdHmacTest {

    private static final byte[] CLAVE = claveDe(0);
    private static final ElectionId ELECCION = new ElectionId("elecciones-2026");

    private static byte[] claveDe(int desplazamiento) {
        byte[] clave = new byte[32];
        for (int i = 0; i < clave.length; i++) {
            clave[i] = (byte) (i + desplazamiento);
        }
        return clave;
    }

    private final DerivadorVoterIdHmac derivador = new DerivadorVoterIdHmac(CLAVE);

    @Test
    @DisplayName("Debe dar el vector calculado de forma independiente (HMAC-SHA-256 con .NET)")
    void vectorConocido() {
        // clave = bytes 0..31; mensaje = "zerotrust-voter-v1" 00 "elecciones-2026" 00 "0012345678"
        // voting_api y voter_registration_service deben dar este mismo valor.
        VoterId voterId = derivador.derivar(new Cedula("0012345678"), ELECCION);

        assertThat(voterId.valor()).isEqualTo("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1");
    }

    @Test
    @DisplayName("La misma cédula en la misma elección debe dar siempre el mismo UUID")
    void determinista() {
        VoterId primero = derivador.derivar(new Cedula("1234567890"), ELECCION);
        VoterId segundo = new DerivadorVoterIdHmac(CLAVE).derivar(new Cedula("1234567890"), ELECCION);

        assertThat(primero).isEqualTo(segundo);
    }

    @Test
    @DisplayName("Dos entradas equivalentes según la normalización deben dar el mismo UUID")
    void equivalentes() {
        VoterId a = derivador.derivar(new Cedula("1.234.567.890"), ELECCION);
        VoterId b = derivador.derivar(new Cedula(" 1234567890 "), ELECCION);

        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("Una elección distinta debe dar un UUID distinto")
    void distintaEleccion() {
        Cedula cedula = new Cedula("1234567890");

        assertThat(derivador.derivar(cedula, ELECCION))
                .isNotEqualTo(derivador.derivar(cedula, new ElectionId("elecciones-2027")));
    }

    @Test
    @DisplayName("Una cédula distinta, o con ceros iniciales distintos, debe dar un UUID distinto")
    void distintaCedula() {
        assertThat(derivador.derivar(new Cedula("1234567890"), ELECCION))
                .isNotEqualTo(derivador.derivar(new Cedula("1234567891"), ELECCION));
        assertThat(derivador.derivar(new Cedula("01234567"), ELECCION))
                .isNotEqualTo(derivador.derivar(new Cedula("1234567"), ELECCION));
    }

    @Test
    @DisplayName("Una clave distinta debe dar un UUID distinto")
    void distintaClave() {
        Cedula cedula = new Cedula("1234567890");

        assertThat(derivador.derivar(cedula, ELECCION))
                .isNotEqualTo(new DerivadorVoterIdHmac(claveDe(1)).derivar(cedula, ELECCION));
    }

    @Test
    @DisplayName("El resultado debe ser un UUID versión 8 con la variante RFC 9562 y no contener la cédula")
    void versionYVariante() {
        for (int i = 0; i < 200; i++) {
            String cedula = String.valueOf(1_000_000 + i);
            UUID uuid = UUID.fromString(derivador.derivar(new Cedula(cedula), ELECCION).valor());

            assertThat(uuid.version()).isEqualTo(8);
            assertThat(uuid.variant()).isEqualTo(2);
            assertThat(uuid.toString().replace("-", "")).doesNotContain(cedula);
        }
    }

    @Test
    @DisplayName("Debe rechazar claves nulas o de menos de 32 bytes")
    void claveCorta() {
        assertThrows(IllegalArgumentException.class, () -> new DerivadorVoterIdHmac(null));
        assertThrows(IllegalArgumentException.class, () -> new DerivadorVoterIdHmac(new byte[31]));
    }

    @Test
    @DisplayName("Debe leer la clave en Base64 y rechazar vacía, mal codificada o corta sin revelarla")
    void claveBase64() {
        String valida = Base64.getEncoder().encodeToString(CLAVE);
        assertThat(DerivadorVoterIdHmac.desdeBase64(valida).derivar(new Cedula("0012345678"), ELECCION).valor())
                .isEqualTo("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1");

        assertThrows(IllegalArgumentException.class, () -> DerivadorVoterIdHmac.desdeBase64(null));
        assertThrows(IllegalArgumentException.class, () -> DerivadorVoterIdHmac.desdeBase64(" "));
        IllegalArgumentException mala = assertThrows(IllegalArgumentException.class,
                () -> DerivadorVoterIdHmac.desdeBase64("no-es-base64!!"));
        String corta = Base64.getEncoder().encodeToString(new byte[8]);
        IllegalArgumentException errorCorta = assertThrows(IllegalArgumentException.class,
                () -> DerivadorVoterIdHmac.desdeBase64(corta));
        assertThat(mala.getMessage()).doesNotContain("no-es-base64");
        assertThat(errorCorta.getMessage()).doesNotContain(corta);
    }
}
