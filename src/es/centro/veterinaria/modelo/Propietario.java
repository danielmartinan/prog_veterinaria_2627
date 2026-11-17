package es.centro.veterinaria.modelo;

/** El propietario de una o varias mascotas. */
public class Propietario {
    private static final String REGEX_DNI = "\\d{8}[A-Z]";
    private static final String LETRAS_DNI = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final String REGEX_TELEFONO = "[6789]\\d{8}";
    private static final String REGEX_EMAIL = "[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[a-zA-Z]{2,}";

    private final String dni;
    private String nombre;
    private String telefono;
    private String email;

    /**
     * Crea un propietario. Antes hay que comprobar los datos con {@link #sonDatosValidos}.
     * Si aun así llega un nombre, un teléfono o un email no válidos, se guardan
     * "Sin nombre" y "" (sin teléfono o sin email).
     */
    public Propietario(String dni, String nombre, String telefono, String email) {
        this.dni = normalizarDni(dni);
        this.nombre = esNombreValido(nombre) ? nombre.trim() : "Sin nombre";
        this.telefono = esTelefonoValido(telefono) ? normalizarTelefono(telefono) : "";
        this.email = esEmailValido(email) ? email.trim() : "";
    }

    // ---------------------------------------------------------------- Validación

    /** Pasa el DNI a mayúsculas y le quita los espacios. Devuelve "" si es null. */
    public static String normalizarDni(String dni) {
        return (dni == null) ? "" : dni.trim().toUpperCase();
    }

    /** Quita los espacios del teléfono. Devuelve "" si es null. */
    private static String normalizarTelefono(String telefono) {
        return (telefono == null) ? "" : telefono.replace(" ", "");
    }

    /**
     * Comprueba todos los datos necesarios para dar de alta un propietario.
     * Es el único método de validación público: lo usa la clínica antes de crear el objeto.
     */
    public static boolean sonDatosValidos(String dni, String nombre, String telefono, String email) {
        return esDniValido(dni) && esNombreValido(nombre) && esTelefonoValido(telefono) && esEmailValido(email);
    }

    /** 8 dígitos y la letra que corresponde al número. */
    private static boolean esDniValido(String dni) {
        String limpio = normalizarDni(dni);
        if (!limpio.matches(REGEX_DNI)) {
            return false;
        }
        int numero = Integer.parseInt(limpio.substring(0, 8));
        return limpio.charAt(8) == LETRAS_DNI.charAt(numero % 23);
    }

    private static boolean esNombreValido(String nombre) {
        return nombre != null && !nombre.isBlank();
    }

    /** 9 dígitos empezando por 6, 7, 8 o 9. Se admiten espacios ("612 345 678"). */
    private static boolean esTelefonoValido(String telefono) {
        return normalizarTelefono(telefono).matches(REGEX_TELEFONO);
    }

    private static boolean esEmailValido(String email) {
        return email != null && email.trim().matches(REGEX_EMAIL);
    }

    // ---------------------------------------------------------------- Getters y setters

    public String getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    /** Cambia el nombre. Devuelve false, sin modificar nada, si está vacío. */
    public boolean setNombre(String nombre) {
        if (!esNombreValido(nombre)) {
            return false;
        }
        this.nombre = nombre.trim();
        return true;
    }

    /** Cambia el teléfono. Devuelve false, sin modificar nada, si no es válido. */
    public boolean setTelefono(String telefono) {
        if (!esTelefonoValido(telefono)) {
            return false;
        }
        this.telefono = normalizarTelefono(telefono);
        return true;
    }

    /** Cambia el email. Devuelve false, sin modificar nada, si no es válido. */
    public boolean setEmail(String email) {
        if (!esEmailValido(email)) {
            return false;
        }
        this.email = email.trim();
        return true;
    }

    @Override
    public String toString() {
        return nombre + " (" + dni + ") - tel. " + telefono + " - " + email;
    }
}
