package es.centro.veterinaria.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Scanner;

/**
 * Lectura segura de datos por teclado. Ningún método termina con error: si el dato
 * no es válido, se vuelve a pedir.
 */
public class Consola {
    private static final Scanner SC = new Scanner(System.in);
    private static final String REGEX_FECHA = "\\d{2}/\\d{2}/\\d{4}";
    private static final String REGEX_HORA = "([01]\\d|2[0-3]):[0-5]\\d";

    private Consola() {
        // Clase de utilidades: no se crean objetos
    }

    /** Pide un texto hasta que no esté vacío. Lo devuelve sin espacios al principio ni al final. */
    public static String leerTexto(String mensaje) {
        String texto;
        do {
            System.out.print(mensaje);
            texto = SC.nextLine().trim();
            if (texto.isEmpty()) {
                System.out.println("  El texto no puede estar vacío.");
            }
        } while (texto.isEmpty());
        return texto;
    }

    /** Pide un entero hasta que se escriba un número entre min y max (ambos incluidos). */
    public static int leerEntero(String mensaje, int min, int max) {
        int numero = 0;
        boolean valido = false;
        do {
            System.out.print(mensaje);
            if (SC.hasNextInt()) {
                numero = SC.nextInt();
                valido = numero >= min && numero <= max;
            }
            SC.nextLine();   // descarta el resto de la línea (y el salto de línea)
            if (!valido) {
                System.out.println("  Escribe un número entero entre " + min + " y " + max + ".");
            }
        } while (!valido);
        return numero;
    }

    /** Pide un decimal hasta que se escriba un número entre min y max (ambos incluidos). */
    public static double leerDecimal(String mensaje, double min, double max) {
        double numero = 0;
        boolean valido = false;
        do {
            System.out.print(mensaje);
            if (SC.hasNextDouble()) {
                numero = SC.nextDouble();
                valido = numero >= min && numero <= max;
            }
            SC.nextLine();
            if (!valido) {
                System.out.println("  Escribe un número entre " + min + " y " + max + ".");
            }
        } while (!valido);
        return numero;
    }

    /** Pide una fecha con formato dd/MM/yyyy hasta que sea correcta y exista. */
    public static LocalDate leerFecha(String mensaje) {
        LocalDate fecha;
        do {
            fecha = convertirFecha(leerTexto(mensaje));
            if (fecha == null) {
                System.out.println("  Escribe una fecha válida con el formato dd/MM/yyyy.");
            }
        } while (fecha == null);
        return fecha;
    }

    /**
     * Igual que {@link #leerFecha(String)}, pero si se pulsa Intro sin escribir nada
     * devuelve la fecha por defecto.
     */
    public static LocalDate leerFecha(String mensaje, LocalDate porDefecto) {
        LocalDate fecha;
        do {
            System.out.print(mensaje + " [Intro = " + Fechas.formatear(porDefecto) + "]: ");
            String texto = SC.nextLine().trim();
            fecha = texto.isEmpty() ? porDefecto : convertirFecha(texto);
            if (fecha == null) {
                System.out.println("  Escribe una fecha válida con el formato dd/MM/yyyy.");
            }
        } while (fecha == null);
        return fecha;
    }

    /**
     * Convierte un texto dd/MM/yyyy en fecha. Devuelve null si no tiene el formato o si
     * la fecha no existe (por ejemplo, 31/02/2026), sin provocar ningún error.
     */
    private static LocalDate convertirFecha(String texto) {
        if (!texto.matches(REGEX_FECHA)) {
            return null;
        }
        String[] partes = texto.split("/");
        int dia = Integer.parseInt(partes[0]);
        int mes = Integer.parseInt(partes[1]);
        int anio = Integer.parseInt(partes[2]);
        if (anio < 1 || mes < 1 || mes > 12 || dia < 1 || dia > YearMonth.of(anio, mes).lengthOfMonth()) {
            return null;
        }
        return LocalDate.of(anio, mes, dia);
    }

    /** Pide una hora con formato HH:mm (de 00:00 a 23:59) hasta que sea correcta. */
    public static LocalTime leerHora(String mensaje) {
        String texto;
        do {
            texto = leerTexto(mensaje);
            if (!texto.matches(REGEX_HORA)) {
                System.out.println("  Escribe una hora válida con el formato HH:mm (por ejemplo 09:30).");
            }
        } while (!texto.matches(REGEX_HORA));
        String[] partes = texto.split(":");
        return LocalTime.of(Integer.parseInt(partes[0]), Integer.parseInt(partes[1]));
    }

    /** Pide una respuesta hasta que sea s, S, n o N. Devuelve true si es "sí". */
    public static boolean leerSiNo(String mensaje) {
        String respuesta;
        do {
            System.out.print(mensaje + " (s/n): ");
            respuesta = SC.nextLine().trim();
        } while (!respuesta.matches("[sSnN]"));
        return respuesta.equalsIgnoreCase("s");
    }
}
