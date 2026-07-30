package com.veterinaria.backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.enums.TipoDocumento;
import com.veterinaria.backend.cliente.repository.ClienteRepository;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.servicio.entity.Servicio;
import com.veterinaria.backend.servicio.repository.PrecioServicioTamanoRepository;
import com.veterinaria.backend.servicio.repository.ServicioRepository;
import com.veterinaria.backend.support.PostgreSqlContainerConfiguration;
import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.repository.RolRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PersistenceMappingIntegrationTest extends PostgreSqlContainerConfiguration {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioRolRepository usuarioRolRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private PrecioServicioTamanoRepository precioServicioTamanoRepository;

    @Test
    void shouldQueryExistingRoles() {
        assertThat(rolRepository.findAll()).extracting(Rol::getNombre).contains(NombreRol.ADMINISTRADOR);
    }

    @Test
    void shouldPersistClientAndPet() {
        Cliente cliente = new Cliente();
        cliente.setPrimerNombre("Ana");
        cliente.setPrimerApellido("Lopez");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("88889999");
        cliente.setActivo(Boolean.TRUE);

        Cliente savedCliente = clienteRepository.save(cliente);

        Mascota mascota = new Mascota();
        mascota.setCliente(savedCliente);
        mascota.setNombre("Firulais");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setActivo(Boolean.TRUE);

        Mascota savedMascota = mascotaRepository.save(mascota);

        assertThat(savedMascota.getId()).isNotNull();
        assertThat(mascotaRepository.findByClienteId(savedCliente.getId())).hasSize(1);
    }

    @Test
    void shouldCreateUserRoleRelationship() {
        Rol rol = rolRepository.findByNombre(NombreRol.RECEPCIONISTA).orElseThrow();

        Usuario usuario = new Usuario();
        usuario.setPrimerNombre("Luis");
        usuario.setPrimerApellido("Perez");
        usuario.setCorreo("luis.perez@test.dev");
        usuario.setPasswordHash("hash-seguro");
        usuario.setActivo(Boolean.TRUE);
        usuario.setIntentosFallidos(0);

        Usuario savedUsuario = usuarioRepository.save(usuario);

        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setUsuario(savedUsuario);
        usuarioRol.setRol(rol);

        usuarioRolRepository.save(usuarioRol);

        assertThat(usuarioRolRepository.findByUsuario_Id(savedUsuario.getId())).hasSize(1);
    }

    @Test
    void shouldQueryServicesAndPricesBySize() {
        List<Servicio> servicios = servicioRepository.findAll();

        assertThat(servicios).isNotEmpty();
        assertThat(precioServicioTamanoRepository.findByServicioNombre("Baño y corte")).hasSize(3);
    }
}
