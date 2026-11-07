package org.cuatrovientos.dam.psp.Tamagochi_Proyecto;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record Configuracion(
        int numeroTamagotchis,
        int tiempoVidaSegundos,
        int intervaloSuciedadSegundos,
        int tiempoLimpiezaSegundos,
        int comerMinimoSegundos,
        int comerMaximoSegundos) {

    public static Configuracion cargar() throws IOException {
        Properties propiedades = new Properties();

        try (InputStream entrada = Configuracion.class
                .getResourceAsStream("/configuracion.properties")) {
            if (entrada == null) {
                throw new IOException(
                        "No se encuentra configuracion.properties en src/main/resources.");
            }
            propiedades.load(entrada);
        }

        int cantidad = leerEntero(propiedades, "tamagotchis");
        int vida = leerEntero(propiedades, "tiempo.vida");
        int intervalo = leerEntero(propiedades, "intervalo.suciedad");
        int limpieza = leerEntero(propiedades, "tiempo.limpieza");
        int comerMin = leerEntero(propiedades, "comer.minimo");
        int comerMax = leerEntero(propiedades, "comer.maximo");

        validarPositivo("tamagotchis", cantidad);
        validarPositivo("tiempo.vida", vida);
        validarPositivo("intervalo.suciedad", intervalo);
        validarPositivo("tiempo.limpieza", limpieza);
        validarPositivo("comer.minimo", comerMin);
        validarPositivo("comer.maximo", comerMax);

        if (comerMax < comerMin) {
            throw new IllegalArgumentException(
                    "comer.maximo no puede ser menor que comer.minimo.");
        }

        return new Configuracion(
                cantidad, vida, intervalo, limpieza, comerMin, comerMax);
    }

    private static int leerEntero(Properties propiedades, String clave) {
        String valor = propiedades.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta la propiedad: " + clave);
        }

        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "La propiedad " + clave + " debe ser un número entero.");
        }
    }

    private static void validarPositivo(String clave, int valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException(
                    "La propiedad " + clave + " debe ser mayor que cero.");
        }
    }
}
