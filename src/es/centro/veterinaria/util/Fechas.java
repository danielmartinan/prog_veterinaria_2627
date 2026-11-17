package es.centro.veterinaria.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Utilidades para mostrar fechas (dd/MM/yyyy) y fechas con hora (dd/MM/yyyy HH:mm). */
public class Fechas {
    public static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Fechas() {
        // Clase de utilidades: no se crean objetos
    }

    /** Devuelve la fecha con formato dd/MM/yyyy, o "-" si es null. */
    public static String formatear(LocalDate fecha) {
        return (fecha == null) ? "-" : fecha.format(FORMATO);
    }

    /** Devuelve la fecha y la hora con formato dd/MM/yyyy HH:mm, o "-" si es null. */
    public static String formatear(LocalDateTime fechaHora) {
        return (fechaHora == null) ? "-" : fechaHora.format(FORMATO_FECHA_HORA);
    }
}
