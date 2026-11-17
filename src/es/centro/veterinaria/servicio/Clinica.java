package es.centro.veterinaria.servicio;

import es.centro.veterinaria.modelo.Cita;
import es.centro.veterinaria.modelo.EstadoCita;
import es.centro.veterinaria.modelo.Especie;
import es.centro.veterinaria.modelo.Mascota;
import es.centro.veterinaria.modelo.Propietario;
import es.centro.veterinaria.modelo.TipoConsulta;
import es.centro.veterinaria.util.Fechas;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Gestiona propietarios, mascotas, citas, agenda y facturación, y aplica las reglas de la
 * clínica. Los datos se guardan en arrays de capacidad fija con un contador.
 */
public class Clinica {
    public static final int CAPACIDAD_PROPIETARIOS = 50;
    public static final int CAPACIDAD_MASCOTAS = 100;
    public static final int CAPACIDAD_CITAS = 300;

    // --- Horario: de lunes a viernes, de 9:00 a 14:00 y de 16:00 a 20:00, en franjas de 30 min
    public static final LocalTime APERTURA_MANANA = LocalTime.of(9, 0);
    public static final LocalTime CIERRE_MANANA = LocalTime.of(14, 0);
    public static final LocalTime APERTURA_TARDE = LocalTime.of(16, 0);
    public static final LocalTime CIERRE_TARDE = LocalTime.of(20, 0);
    public static final int MINUTOS_FRANJA = 30;
    public static final int DIAS_LABORABLES = 5;
    /** Las horas de inicio de todas las franjas, en orden: 09:00, 09:30... 19:30 (18 franjas). */
    public static final LocalTime[] FRANJAS = calcularFranjas();
    public static final int DIAS_BUSQUEDA_FRANJA = 14;

    // --- Precios
    public static final double RECARGO_AVES_REPTILES = 20;     // %
    public static final double RECARGO_URGENCIA_FUERA = 50;    // %

    private static final char OCUPADA = 'X';
    private static final char URGENCIA = 'U';
    private static final char LIBRE = '·';
    private static final String[] NOMBRES_DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie"};

    private final Propietario[] propietarios = new Propietario[CAPACIDAD_PROPIETARIOS];
    private int numPropietarios = 0;

    private final Mascota[] mascotas = new Mascota[CAPACIDAD_MASCOTAS];
    private int numMascotas = 0;

    private final Cita[] citas = new Cita[CAPACIDAD_CITAS];
    private int numCitas = 0;

    // =================================================================== HORARIO

    private static LocalTime[] calcularFranjas() {
        int porManana = (int) (Duration.between(APERTURA_MANANA, CIERRE_MANANA).toMinutes() / MINUTOS_FRANJA);
        int porTarde = (int) (Duration.between(APERTURA_TARDE, CIERRE_TARDE).toMinutes() / MINUTOS_FRANJA);
        LocalTime[] franjas = new LocalTime[porManana + porTarde];
        for (int i = 0; i < porManana; i++) {
            franjas[i] = APERTURA_MANANA.plusMinutes((long) i * MINUTOS_FRANJA);
        }
        for (int i = 0; i < porTarde; i++) {
            franjas[porManana + i] = APERTURA_TARDE.plusMinutes((long) i * MINUTOS_FRANJA);
        }
        return franjas;
    }

    private static boolean esLaborable(LocalDate dia) {
        DayOfWeek diaSemana = dia.getDayOfWeek();
        return diaSemana != DayOfWeek.SATURDAY && diaSemana != DayOfWeek.SUNDAY;
    }

    /** Posición de la franja que empieza a esa hora, o -1 si no es el inicio de ninguna franja. */
    private static int indiceFranja(LocalTime hora) {
        for (int i = 0; i < FRANJAS.length; i++) {
            if (FRANJAS[i].equals(hora)) {
                return i;
            }
        }
        return -1;
    }

