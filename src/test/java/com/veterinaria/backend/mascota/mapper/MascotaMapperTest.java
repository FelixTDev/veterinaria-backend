package com.veterinaria.backend.mascota.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import org.junit.jupiter.api.Test;

class MascotaMapperTest {

    private final MascotaMapper mapper = new MascotaMapper();

    @Test
    void shouldMapPersistedApproximateAgeWithoutCalculatingIt() {
        Cliente cliente = new Cliente();
        cliente.setId(8L);
        cliente.setPrimerNombre("Maria");
        cliente.setPrimerApellido("Torres");

        Mascota mascota = new Mascota();
        mascota.setId(9L);
        mascota.setCliente(cliente);
        mascota.setNombre("Luna");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setEdadAproximadaAnios(4);

        assertThat(mapper.toDetalle(mascota).edadAproximadaAnios()).isEqualTo(4);
    }
}
