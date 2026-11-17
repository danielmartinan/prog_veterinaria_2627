package es.centro.veterinaria.modelo;

import es.centro.veterinaria.util.Fechas;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;

/** Una mascota de la clínica, con su historial de peso y sus vacunas. */
public class Mascota {
    public static final int MAX_PESOS = 6;
    public static final int MAX_VACUNAS = 20;
    private static final String REGEX_MICROCHIP = "\\d{15}";
    private static final String RAZA_DESCONOCIDA = "Desconocida";

    private static int ultimoId = 0;

    private final int id;
    private final String microchip;        // null si no tiene (solo aves, reptiles y conejos)
    private String nombre;
    private final Especie especie;
    private String raza;
    private final LocalDate fechaNacimiento;
    private Propietario propietario;
    private double peso;

    private final double[] historialPeso = new double[MAX_PESOS];
    private int numPesos = 0;

    // Arrays paralelos: vacunas[i] se puso en la fecha fechasVacunas[i]
    private final String[] vacunas = new String[MAX_VACUNAS];
    private final LocalDate[] fechasVacunas = new LocalDate[MAX_VACUNAS];
    private int numVacunas = 0;

    /**
     * Crea una mascota. Antes hay que comprobar los datos con {@link #sonDatosValidos}:
     * así no se gasta un identificador con datos incorrectos. Si aun así llega algún dato
     * no válido, se usan valores por defecto (nombre "Sin nombre", especie PERRO, raza
     * "Desconocida", nacida hoy, peso 0 sin registrar en el historial).
     */
    public Mascota(String microchip, String nombre, Especie especie, String raza,
                   LocalDate fechaNacimiento, Propietario propietario, double peso) {
        ultimoId++;
        this.id = ultimoId;
        this.especie = (especie != null) ? especie : Especie.PERRO;
        this.microchip = esMicrochipValido(microchip, this.especie) ? normalizarMicrochip(microchip) : null;
        this.nombre = esTextoValido(nombre) ? nombre.trim() : "Sin nombre";
        this.raza = esTextoValido(raza) ? raza.trim() : RAZA_DESCONOCIDA;
        this.fechaNacimiento = esFechaNacimientoValida(fechaNacimiento) ? fechaNacimiento : LocalDate.now();
        this.propietario = propietario;
        if (esPesoValido(peso)) {
            anadirAlHistorial(peso);
        }
    }

    /** Crea una mascota sin raza ni microchip (pensado para aves y reptiles). */
    public Mascota(String nombre, Especie especie, LocalDate fechaNacimiento, Propietario propietario, double peso) {
        this(null, nombre, especie, null, fechaNacimiento, propietario, peso);
    }

    // ---------------------------------------------------------------- Validación

    /** Quita los espacios del microchip. Devuelve null si es null o queda vacío. */
    public static String normalizarMicrochip(String microchip) {
        if (microchip == null || microchip.isBlank()) {
            return null;
        }
        return microchip.replace(" ", "");
    }

    /**
     * Comprueba todos los datos necesarios para dar de alta una mascota.
     * Es el único método de validación público: lo usa la clínica antes de crear el objeto.
     */
    public static boolean sonDatosValidos(String microchip, String nombre, Especie especie,
                                          LocalDate fechaNacimiento, Propietario propietario, double peso) {
        return especie != null && esMicrochipValido(microchip, especie) && esTextoValido(nombre)
                && esFechaNacimientoValida(fechaNacimiento) && propietario != null && esPesoValido(peso);
    }

    /** 15 dígitos. Es obligatorio para perros y gatos; para el resto puede ser null. */
    private static boolean esMicrochipValido(String microchip, Especie especie) {
        String limpio = normalizarMicrochip(microchip);
        if (limpio == null) {
            return especie != Especie.PERRO && especie != Especie.GATO;
        }
        return limpio.matches(REGEX_MICROCHIP);
    }