    /** Día laborable y hora de inicio de una franja (en punto o y media, dentro del horario). */
    private static boolean esDentroDeHorario(LocalDateTime fechaHora) {
        return esLaborable(fechaHora.toLocalDate()) && indiceFranja(fechaHora.toLocalTime()) != -1;
    }

    /** Hora en punto o y media, sin segundos. */
    private static boolean esEnPuntoOYMedia(LocalDateTime fechaHora) {
        int minuto = fechaHora.getMinute();
        return (minuto == 0 || minuto == 30) && fechaHora.getSecond() == 0 && fechaHora.getNano() == 0;
    }

    // =================================================================== PROPIETARIOS

    /** Devuelve el propietario creado, o null si los datos no son válidos, el DNI ya existe o no cabe. */
    public Propietario altaPropietario(String dni, String nombre, String telefono, String email) {
        if (!Propietario.sonDatosValidos(dni, nombre, telefono, email)
                || buscarPropietario(dni) != null || numPropietarios == CAPACIDAD_PROPIETARIOS) {
            return null;
        }
        Propietario propietario = new Propietario(dni, nombre, telefono, email);
        propietarios[numPropietarios] = propietario;
        numPropietarios++;
        return propietario;
    }

    /** Devuelve el propietario con ese DNI, o null si no existe. */
    public Propietario buscarPropietario(String dni) {
        String buscado = Propietario.normalizarDni(dni);
        for (int i = 0; i < numPropietarios; i++) {
            if (propietarios[i].getDni().equals(buscado)) {
                return propietarios[i];
            }
        }
        return null;
    }

    /** Copia con los propietarios dados de alta. */
    public Propietario[] getPropietarios() {
        Propietario[] copia = new Propietario[numPropietarios];
        for (int i = 0; i < numPropietarios; i++) {
            copia[i] = propietarios[i];
        }
        return copia;
    }

    // =================================================================== MASCOTAS

    /**
     * Da de alta una mascota con todos sus datos. Devuelve la mascota creada, o null si el
     * propietario no existe, los datos no son válidos, el microchip ya está registrado o no cabe.
     */
    public Mascota altaMascota(String dniPropietario, String microchip, String nombre, Especie especie,
                               String raza, LocalDate fechaNacimiento, double peso) {
        Propietario propietario = buscarPropietario(dniPropietario);
        if (!puedeDarseDeAlta(microchip, nombre, especie, fechaNacimiento, propietario, peso)) {
            return null;
        }
        return guardarMascota(new Mascota(microchip, nombre, especie, raza, fechaNacimiento, propietario, peso));
    }

    /** Da de alta una mascota sin raza ni microchip (aves y reptiles). Devuelve null si no es posible. */
    public Mascota altaMascota(String dniPropietario, String nombre, Especie especie,
                               LocalDate fechaNacimiento, double peso) {
        Propietario propietario = buscarPropietario(dniPropietario);
        if (!puedeDarseDeAlta(null, nombre, especie, fechaNacimiento, propietario, peso)) {
            return null;
        }
        return guardarMascota(new Mascota(nombre, especie, fechaNacimiento, propietario, peso));
    }

    private boolean puedeDarseDeAlta(String microchip, String nombre, Especie especie,
                                     LocalDate fechaNacimiento, Propietario propietario, double peso) {
        String chip = Mascota.normalizarMicrochip(microchip);
        return Mascota.sonDatosValidos(microchip, nombre, especie, fechaNacimiento, propietario, peso)
                && (chip == null || buscarPorMicrochip(chip) == null)
                && numMascotas < CAPACIDAD_MASCOTAS;
    }

    private Mascota guardarMascota(Mascota mascota) {
        mascotas[numMascotas] = mascota;
        numMascotas++;
        return mascota;
    }

