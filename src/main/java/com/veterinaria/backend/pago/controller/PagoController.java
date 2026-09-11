package com.veterinaria.backend.pago.controller;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import com.veterinaria.backend.pago.dto.*;
import com.veterinaria.backend.pago.service.PagoService;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import com.veterinaria.backend.usuario.enums.NombreRol;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class PagoController {
    private final PagoService service;
    public PagoController(PagoService service) { this.service = service; }
    @PostMapping("/citas/{citaId}/pagos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
    public PagoResponse registrar(@PathVariable Long citaId, @Valid @RequestBody RegistrarPagoRequest request, JwtAuthenticationToken auth) { return service.registrar(citaId, uid(auth), roles(auth), request); }
    @GetMapping("/citas/{citaId}/pagos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
    public PaginaResponse<PagoResponse> listar(@PathVariable Long citaId, @RequestParam(required=false) Integer page, @RequestParam(required=false) Integer size, JwtAuthenticationToken auth) { return service.listar(citaId, page, size, roles(auth)); }
    @GetMapping("/pagos/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
    public PagoResponse obtener(@PathVariable Long id, JwtAuthenticationToken auth) { return service.obtener(id, roles(auth)); }
    private Long uid(JwtAuthenticationToken a) { Object v=a.getToken().getClaim("uid"); return v instanceof Number n?n.longValue():Long.valueOf(v.toString()); }
    private Set<NombreRol> roles(JwtAuthenticationToken a) { Set<NombreRol> r=new LinkedHashSet<>(); for(GrantedAuthority x:a.getAuthorities()) if(x.getAuthority().startsWith("ROLE_")) try{r.add(NombreRol.valueOf(x.getAuthority().substring(5)));}catch(IllegalArgumentException ignored){} return Set.copyOf(r); }
}
