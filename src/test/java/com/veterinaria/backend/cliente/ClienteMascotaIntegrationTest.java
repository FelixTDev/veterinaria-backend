package com.veterinaria.backend.cliente;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.veterinaria.backend.auth.security.JwtTokenService;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.repository.ClienteRepository;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.support.PostgreSqlContainerConfiguration;
import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.repository.RolRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClienteMascotaIntegrationTest extends PostgreSqlContainerConfiguration {

    @Autowired private MockMvc mockMvc;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UsuarioRolRepository usuarioRolRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;

    @BeforeEach
    void cleanUp() {
        mascotaRepository.deleteAll();
        clienteRepository.deleteAll();
        usuarioRolRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/clientes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/mascotas")).andExpect(status().isUnauthorized());
    }

    @Test
    void receptionistCanCreateClientAndResponseUsesRealFields() throws Exception {
        String token = tokenFor(createUser("recep@test.dev", "RECEPCIONISTA"));

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson(" Maria ", "Torres", "maria@TEST.dev", "111")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("maria@test.dev"))
                .andExpect(jsonPath("$.nombreCompleto").value("Maria Torres"))
                .andExpect(jsonPath("$.direccion").doesNotExist());
    }

    @Test
    void veterinarianCannotManageClients() throws Exception {
        String token = tokenFor(createUser("vet@test.dev", "VETERINARIO"));

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson("Maria", "Torres", "maria@test.dev", "111")))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateDocumentReturnsConflictAndMascotaRequiresActiveOwner() throws Exception {
        String token = tokenFor(createUser("admin@test.dev", "ADMINISTRADOR"));
        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson("Maria", "Torres", "maria@test.dev", "111")))
                .andExpect(status().isCreated());
        Cliente cliente = clienteRepository.findAll().get(0);

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson("Ana", "Lopez", "ana@test.dev", "111")))
                .andExpect(status().isConflict());

        cliente.setActivo(false);
        clienteRepository.save(cliente);
        mockMvc.perform(post("/api/v1/mascotas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":" + cliente.getId() + ",\"nombre\":\"Luna\",\"especie\":\"PERRO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void administratorCanCreateAndListMascotaWithOwnerAndPagination() throws Exception {
        String token = tokenFor(createUser("admin@test.dev", "ADMINISTRADOR"));
        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson("Maria", "Torres", "maria@test.dev", "111")))
                .andExpect(status().isCreated());
        Cliente cliente = clienteRepository.findAll().get(0);

        mockMvc.perform(post("/api/v1/mascotas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":" + cliente.getId() + ",\"nombre\":\"Luna\",\"especie\":\"PERRO\",\"edadAproximadaAnios\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.propietario").value("Maria Torres"))
                .andExpect(jsonPath("$.edadAproximadaAnios").value(4))
                .andExpect(jsonPath("$.numeroMicrochip").doesNotExist());

        mockMvc.perform(get("/api/v1/mascotas?page=0&size=10&especie=PERRO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void receptionistCanCreateMascotaAndClientListIncludesMascotaCount() throws Exception {
        String token = tokenFor(createUser("recep@test.dev", "RECEPCIONISTA"));
        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson("Maria", "Torres", "maria@test.dev", "222")))
                .andExpect(status().isCreated());
        Cliente cliente = clienteRepository.findAll().get(0);

        mockMvc.perform(post("/api/v1/mascotas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":" + cliente.getId() + ",\"nombre\":\"Nina\",\"especie\":\"GATO\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/clientes?page=0&size=10&search=maria&activo=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cantidadMascotas").value(1));

        mockMvc.perform(get("/api/v1/clientes/" + cliente.getId() + "/mascotas?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Nina"));
    }

    @Test
    void shouldEditWithoutChangingOwnerAndKeepRecordsAfterDeactivation() throws Exception {
        String token = tokenFor(createUser("admin@test.dev", "ADMINISTRADOR"));
        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson("Maria", "Torres", "maria@test.dev", "333")))
                .andExpect(status().isCreated());
        Cliente cliente = clienteRepository.findAll().get(0);
        mockMvc.perform(post("/api/v1/mascotas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":" + cliente.getId() + ",\"nombre\":\"Luna\",\"especie\":\"PERRO\"}"))
                .andExpect(status().isCreated());
        Long mascotaId = mascotaRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/v1/mascotas/" + mascotaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Luna Nueva\",\"especie\":\"PERRO\",\"raza\":\"Labrador\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value(cliente.getId()))
                .andExpect(jsonPath("$.nombre").value("Luna Nueva"));

        mockMvc.perform(patch("/api/v1/clientes/" + cliente.getId() + "/estado")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        org.assertj.core.api.Assertions.assertThat(clienteRepository.existsById(cliente.getId())).isTrue();
        org.assertj.core.api.Assertions.assertThat(mascotaRepository.existsById(mascotaId)).isTrue();
    }

    @Test
    void shouldRejectFutureBirthDateAndNonPositiveWeight() throws Exception {
        String token = tokenFor(createUser("admin@test.dev", "ADMINISTRADOR"));
        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson("Maria", "Torres", "maria@test.dev", "444")))
                .andExpect(status().isCreated());
        Cliente cliente = clienteRepository.findAll().get(0);

        mockMvc.perform(post("/api/v1/mascotas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":" + cliente.getId() + ",\"nombre\":\"Luna\",\"especie\":\"PERRO\",\"fechaNacimiento\":\"2999-01-01\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/mascotas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":" + cliente.getId() + ",\"nombre\":\"Luna\",\"especie\":\"PERRO\",\"pesoKg\":0}"))
                .andExpect(status().isBadRequest());
    }

    private Usuario createUser(String correo, String roleName) {
        Usuario usuario = new Usuario();
        usuario.setPrimerNombre("Test");
        usuario.setPrimerApellido("User");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode("Password1!"));
        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);
        Usuario saved = usuarioRepository.save(usuario);
        Rol rol = rolRepository.findAll().stream().filter(item -> item.getNombre().name().equals(roleName)).findFirst().orElseThrow();
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setUsuario(saved);
        usuarioRol.setRol(rol);
        usuarioRolRepository.save(usuarioRol);
        return saved;
    }

    private String tokenFor(Usuario usuario) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + usuario.getCorreo() + "\",\"password\":\"Password1!\"}"))
                .andExpect(status().isOk()).andReturn();
        Matcher matcher = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"").matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) throw new AssertionError("accessToken missing");
        return matcher.group(1);
    }

    private String clientJson(String name, String surname, String correo, String document) {
        return "{\"primerNombre\":\"" + name + "\",\"primerApellido\":\"" + surname
                + "\",\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"" + document
                + "\",\"correo\":\"" + correo + "\"}";
    }
}