    /**
     * Da de baja una mascota desplazando los elementos siguientes. Devuelve false si no
     * existe o si tiene citas pendientes. Sus citas antiguas se conservan en el historial.
     */
    public boolean bajaMascota(int id) {
        int posicion = posicionMascota(id);
        if (posicion == -1 || tieneCitasPendientes(id)) {
            return false;
        }
        for (int i = posicion; i < numMascotas - 1; i++) {
            mascotas[i] = mascotas[i + 1];
        }
        numMascotas--;
        mascotas[numMascotas] = null;
        return true;
    }

    private boolean tieneCitasPendientes(int idMascota) {
        for (int i = 0; i < numCitas; i++) {
            if (citas[i].getMascota().getId() == idMascota && citas[i].estaPendiente()) {
                return true;
            }
        }
        return false;
    }

    private int posicionMascota(int id) {
        for (int i = 0; i < numMascotas; i++) {
            if (mascotas[i].getId() == id) {
                return i;
            }
        }
        return -1;
    }

    /** Devuelve la mascota con ese id, o null si no existe. */
    public Mascota buscarMascota(int id) {
        int posicion = posicionMascota(id);
        return (posicion == -1) ? null : mascotas[posicion];
    }

    /** Devuelve la mascota con ese microchip (se admiten espacios), o null si no existe. */
    public Mascota buscarPorMicrochip(String microchip) {
        String buscado = Mascota.normalizarMicrochip(microchip);
        if (buscado == null) {
            return null;
        }
        for (int i = 0; i < numMascotas; i++) {
            if (buscado.equals(mascotas[i].getMicrochip())) {   // getMicrochip() puede ser null
                return mascotas[i];
            }
        }
        return null;
    }

    /** Mascotas cuyo nombre contiene el texto, sin distinguir mayúsculas (array de tamaño exacto). */
    public Mascota[] buscarMascotas(String texto) {
        if (texto == null || texto.isBlank()) {
            return new Mascota[0];
        }
        String buscado = texto.trim().toLowerCase();
        int total = 0;
        for (int i = 0; i < numMascotas; i++) {
            if (mascotas[i].getNombre().toLowerCase().contains(buscado)) {
                total++;
            }
        }
        Mascota[] resultado = new Mascota[total];
        int j = 0;
        for (int i = 0; i < numMascotas; i++) {
            if (mascotas[i].getNombre().toLowerCase().contains(buscado)) {
                resultado[j] = mascotas[i];
                j++;
            }
        }
        return resultado;
    }

    /** Mascotas de un propietario (array de tamaño exacto, vacío si no tiene o no existe). */
    public Mascota[] mascotasDePropietario(String dni) {
        String buscado = Propietario.normalizarDni(dni);
        int total = 0;
        for (int i = 0; i < numMascotas; i++) {
            if (mascotas[i].getPropietario().getDni().equals(buscado)) {
                total++;
            }
        }
        Mascota[] resultado = new Mascota[total];
        int j = 0;
        for (int i = 0; i < numMascotas; i++) {
            if (mascotas[i].getPropietario().getDni().equals(buscado)) {
                resultado[j] = mascotas[i];
                j++;
            }
        }
        return resultado;
    }

    // =================================================================== CITAS

    /**
     * Pide una cita. Devuelve la cita creada, o null si la mascota no existe, la fecha ya ha
     * pasado, no es una hora en punto o y media, la franja está ocupada o, salvo en las
     * urgencias, no es un día laborable dentro del horario.
     */
    public Cita pedirCita(int idMascota, LocalDateTime fechaHora, TipoConsulta tipo) {
        Mascota mascota = buscarMascota(idMascota);
        if (mascota == null || tipo == null || !esFechaValidaParaCita(fechaHora, tipo, null)) {
            return null;
        }
        Cita cita = new Cita(mascota, fechaHora, tipo);
        return guardarCita(cita) ? cita : null;
    }

