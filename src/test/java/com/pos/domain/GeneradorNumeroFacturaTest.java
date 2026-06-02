package com.pos.domain;

import com.pos.domain.service.GeneradorNumeroFactura;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorNumeroFacturaTest {

    private GeneradorNumeroFactura generador;

    @BeforeEach
    void setUp() {
        generador = new GeneradorNumeroFactura();
    }

    @Test
    void generar_secuenciaUno_retornaFormatoCorrecto() {
        String resultado = generador.generar(LocalDate.of(2026, 5, 23), 1);
        assertThat(resultado).isEqualTo("FAC-20260523-000001");
    }

    @Test
    void generar_secuenciaMaxima_retormaFormatoCorrecto() {
        String resultado = generador.generar(LocalDate.of(2026, 5, 23), 999999);
        assertThat(resultado).isEqualTo("FAC-20260523-999999");
    }

    @Test
    void validarFormato_formatoCorrecto_retornaTrue() {
        assertThat(generador.validarFormato("FAC-20260523-000007")).isTrue();
    }

    @Test
    void validarFormato_nulo_retornaFalse() {
        assertThat(generador.validarFormato(null)).isFalse();
    }

    @Test
    void validarFormato_sinGuiones_retornaFalse() {
        assertThat(generador.validarFormato("FAC20260523000007")).isFalse();
    }

    @Test
    void validarFormato_letrasEnSecuencia_retornaFalse() {
        assertThat(generador.validarFormato("FAC-20260523-ABCDEF")).isFalse();
    }
}
