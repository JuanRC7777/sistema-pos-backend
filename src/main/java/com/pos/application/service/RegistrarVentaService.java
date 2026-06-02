package com.pos.application.service;

import com.pos.application.dto.command.ItemVentaCommand;
import com.pos.application.dto.command.RegistrarVentaCommand;
import com.pos.application.dto.response.DetalleVentaResponse;
import com.pos.application.dto.response.VentaResponse;
import com.pos.application.port.in.venta.RegistrarVentaUseCase;
import com.pos.application.port.out.ProductoRepositoryPort;
import com.pos.application.port.out.SecuenciaFacturaRepositoryPort;
import com.pos.application.port.out.VentaRepositoryPort;
import com.pos.domain.exception.ProductoNoEncontradoException;
import com.pos.domain.exception.StockInsuficienteException;
import com.pos.domain.model.DetalleVenta;
import com.pos.domain.model.Producto;
import com.pos.domain.model.Venta;
import com.pos.domain.service.GeneradorNumeroFactura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RegistrarVentaService implements RegistrarVentaUseCase {

    private final ProductoRepositoryPort productoRepo;
    private final VentaRepositoryPort ventaRepo;
    private final SecuenciaFacturaRepositoryPort secuenciaRepo;
    private final GeneradorNumeroFactura generadorNumeroFactura;
    private final BigDecimal tasaImpuesto;

    public RegistrarVentaService(
            ProductoRepositoryPort productoRepo,
            VentaRepositoryPort ventaRepo,
            SecuenciaFacturaRepositoryPort secuenciaRepo,
            GeneradorNumeroFactura generadorNumeroFactura,
            BigDecimal tasaImpuesto) {
        this.productoRepo = productoRepo;
        this.ventaRepo = ventaRepo;
        this.secuenciaRepo = secuenciaRepo;
        this.generadorNumeroFactura = generadorNumeroFactura;
        this.tasaImpuesto = tasaImpuesto;
    }

    @Override
    public VentaResponse registrar(RegistrarVentaCommand cmd) {
        validarMetodoPago(cmd.getMetodoPago());
        validarDatosCliente(cmd);

        List<DetalleVenta> detalles = construirDetalles(cmd);

        String ventaId = UUID.randomUUID().toString();
        LocalDate hoy = LocalDate.now(ZoneOffset.UTC);

        Venta venta = new Venta();
        venta.setId(ventaId);
        venta.setNombreCajero(cmd.getUsernameCajero());
        venta.setNombreCliente(cmd.getNombreCliente());
        venta.setCedulaCliente(cmd.getCedulaCliente());
        venta.setMetodoPago(cmd.getMetodoPago());
        venta.setTasaImpuesto(tasaImpuesto);
        venta.setDetalles(detalles);
        venta.setFecha(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        venta.calcularTotales();

        // Validar y calcular pago en efectivo
        if ("EFECTIVO".equals(cmd.getMetodoPago())) {
            validarPagoEfectivo(cmd.getMontoPagado(), venta.getTotal());
            venta.setMontoPagado(cmd.getMontoPagado());
            venta.calcularCambio();
        }

        int secuencia = secuenciaRepo.obtenerYIncrementarSecuencia(hoy);
        venta.setNumeroFactura(generadorNumeroFactura.generar(hoy, secuencia));

        for (ItemVentaCommand item : cmd.getItems()) {
            Producto producto = productoRepo.findByCodigo(item.getCodigoProducto()).get();
            productoRepo.decrementarStock(producto.getId(), item.getCantidad());
        }

        return ventaRepo.save(venta, detalles);
    }

    private List<DetalleVenta> construirDetalles(RegistrarVentaCommand cmd) {
        List<DetalleVenta> detalles = new ArrayList<>();

        for (ItemVentaCommand item : cmd.getItems()) {
            if (item.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
            }

            Producto producto = productoRepo.findByCodigo(item.getCodigoProducto())
                .orElseThrow(() -> new ProductoNoEncontradoException(item.getCodigoProducto()));

            if (!producto.tieneStockSuficiente(item.getCantidad())) {
                throw new StockInsuficienteException(
                    producto.getNombre(), item.getCantidad(), producto.getStock());
            }

            DetalleVenta detalle = new DetalleVenta();
            detalle.setProductoId(producto.getId());
            detalle.setCodigoProducto(producto.getCodigo());
            detalle.setNombreProducto(producto.getNombre());
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.calcularSubtotal();
            detalles.add(detalle);
        }

        return detalles;
    }

    private void validarMetodoPago(String metodoPago) {
        if (metodoPago == null ||
            (!metodoPago.equals("EFECTIVO") &&
             !metodoPago.equals("TARJETA") &&
             !metodoPago.equals("TRANSFERENCIA"))) {
            throw new IllegalArgumentException(
                "Método de pago inválido. Use: EFECTIVO, TARJETA o TRANSFERENCIA");
        }
    }

    private void validarPagoEfectivo(BigDecimal montoPagado, BigDecimal total) {
        if (montoPagado == null) {
            throw new IllegalArgumentException(
                "El campo montoPagado es obligatorio cuando el método de pago es EFECTIVO");
        }
        if (montoPagado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "El monto pagado debe ser mayor a cero");
        }
        if (montoPagado.compareTo(total) < 0) {
            throw new IllegalArgumentException(
                "El monto pagado (" + montoPagado + ") es insuficiente. Total a pagar: " + total);
        }
    }

    private void validarDatosCliente(RegistrarVentaCommand cmd) {
        if (cmd.getCedulaCliente() != null && !cmd.getCedulaCliente().matches("^\\d{10}$")) {
            throw new IllegalArgumentException("La cédula debe tener exactamente 10 dígitos");
        }
        if (cmd.getNombreCliente() != null && cmd.getNombreCliente().length() > 100) {
            throw new IllegalArgumentException(
                "El nombre del cliente no puede superar 100 caracteres");
        }
    }
}