    /**
     * Comprueba la fecha de una cita. La cita "ignorada" no cuenta como ocupación (sirve
     * para reprogramar una cita a su misma hora sin que choque consigo misma).
     */
    private boolean esFechaValidaParaCita(LocalDateTime fechaHora, TipoConsulta tipo, Cita ignorada) {
        if (fechaHora == null || fechaHora.isBefore(LocalDateTime.now()) || !esEnPuntoOYMedia(fechaHora)) {
            return false;
        }
        if (tipo != TipoConsulta.URGENCIA && !esDentroDeHorario(fechaHora)) {
            return false;
        }
        return !estaOcupada(fechaHora, ignorada);
    }

    /** true si hay una cita no cancelada a esa hora. */
    public boolean franjaOcupada(LocalDateTime fechaHora) {
        return estaOcupada(fechaHora, null);
    }

    private boolean estaOcupada(LocalDateTime fechaHora, Cita ignorada) {
        for (int i = 0; i < numCitas; i++) {
            if (citas[i] != ignorada && citas[i].ocupaFranja() && citas[i].getFechaHora().equals(fechaHora)) {
                return true;
            }
        }
        return false;
    }

    private boolean guardarCita(Cita cita) {
        if (numCitas == CAPACIDAD_CITAS) {
            return false;
        }
        citas[numCitas] = cita;
        numCitas++;
        return true;
    }

    /** Devuelve la cita con ese id, o null si no existe. */
    public Cita buscarCita(int id) {
        for (int i = 0; i < numCitas; i++) {
            if (citas[i].getId() == id) {
                return citas[i];
            }
        }
        return null;
    }

    /** Cancela una cita. Devuelve false si no existe o no está pendiente. */
    public boolean cancelarCita(int id) {
        Cita cita = buscarCita(id);
        return cita != null && cita.cancelar();
    }

    /**
     * Cancela la cita y crea otra igual en la nueva fecha (constructor de copia).
     * Devuelve la cita nueva, o null si la original no existe o no está pendiente, o la
     * nueva fecha no es válida. En ese caso la original no se cancela.
     */
    public Cita reprogramarCita(int id, LocalDateTime nueva) {
        Cita original = buscarCita(id);
        if (original == null || !original.estaPendiente() || numCitas == CAPACIDAD_CITAS
                || !esFechaValidaParaCita(nueva, original.getTipo(), original)) {
            return null;
        }
        original.cancelar();
        Cita reprogramada = new Cita(original, nueva);
        guardarCita(reprogramada);
        return reprogramada;
    }

    /**
     * Atiende una cita pendiente: guarda el diagnóstico, registra el peso de la mascota y,
     * si es una vacunación, le añade la vacuna con la fecha de la cita. Devuelve el importe,
     * o -1 (sin modificar nada) si la cita no existe o no está pendiente, el diagnóstico
     * está vacío, el peso no es positivo o falta el nombre de la vacuna en una vacunación.
     * En el resto de consultas, el parámetro vacuna se ignora y puede ser null.
     */
    public double atenderCita(int id, String diagnostico, double peso, String vacuna) {
        Cita cita = buscarCita(id);
        if (cita == null || !cita.estaPendiente() || diagnostico == null || diagnostico.isBlank() || peso <= 0) {
            return -1;
        }
        boolean esVacunacion = cita.getTipo() == TipoConsulta.VACUNACION;
        if (esVacunacion && (vacuna == null || vacuna.isBlank())) {
            return -1;
        }
        double importe = calcularImporte(cita);
        cita.atender(diagnostico, importe);
        Mascota mascota = cita.getMascota();
        mascota.registrarPeso(peso);
        if (esVacunacion) {
            mascota.anadirVacuna(vacuna, cita.getFechaHora().toLocalDate());
        }
        return importe;
    }