    private static boolean esTextoValido(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static boolean esFechaNacimientoValida(LocalDate fecha) {
        return fecha != null && !fecha.isAfter(LocalDate.now());
    }

    private static boolean esPesoValido(double peso) {
        return peso > 0;
    }

    // ---------------------------------------------------------------- Edad

    /** Edad en años y meses, por ejemplo "3 años y 2 meses". */
    public String getEdad() {
        Period edad = Period.between(fechaNacimiento, LocalDate.now());
        int anios = edad.getYears();
        int meses = edad.getMonths();
        String textoAnios = anios + (anios == 1 ? " año" : " años");
        String textoMeses = meses + (meses == 1 ? " mes" : " meses");
        return textoAnios + " y " + textoMeses;
    }

    // ---------------------------------------------------------------- Peso

    /**
     * Registra un peso nuevo, que pasa a ser el peso actual. Si el historial está lleno,
     * se descarta el más antiguo. Devuelve false, sin modificar nada, si el peso no es positivo.
     */
    public boolean registrarPeso(double peso) {
        if (!esPesoValido(peso)) {
            return false;
        }
        anadirAlHistorial(peso);
        return true;
    }

    // Método privado: el constructor no debe llamar a métodos públicos que se puedan redefinir.
    private void anadirAlHistorial(double peso) {
        if (numPesos == MAX_PESOS) {
            for (int i = 0; i < MAX_PESOS - 1; i++) {   // se descarta el más antiguo
                historialPeso[i] = historialPeso[i + 1];
            }
            historialPeso[MAX_PESOS - 1] = peso;
        } else {
            historialPeso[numPesos] = peso;
            numPesos++;
        }
        this.peso = peso;
    }

    /**
     * Diferencia en porcentaje entre el primer y el último peso del historial.
     * Devuelve 0 si hay menos de dos pesos registrados.
     */
    public double variacionPeso() {
        if (numPesos < 2) {
            return 0;
        }
        double primero = historialPeso[0];
        double ultimo = historialPeso[numPesos - 1];
        return (ultimo - primero) / primero * 100;
    }

    /** Copia del historial con solo las posiciones ocupadas, del más antiguo al más reciente. */
    public double[] getHistorialPeso() {
        return Arrays.copyOf(historialPeso, numPesos);
    }

    // ---------------------------------------------------------------- Vacunas

    /** Anota una dosis de una vacuna. Devuelve false si el nombre está vacío, la fecha es null o no cabe. */
    public boolean anadirVacuna(String nombre, LocalDate fecha) {
        if (!esTextoValido(nombre) || fecha == null || numVacunas == MAX_VACUNAS) {
            return false;
        }
        vacunas[numVacunas] = nombre.trim();
        fechasVacunas[numVacunas] = fecha;
        numVacunas++;
        return true;
    }

    /**
     * Nombres de las vacunas cuya última dosis tiene más de un año. Si una vacuna aparece
     * varias veces (sin distinguir mayúsculas), solo cuenta la dosis más reciente.
     * Devuelve un array del tamaño exacto (vacío si no hay ninguna caducada).
     */
    public String[] vacunasCaducadas(LocalDate hoy) {
        String[] caducadas = new String[numVacunas];
        int total = 0;
        for (int i = 0; i < numVacunas; i++) {
            if (yaRevisada(i)) {
                continue;   // esta vacuna ya se comprobó con una dosis anterior del array
            }
            LocalDate ultimaDosis = ultimaDosis(vacunas[i]);
            if (ultimaDosis.plusYears(1).isBefore(hoy)) {
                caducadas[total] = vacunas[i];
                total++;
            }
        }
        return Arrays.copyOf(caducadas, total);
    }

    /** true si la vacuna de la posición i ya aparecía en una posición anterior. */
    private boolean yaRevisada(int i) {
        for (int j = 0; j < i; j++) {
            if (vacunas[j].equalsIgnoreCase(vacunas[i])) {
                return true;
            }
        }
        return false;
    }

    /** Fecha más reciente en la que se puso la vacuna indicada. */
    private LocalDate ultimaDosis(String vacuna) {
        LocalDate ultima = null;
        for (int i = 0; i < numVacunas; i++) {
            if (vacunas[i].equalsIgnoreCase(vacuna) && (ultima == null || fechasVacunas[i].isAfter(ultima))) {
                ultima = fechasVacunas[i];
            }
        }
        return ultima;
    }

    public boolean tieneVacunasCaducadas(LocalDate hoy) {
        return vacunasCaducadas(hoy).length > 0;
    }

    /** Listado de las dosis puestas, construido con StringBuilder. */
    public String informeVacunas() {
        if (numVacunas == 0) {
            return "  (sin vacunas registradas)\n";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numVacunas; i++) {
            sb.append("  ").append(Fechas.formatear(fechasVacunas[i])).append("  ").append(vacunas[i]).append('\n');
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- Getters y setters

    public int getId() {
        return id;
    }

    public String getMicrochip() {
        return microchip;
    }

    public String getNombre() {
        return nombre;
    }

    public Especie getEspecie() {
        return especie;
    }

    public String getRaza() {
        return raza;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public Propietario getPropietario() {
        return propietario;
    }

    public double getPeso() {
        return peso;
    }

    /** Cambia el nombre. Devuelve false, sin modificar nada, si está vacío. */
    public boolean setNombre(String nombre) {
        if (!esTextoValido(nombre)) {
            return false;
        }
        this.nombre = nombre.trim();
        return true;
    }

    /** Cambia la raza. Devuelve false, sin modificar nada, si está vacía. */
    public boolean setRaza(String raza) {
        if (!esTextoValido(raza)) {
            return false;
        }
        this.raza = raza.trim();
        return true;
    }

    /** Cambia de propietario. Devuelve false, sin modificar nada, si es null. */
    public boolean setPropietario(Propietario propietario) {
        if (propietario == null) {
            return false;
        }
        this.propietario = propietario;
        return true;
    }

    @Override
    public String toString() {
        String chip = (microchip == null) ? "sin chip" : "chip " + microchip;
        String duenio = (propietario == null) ? "sin propietario" : propietario.getNombre();
        return "#" + id + " " + nombre + " (" + especie + ", " + raza + ") - " + chip
                + String.format(" - %.2f kg", peso) + " - propietario: " + duenio;
    }
}
