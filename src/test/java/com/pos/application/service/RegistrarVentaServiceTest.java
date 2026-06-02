package com.pos.application.service;

import com.pos.application.dto.command.ItemVentaCommand;
import com.pos.application.dto.command.RegistrarVentaCommand;
import com.pos.application.dto.response.VentaResponse;
import com.pos.application.port.out.ProductoRepositoryPort;
import com.pos.application.port.out.SecuenciaFacturaRepositoryPort;
import com.pos.application.port.out.VentaRepositoryPort;
import com.pos.domain.exception.ProductoNoEncontradoException;
import com.pos.domain.exception.StockInsuficienteException;
import com.pos.domain.model.Producto;
import com.pos.domain.service.GeneradorNumeroFactura;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrarVentaServiceTest {

    private ProductoRepositoryPort productoRepo;
    private VentaRepositoryPort ventaRepo;
    private SecuenciaFacturaRepositoryPort secuenciaRepo;
    private RegistrarVentaService service;

    @BeforeEach
    void setUp() {
        productoRepo = mock(ProductoRepositoryPort.class);
        ventaRepo = mock(VentaRepositoryPort.class);
        secuenciaRepo = mock(SecuenciaFacturaRepositoryPort.class);

        service = new RegistrarVentaService(
            productoRepo, ventaRepo, secuenciaRepo,
            new GeneradorNumeroFactura(),
            new BigDecimal("0.05")
        );
    }

    @Test
    void registrar_ventaValida_retornaVentaResponse() {
        Producto producto = productoConStock("BEB-001", "Bebida Cola", "1.50", 10);
        when(productoRepo.findByCodigo("BEB-001")).thenReturn(Optional.of(producto));
        when(secuenciaRepo.obtenerYIncrementarSecuencia(any(LocalDate.class))).thenReturn(1);

        VentaResponse expected = new VentaResponse();
        expected.setNumeroFactura("FAC-20260523-000001");
        when(ventaRepo.save(any(), any())).thenReturn(expected);

        RegistrarVentaCommand cmd = comandoCon("cajero1", "BEB-001", 2, "EFECTIVO");
        cmd.setMontoPagado(new BigDecimal("10.00"));
        VentaResponse resultado = service.registrar(cmd);

        assertThat(resultado).isNotNull();
        verify(productoRepo).decrementarStock(eq("prod-1"), eq(2));
        verify(ventaRepo).save(any(), any());
    }

    @Test
    void registrar_productoNoExiste_lanzaExcepcion() {
        when(productoRepo.findByCodigo("XXX-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(comandoCon("cajero1", "XXX-999", 1, "EFECTIVO")))
            .isInstanceOf(ProductoNoEncontradoException.class)
            .hasMessageContaining("XXX-999");
    }

    @Test
    void registrar_stockInsuficiente_lanzaExcepcion() {
        Producto producto = productoConStock("BEB-001", "Bebida Cola", "1.50", 1);
        when(productoRepo.findByCodigo("BEB-001")).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.registrar(comandoCon("cajero1", "BEB-001", 5, "EFECTIVO")))
            .isInstanceOf(StockInsuficienteException.class)
            .hasMessageContaining("Bebida Cola");
    }

    @Test
    void registrar_metodoPagoInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> service.registrar(comandoCon("cajero1", "BEB-001", 1, "BITCOIN")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Método de pago inválido");
    }

    @Test
    void registrar_cedulaConFormatoInvalido_lanzaExcepcion() {
        RegistrarVentaCommand cmd = comandoCon("cajero1", "BEB-001", 1, "EFECTIVO");
        cmd.setCedulaCliente("123");  // solo 3 dígitos

        assertThatThrownBy(() -> service.registrar(cmd))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cédula");
    }

    @Test
    void registrar_clienteNulo_noLanzaError() {
        Producto producto = productoConStock("BEB-001", "Bebida Cola", "1.50", 10);
        when(productoRepo.findByCodigo("BEB-001")).thenReturn(Optional.of(producto));
        when(secuenciaRepo.obtenerYIncrementarSecuencia(any(LocalDate.class))).thenReturn(1);
        when(ventaRepo.save(any(), any())).thenReturn(new VentaResponse());

        RegistrarVentaCommand cmd = comandoCon("cajero1", "BEB-001", 1, "TARJETA");
        cmd.setNombreCliente(null);
        cmd.setCedulaCliente(null);

        assertThatNoException().isThrownBy(() -> service.registrar(cmd));
    }

    // Helpers
    private Producto productoConStock(String codigo, String nombre, String precio, int stock) {
        Producto p = new Producto();
        p.setId("prod-1");
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        p.setActivo(true);
        return p;
    }

    private RegistrarVentaCommand comandoCon(String cajero, String codigo, int cantidad, String metodoPago) {
        ItemVentaCommand item = new ItemVentaCommand();
        item.setCodigoProducto(codigo);
        item.setCantidad(cantidad);

        RegistrarVentaCommand cmd = new RegistrarVentaCommand();
        cmd.setUsernameCajero(cajero);
        cmd.setItems(List.of(item));
        cmd.setMetodoPago(metodoPago);
        return cmd;
    }
}