    /**
     * Precio base según el tipo, más un 20 % para aves y reptiles y un 50 % para las
     * urgencias fuera de horario. Los recargos se suman sobre el precio base.
     */
    private double calcularImporte(Cita cita) {
        double base = switch (cita.getTipo()) {
            case REVISION -> 30;
            case VACUNACION -> 45;
            case URGENCIA -> 80;
            case CIRUGIA -> 250;
        };
        double recargo = 0;
        Especie especie = cita.getMascota().getEspecie();
        if (especie == Especie.AVE || especie == Especie.REPTIL) {
            recargo += RECARGO_AVES_REPTILES;
        }
        if (cita.getTipo() == TipoConsulta.URGENCIA && !esDentroDeHorario(cita.getFechaHora())) {
            recargo += RECARGO_URGENCIA_FUERA;
        }
        double importe = base * (1 + recargo / 100);
        return Math.round(importe * 100) / 100.0;   // redondeo a céntimos
    }

    /** Citas no canceladas de ese día, ordenadas por hora (selección). */
    public Cita[] citasDelDia(LocalDate dia) {
        int total = 0;
        for (int i = 0; i < numCitas; i++) {
            if (citas[i].ocupaFranja() && citas[i].getFechaHora().toLocalDate().equals(dia)) {
                total++;
            }
        }
        Cita[] resultado = new Cita[total];
        int j = 0;
        for (int i = 0; i < numCitas; i++) {
            if (citas[i].ocupaFranja() && citas[i].getFechaHora().toLocalDate().equals(dia)) {
                resultado[j] = citas[i];
                j++;
            }
        }
        for (int i = 0; i < resultado.length - 1; i++) {
            int menor = i;
            for (int k = i + 1; k < resultado.length; k++) {
                if (resultado[k].getFechaHora().isBefore(resultado[menor].getFechaHora())) {
                    menor = k;
                }
            }
            Cita aux = resultado[i];
            resultado[i] = resultado[menor];
            resultado[menor] = aux;
        }
        return resultado;
    }

    /** Citas atendidas de una mascota. */
    public Cita[] historialMascota(int idMascota) {
        return citasDeMascota(idMascota, true);
    }

    /** Todas las citas de una mascota, en cualquier estado. */
    public Cita[] citasDeMascota(int idMascota) {
        return citasDeMascota(idMascota, false);
    }

    private Cita[] citasDeMascota(int idMascota, boolean soloAtendidas) {
        int total = 0;
        for (int i = 0; i < numCitas; i++) {
            if (esDeMascota(citas[i], idMascota, soloAtendidas)) {
                total++;
            }
        }
        Cita[] resultado = new Cita[total];
        int j = 0;
        for (int i = 0; i < numCitas; i++) {
            if (esDeMascota(citas[i], idMascota, soloAtendidas)) {
                resultado[j] = citas[i];
                j++;
            }
        }
        return resultado;
    }

    private boolean esDeMascota(Cita cita, int idMascota, boolean soloAtendidas) {
        return cita.getMascota().getId() == idMascota
                && (!soloAtendidas || cita.getEstado() == EstadoCita.ATENDIDA);
    }

    // =================================================================== AGENDA

