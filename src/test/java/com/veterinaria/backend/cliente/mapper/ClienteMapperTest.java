package com.veterinaria.backend.cliente.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.veterinaria.backend.cliente.entity.Cliente;
import org.junit.jupiter.api.Test;

class ClienteMapperTest {

    private final ClienteMapper mapper = new ClienteMapper();

    @Test
    void shouldBuildFullNameWithoutExtraSpaces() {
        Cliente cliente = new Cliente();
        cliente.setPrimerNombre("  Maria ");
        cliente.setSegundoNombre(" Elena ");
        cliente.setPrimerApellido(" Torres ");
        cliente.setSegundoApellido(" Lopez ");

        assertThat(mapper.nombreCompleto(cliente)).isEqualTo("Maria Elena Torres Lopez");
    }
}
