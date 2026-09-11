package com.veterinaria.backend.comprobante.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.veterinaria.backend.comprobante.dto.*;
import com.veterinaria.backend.comprobante.entity.Comprobante;
import com.veterinaria.backend.comprobante.enums.*;
import com.veterinaria.backend.comprobante.repository.ComprobanteRepository;
import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.pago.entity.Pago;
import com.veterinaria.backend.pago.enums.EstadoPago;
import com.veterinaria.backend.pago.repository.PagoRepository;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.pago.exception.PagoNoEncontradoException;
import com.veterinaria.backend.comprobante.exception.ComprobanteConflictException;

@ExtendWith(MockitoExtension.class)
class ComprobanteServiceTest {
 @Mock PagoRepository pagos; @Mock ComprobanteRepository comprobantes; @InjectMocks ComprobanteService service;
 @Test void emitsOnlyForPaidAndForcesEmittedState(){Pago p=paid();when(pagos.findWithCitaAndRegistradoPorById(1L)).thenReturn(Optional.of(p));when(comprobantes.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));service.emitir(1L,Set.of(NombreRol.RECEPCIONISTA),new EmitirComprobanteRequest(TipoComprobante.BOLETA," B001 "," 7 ",null,null,null));ArgumentCaptor<Comprobante> c=ArgumentCaptor.forClass(Comprobante.class);verify(comprobantes).saveAndFlush(c.capture());org.assertj.core.api.Assertions.assertThat(c.getValue().getEstado()).isEqualTo(EstadoComprobante.EMITIDO);org.assertj.core.api.Assertions.assertThat(c.getValue().getSerie()).isEqualTo("B001");}
 @Test void rejectsUnpaid(){Pago p=paid();p.setEstado(EstadoPago.PENDIENTE);when(pagos.findWithCitaAndRegistradoPorById(1L)).thenReturn(Optional.of(p));assertThatThrownBy(()->service.emitir(1L,Set.of(NombreRol.ADMINISTRADOR),new EmitirComprobanteRequest(TipoComprobante.BOLETA,"B","1",null,null,null))).isInstanceOf(ComprobanteConflictException.class);}
 @Test void validatesInvoiceFiscalFields(){when(pagos.findWithCitaAndRegistradoPorById(1L)).thenReturn(Optional.of(paid()));assertThatThrownBy(()->service.emitir(1L,Set.of(NombreRol.ADMINISTRADOR),new EmitirComprobanteRequest(TipoComprobante.FACTURA,"F","1","123",null,""))).isInstanceOf(IllegalArgumentException.class);}
 private Pago paid(){Pago p=new Pago();p.setId(1L);p.setEstado(EstadoPago.PAGADO);p.setMontoTotal(new BigDecimal("10"));Cita c=new Cita();c.setId(2L);p.setCita(c);return p;}
}