    /**
     * Agenda de la semana del día indicado: una fila por franja y una columna por día
     * laborable. X = ocupada, U = urgencia, · = libre.
     */
    public String agendaSemana(LocalDate dia) {
        LocalDate lunes = dia.minusDays(dia.getDayOfWeek().getValue() - 1);
        char[][] agenda = new char[FRANJAS.length][DIAS_LABORABLES];
        for (int f = 0; f < FRANJAS.length; f++) {
            for (int d = 0; d < DIAS_LABORABLES; d++) {
                agenda[f][d] = LIBRE;
            }
        }
        for (int i = 0; i < numCitas; i++) {
            Cita cita = citas[i];
            int columna = (int) ChronoUnit.DAYS.between(lunes, cita.getFechaHora().toLocalDate());
            int fila = indiceFranja(cita.getFechaHora().toLocalTime());
            if (cita.ocupaFranja() && columna >= 0 && columna < DIAS_LABORABLES && fila != -1) {
                agenda[fila][columna] = (cita.getTipo() == TipoConsulta.URGENCIA) ? URGENCIA : OCUPADA;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Semana del ").append(Fechas.formatear(lunes)).append(" al ")
                .append(Fechas.formatear(lunes.plusDays(DIAS_LABORABLES - 1))).append('\n');
        sb.append("       ");
        for (int d = 0; d < DIAS_LABORABLES; d++) {
            sb.append(String.format(" %s %02d", NOMBRES_DIAS[d], lunes.plusDays(d).getDayOfMonth()));
        }
        sb.append('\n');
        for (int f = 0; f < FRANJAS.length; f++) {
            if (FRANJAS[f].equals(APERTURA_TARDE)) {
                sb.append('\n');   // separación entre mañana y tarde
            }
            sb.append(FRANJAS[f]).append("  ");
            for (int d = 0; d < DIAS_LABORABLES; d++) {
                sb.append("   ").append(agenda[f][d]).append("   ");
            }
            sb.append('\n');
        }
        sb.append("(X ocupada, U urgencia, ").append(LIBRE).append(" libre. Las urgencias fuera de horario no aparecen.)");
        return sb.toString();
    }

    /**
     * Primera franja libre dentro del horario a partir de un momento dado (y nunca en el
     * pasado), buscando como mucho DIAS_BUSQUEDA_FRANJA días. Devuelve null si no hay ninguna.
     */
    public LocalDateTime primeraFranjaLibre(LocalDateTime desde) {
        LocalDateTime inicio = desde.isBefore(LocalDateTime.now()) ? LocalDateTime.now() : desde;
        LocalDate dia = inicio.toLocalDate();
        for (int d = 0; d <= DIAS_BUSQUEDA_FRANJA; d++) {
            if (esLaborable(dia)) {
                for (LocalTime franja : FRANJAS) {
                    LocalDateTime candidata = LocalDateTime.of(dia, franja);
                    if (!candidata.isBefore(inicio) && !franjaOcupada(candidata)) {
                        return candidata;
                    }
                }
            }
            dia = dia.plusDays(1);
        }
        return null;
    }

    // =================================================================== INFORMES

    /**
     * Facturación de las citas atendidas en un mes: total, desglose por tipo de consulta,
     * especie que más ha facturado e importe medio por cita.
     */
    public String facturacionMes(int anio, int mes) {
        if (mes < 1 || mes > 12) {
            return "Mes no válido.";
        }
        TipoConsulta[] tipos = TipoConsulta.values();
        Especie[] especies = Especie.values();
        double[] porTipo = new double[tipos.length];
        double[] porEspecie = new double[especies.length];
        double total = 0;
        int numAtendidas = 0;

        for (int i = 0; i < numCitas; i++) {
            Cita cita = citas[i];
            LocalDateTime fecha = cita.getFechaHora();
            if (cita.getEstado() == EstadoCita.ATENDIDA && fecha.getYear() == anio && fecha.getMonthValue() == mes) {
                porTipo[cita.getTipo().ordinal()] += cita.getImporte();
                porEspecie[cita.getMascota().getEspecie().ordinal()] += cita.getImporte();
                total += cita.getImporte();
                numAtendidas++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("FACTURACIÓN %02d/%d%n", mes, anio));
        if (numAtendidas == 0) {
            sb.append("No hay citas atendidas ese mes.");
            return sb.toString();
        }
        for (int i = 0; i < tipos.length; i++) {
            sb.append(String.format("  %-11s %10.2f €%n", tipos[i], porTipo[i]));
        }
        sb.append(String.format("  %-11s %10.2f €%n", "TOTAL", total));

        int mejor = 0;
        for (int i = 1; i < especies.length; i++) {
            if (porEspecie[i] > porEspecie[mejor]) {
                mejor = i;
            }
        }
        sb.append(String.format("Especie que más ha facturado: %s (%.2f €)%n", especies[mejor], porEspecie[mejor]));
        sb.append(String.format("Citas atendidas: %d. Importe medio por cita: %.2f €", numAtendidas, total / numAtendidas));
        return sb.toString();
    }

    /** Mascotas con alguna vacuna caducada en esa fecha (array de tamaño exacto). */
    public Mascota[] mascotasConVacunasCaducadas(LocalDate hoy) {
        int total = 0;
        for (int i = 0; i < numMascotas; i++) {
            if (mascotas[i].tieneVacunasCaducadas(hoy)) {
                total++;
            }
        }
        Mascota[] resultado = new Mascota[total];
        int j = 0;
        for (int i = 0; i < numMascotas; i++) {
            if (mascotas[i].tieneVacunasCaducadas(hoy)) {
                resultado[j] = mascotas[i];
                j++;
            }
        }
        return resultado;
    }

    // =================================================================== DATOS DE PRUEBA

    /**
     * Carga propietarios, mascotas y citas de ejemplo. Las fechas se calculan a partir de
     * hoy, así que siempre hay citas atendidas este mes y los anteriores, vacunas caducadas,
     * citas pendientes en los próximos días y una urgencia en fin de semana.
     */
    public void cargarDatosDePrueba() {
        LocalDate hoy = LocalDate.now();

        altaPropietario("12345678Z", "Ana García", "612 345 678", "ana.garcia@correo.es");
        altaPropietario("23456789D", "Luis Pérez", "623456789", "luis.perez@correo.es");
        altaPropietario("34567890V", "Marta López", "934 567 890", "marta.lopez@correo.es");
        altaPropietario("45678901G", "Javier Ruiz", "745678901", "javier.ruiz@correo.es");
        altaPropietario("55555555K", "Lucía Fernández", "655 555 555", "lucia.fernandez@correo.es");

        Mascota toby = altaMascota("12345678Z", "941000012345671", "Toby", Especie.PERRO, "Beagle",
                hoy.minusYears(3).minusMonths(2), 8.0);
        Mascota misi = altaMascota("12345678Z", "941000012345672", "Misi", Especie.GATO, "Europeo",
                hoy.minusYears(5).minusMonths(7), 3.8);
        Mascota luna = altaMascota("23456789D", "941000012345673", "Luna", Especie.PERRO, "Golden retriever",
                hoy.minusYears(1).minusMonths(1), 25.0);
        Mascota garfield = altaMascota("34567890V", "941000012345674", "Garfield", Especie.GATO, "Persa",
                hoy.minusYears(8), 5.6);
        Mascota tambor = altaMascota("34567890V", null, "Tambor", Especie.CONEJO, "Belier",
                hoy.minusMonths(10), 1.6);
        Mascota piolin = altaMascota("45678901G", "Piolín", Especie.AVE, hoy.minusYears(2), 0.03);
        Mascota rex = altaMascota("45678901G", "Rex", Especie.REPTIL, hoy.minusYears(4).minusMonths(3), 0.40);
        Mascota kira = altaMascota("55555555K", "941000012345675", "Kira", Especie.PERRO, "Pastor alemán",
                hoy.minusMonths(7), 15.5);
        altaMascota("55555555K", "Coco", Especie.AVE, hoy.minusYears(6), 0.4);

        // Citas ya atendidas (se registran directamente, porque pedirCita no admite fechas pasadas).
        // El peso del alta es el primero del historial; cada cita atendida añade otro.
        historica(toby, diaLaborableAntes(hoy.minusDays(500)), 10, TipoConsulta.VACUNACION, "Primera dosis", 9.0, "Rabia");
        historica(toby, diaLaborableAntes(hoy.minusDays(420)), 11, TipoConsulta.VACUNACION, "Polivalente anual", 10.2, "Polivalente");
        historica(misi, diaLaborableAntes(hoy.minusDays(400)), 9, TipoConsulta.VACUNACION, "Vacuna anual", 4.0, "Trivalente felina");
        historica(toby, diaLaborableAntes(hoy.minusDays(120)), 17, TipoConsulta.REVISION, "Otitis leve", 11.4, null);
        historica(garfield, diaLaborableAntes(hoy.minusDays(75)), 12, TipoConsulta.CIRUGIA, "Extracción dental", 5.9, null);
        historica(rex, diaLaborableAntes(hoy.minusDays(60)), 18, TipoConsulta.REVISION, "Muda correcta", 0.42, null);
        historica(luna, diaLaborableAntes(hoy.minusDays(40)), 10, TipoConsulta.URGENCIA, "Ingesta de chocolate", 27.5, null);
        historica(toby, diaLaborableAntes(hoy.minusDays(30)), 9, TipoConsulta.VACUNACION, "Refuerzo", 12.1, "Rabia");
        historica(piolin, diaLaborableAntes(hoy.minusDays(8)), 16, TipoConsulta.REVISION, "Plumaje sano", 0.03, null);
        historica(kira, diaLaborableAntes(hoy.minusDays(5)), 11, TipoConsulta.VACUNACION, "Cachorro", 18.0, "Polivalente");
        historica(tambor, diaLaborableAntes(hoy.minusDays(3)), 13, TipoConsulta.REVISION, "Dientes largos", 1.9, null);
        historica(toby, diaLaborableAntes(hoy.minusDays(1)), 19, TipoConsulta.REVISION, "Control de peso", 12.6, null);

        // Citas pendientes en los próximos días laborables
        LocalDate manana = diaLaborableDespues(hoy);
        LocalDate pasado = diaLaborableDespues(manana);
        pedirCita(toby.getId(), LocalDateTime.of(manana, LocalTime.of(9, 0)), TipoConsulta.REVISION);
        pedirCita(misi.getId(), LocalDateTime.of(manana, LocalTime.of(9, 30)), TipoConsulta.VACUNACION);
        pedirCita(luna.getId(), LocalDateTime.of(manana, LocalTime.of(10, 0)), TipoConsulta.REVISION);
        pedirCita(garfield.getId(), LocalDateTime.of(manana, LocalTime.of(16, 0)), TipoConsulta.REVISION);
        pedirCita(rex.getId(), LocalDateTime.of(pasado, LocalTime.of(11, 0)), TipoConsulta.CIRUGIA);
        pedirCita(kira.getId(), LocalDateTime.of(pasado, LocalTime.of(12, 30)), TipoConsulta.URGENCIA);

        // Urgencia el próximo sábado por la noche (fuera de horario: lleva recargo)
        LocalDate sabado = hoy.plusDays(1);
        while (sabado.getDayOfWeek() != DayOfWeek.SATURDAY) {
            sabado = sabado.plusDays(1);
        }
        pedirCita(luna.getId(), LocalDateTime.of(sabado, LocalTime.of(22, 0)), TipoConsulta.URGENCIA);
    }

    /** Registra y atiende una cita en el pasado (solo para los datos de prueba). */
    private void historica(Mascota mascota, LocalDate dia, int hora, TipoConsulta tipo,
                           String diagnostico, double peso, String vacuna) {
        Cita cita = new Cita(mascota, LocalDateTime.of(dia, LocalTime.of(hora, 0)), tipo);
        if (guardarCita(cita)) {
            atenderCita(cita.getId(), diagnostico, peso, vacuna);
        }
    }

    /** El mismo día si es laborable o, si no, el viernes anterior. */
    private static LocalDate diaLaborableAntes(LocalDate dia) {
        LocalDate resultado = dia;
        while (!esLaborable(resultado)) {
            resultado = resultado.minusDays(1);
        }
        return resultado;
    }

    /** El primer día laborable posterior al indicado. */
    private static LocalDate diaLaborableDespues(LocalDate dia) {
        LocalDate resultado = dia.plusDays(1);
        while (!esLaborable(resultado)) {
            resultado = resultado.plusDays(1);
        }
        return resultado;
    }
}
