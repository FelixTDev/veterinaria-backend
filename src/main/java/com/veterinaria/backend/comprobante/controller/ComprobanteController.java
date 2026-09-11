package com.veterinaria.backend.comprobante.controller;
import java.util.LinkedHashSet;import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.GrantedAuthority;import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;import org.springframework.web.bind.annotation.*;import jakarta.validation.Valid;
import com.veterinaria.backend.comprobante.dto.*;import com.veterinaria.backend.comprobante.service.ComprobanteService;import com.veterinaria.backend.usuario.enums.NombreRol;
@RestController @RequestMapping("/api/v1") public class ComprobanteController{
 private final ComprobanteService service; public ComprobanteController(ComprobanteService s){service=s;}
 @PostMapping("/pagos/{pagoId}/comprobante") @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')") public ComprobanteResponse emitir(@PathVariable Long pagoId,@Valid @RequestBody EmitirComprobanteRequest r,JwtAuthenticationToken a){return service.emitir(pagoId,roles(a),r);}
 @GetMapping("/pagos/{pagoId}/comprobante") @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')") public ComprobanteResponse porPago(@PathVariable Long pagoId,JwtAuthenticationToken a){return service.porPago(pagoId,roles(a));}
 @GetMapping("/comprobantes/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')") public ComprobanteResponse obtener(@PathVariable Long id,JwtAuthenticationToken a){return service.obtener(id,roles(a));}
 private Set<NombreRol> roles(JwtAuthenticationToken a){Set<NombreRol> r=new LinkedHashSet<>();for(GrantedAuthority x:a.getAuthorities())if(x.getAuthority().startsWith("ROLE_"))try{r.add(NombreRol.valueOf(x.getAuthority().substring(5)));}catch(IllegalArgumentException ignored){}return Set.copyOf(r);}
}
