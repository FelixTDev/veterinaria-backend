package com.veterinaria.backend.pago.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import com.veterinaria.backend.cita.entity.*;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.repository.*;
import com.veterinaria.backend.pago.dto.*;
import com.veterinaria.backend.pago.entity.Pago;
import com.veterinaria.backend.pago.repository.*;
import com.veterinaria.backend.pago.enums.MedioPago;
import com.veterinaria.backend.pago.exception.PagoConflictException;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {
    @Mock CitaRepository citaRepository; @Mock CitaServicioRepository citaServicioRepository; @Mock PagoRepository pagoRepository; @Mock DetallePagoRepository detalleRepository; @Mock UsuarioRepository usuarioRepository; @InjectMocks PagoService service;

    @Test void calculatesTotalFromDetailsAndUsesAppliedPriceSnapshot(){
        Cita cita=cita(1L, EstadoCita.ATENDIDA); Usuario u=usuario(9L); when(usuarioRepository.findById(9L)).thenReturn(Optional.of(u)); when(citaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(cita)); when(citaServicioRepository.sumPrecioAplicadoByCitaId(1L)).thenReturn(new BigDecimal("100.00")); when(pagoRepository.sumPagadoByCitaId(1L)).thenReturn(new BigDecimal("20.00")); when(pagoRepository.saveAndFlush(any(Pago.class))).thenAnswer(i->i.getArgument(0)); when(detalleRepository.saveAllAndFlush(any())).thenAnswer(i->i.getArgument(0));
        PagoResponse response=service.registrar(1L,9L,Set.of(NombreRol.RECEPCIONISTA),new RegistrarPagoRequest(List.of(new DetallePagoRequest(MedioPago.EFECTIVO,new BigDecimal("30.00"),""),new DetallePagoRequest(MedioPago.YAPE,new BigDecimal("50.00")," y ")),""));
        assertThat(response.montoTotal()).isEqualByComparingTo("80.00"); assertThat(response.detalles()).extracting(DetallePagoResponse::referencia).containsExactly(null,"y"); verify(citaServicioRepository).sumPrecioAplicadoByCitaId(1L);
    }
    @Test void rejectsPaymentWhenAppointmentIsNotAttended(){
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario(9L))); when(citaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(cita(1L,EstadoCita.CONFIRMADA)));
        assertThatThrownBy(()->service.registrar(1L,9L,Set.of(NombreRol.ADMINISTRADOR),new RegistrarPagoRequest(List.of(new DetallePagoRequest(MedioPago.EFECTIVO,BigDecimal.ONE,null)),null))).isInstanceOf(PagoConflictException.class); verifyNoInteractions(citaServicioRepository,pagoRepository);
    }
    @Test void rejectsPaymentAboveBalance(){
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario(9L))); when(citaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(cita(1L,EstadoCita.ATENDIDA))); when(citaServicioRepository.sumPrecioAplicadoByCitaId(1L)).thenReturn(new BigDecimal("10")); when(pagoRepository.sumPagadoByCitaId(1L)).thenReturn(BigDecimal.ZERO);
        assertThatThrownBy(()->service.registrar(1L,9L,Set.of(NombreRol.RECEPCIONISTA),new RegistrarPagoRequest(List.of(new DetallePagoRequest(MedioPago.TARJETA,new BigDecimal("11"),null)),null))).isInstanceOf(PagoConflictException.class); verify(pagoRepository,never()).saveAndFlush(any());
    }
    @Test void rejectsVeterinarian(){assertThatThrownBy(()->service.registrar(1L,9L,Set.of(NombreRol.VETERINARIO),null)).isInstanceOf(AccessDeniedException.class); verifyNoInteractions(citaRepository);}
    private Cita cita(Long id,EstadoCita e){Cita c=new Cita();c.setId(id);c.setEstado(e);return c;} private Usuario usuario(Long id){Usuario u=new Usuario();u.setId(id);u.setActivo(true);return u;}
}
