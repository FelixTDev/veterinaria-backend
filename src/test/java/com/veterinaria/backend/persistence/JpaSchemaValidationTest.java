package com.veterinaria.backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.veterinaria.backend.cliente.repository.ClienteRepository;
import com.veterinaria.backend.servicio.repository.ServicioRepository;
import com.veterinaria.backend.support.PostgreSqlContainerConfiguration;
import com.veterinaria.backend.usuario.repository.RolRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class JpaSchemaValidationTest extends PostgreSqlContainerConfiguration {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Test
    void shouldLoadJpaContextAgainstExistingSchema() {
        assertThat(rolRepository).isNotNull();
        assertThat(clienteRepository).isNotNull();
        assertThat(servicioRepository).isNotNull();
    }
}
