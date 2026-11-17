package es.centro.veterinaria.aplicacion;

import es.centro.veterinaria.modelo.Cita;
import es.centro.veterinaria.modelo.Especie;
import es.centro.veterinaria.modelo.Mascota;
import es.centro.veterinaria.modelo.Propietario;
import es.centro.veterinaria.modelo.TipoConsulta;
import es.centro.veterinaria.servicio.Clinica;
import es.centro.veterinaria.util.Consola;
import es.centro.veterinaria.util.Fechas;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Menú de la aplicación. Solo se comunica con el usuario: pide los datos, llama a la
 * clínica y muestra el resultado.
 */
public class Main {
    private static final Clinica clinica = new Clinica();

    public static void main(String[] args) {
        clinica.cargarDatosDePrueba();

        int opcion;
        do {
            System.out.println();
            System.out.println("=== CLÍNICA VETERINARIA ===");
            System.out.println("1. Propietarios");
            System.out.println("2. Mascotas");
            System.out.println("3. Citas");
            System.out.println("4. Agenda semanal");
            System.out.println("5. Informes");
            System.out.println("0. Salir");
            opcion = Consola.leerEntero("Opción: ", 0, 5);

            switch (opcion) {
                case 1 -> menuPropietarios();
                case 2 -> menuMascotas();
                case 3 -> menuCitas();
                case 4 -> System.out.println(clinica.agendaSemana(Consola.leerFecha("Un día de la semana", LocalDate.now())));
                case 5 -> menuInformes();
                case 0 -> System.out.println("¡Hasta pronto!");
            }
        } while (opcion != 0);
    }

    // =================================================================== PROPIETARIOS

    private static void menuPropietarios() {
        int opcion;
        do {
            System.out.println();
            System.out.println("--- PROPIETARIOS ---");
            System.out.println("1. Alta");
            System.out.println("2. Buscar por DNI");
            System.out.println("3. Ver sus mascotas");
            System.out.println("4. Listar todos");
            System.out.println("0. Volver");
            opcion = Consola.leerEntero("Opción: ", 0, 4);

            switch (opcion) {
                case 1 -> altaPropietario();
                case 2 -> {
                    Propietario propietario = clinica.buscarPropietario(Consola.leerTexto("DNI: "));
                    System.out.println(propietario == null ? "No existe ningún propietario con ese DNI." : propietario);
                }
                case 3 -> mostrarMascotas(clinica.mascotasDePropietario(Consola.leerTexto("DNI del propietario: ")));
                case 4 -> {
                    for (Propietario propietario : clinica.getPropietarios()) {
                        System.out.println(propietario);
                    }
                }
            }
        } while (opcion != 0);
    }

    private static void altaPropietario() {
        String dni = Consola.leerTexto("DNI: ");
        String nombre = Consola.leerTexto("Nombre: ");
        String telefono = Consola.leerTexto("Teléfono: ");
        String email = Consola.leerTexto("Email: ");
        Propietario propietario = clinica.altaPropietario(dni, nombre, telefono, email);
        if (propietario == null) {
            System.out.println("No se ha podido dar de alta: algún dato no es válido o el DNI ya existe.");
        } else {
            System.out.println("Alta realizada: " + propietario);
        }
    }

    // =================================================================== MASCOTAS

    private static void menuMascotas() {
        int opcion;
        do {
            System.out.println();
            System.out.println("--- MASCOTAS ---");
            System.out.println("1. Alta");
            System.out.println("2. Buscar por nombre");
            System.out.println("3. Buscar por microchip");
            System.out.println("4. Ficha completa");
            System.out.println("5. Baja");
            System.out.println("0. Volver");
            opcion = Consola.leerEntero("Opción: ", 0, 5);

            switch (opcion) {
                case 1 -> altaMascota();
                case 2 -> mostrarMascotas(clinica.buscarMascotas(Consola.leerTexto("Texto del nombre: ")));
                case 3 -> {
                    Mascota mascota = clinica.buscarPorMicrochip(Consola.leerTexto("Microchip: "));
                    System.out.println(mascota == null ? "No hay ninguna mascota con ese microchip." : mascota);
                }
                case 4 -> fichaMascota();
                case 5 -> {
                    int id = Consola.leerEntero("Id de la mascota: ", 1, Integer.MAX_VALUE);
                    System.out.println(clinica.bajaMascota(id) ? "Mascota dada de baja."
                            : "No se ha podido dar de baja: no existe o tiene citas pendientes.");
                }
            }
        } while (opcion != 0);
    }

    private static void altaMascota() {
        String dni = Consola.leerTexto("DNI del propietario: ");
        if (clinica.buscarPropietario(dni) == null) {
            System.out.println("No existe ningún propietario con ese DNI. Dalo de alta primero.");
            return;
        }
        String nombre = Consola.leerTexto("Nombre: ");
        Especie especie = elegirEspecie();
        LocalDate nacimiento = Consola.leerFecha("Fecha de nacimiento (dd/MM/yyyy): ");
        double peso = Consola.leerDecimal("Peso (kg): ", 0.01, 200);

        Mascota mascota;
        if (especie == Especie.AVE || especie == Especie.REPTIL) {
            mascota = clinica.altaMascota(dni, nombre, especie, nacimiento, peso);
        } else {
            String raza = Consola.leerTexto("Raza: ");
            String microchip = null;
            if (especie == Especie.PERRO || especie == Especie.GATO || Consola.leerSiNo("¿Tiene microchip?")) {
                microchip = Consola.leerTexto("Microchip (15 dígitos): ");
            }
            mascota = clinica.altaMascota(dni, microchip, nombre, especie, raza, nacimiento, peso);
        }

        if (mascota == null) {
            System.out.println("No se ha podido dar de alta: algún dato no es válido o el microchip ya está registrado.");
        } else {
            System.out.println("Alta realizada: " + mascota);
        }
    }

    /** Menú generado con Especie.values(). */
    private static Especie elegirEspecie() {
        Especie[] especies = Especie.values();
        for (int i = 0; i < especies.length; i++) {
            System.out.println("  " + (i + 1) + ". " + especies[i]);
        }
        return especies[Consola.leerEntero("Especie: ", 1, especies.length) - 1];
    }

    private static void fichaMascota() {
        Mascota mascota = clinica.buscarMascota(Consola.leerEntero("Id de la mascota: ", 1, Integer.MAX_VALUE));
        if (mascota == null) {
            System.out.println("No existe ninguna mascota con ese id.");
            return;
        }
        System.out.println();
        System.out.println(mascota);
        System.out.println("Nacimiento: " + Fechas.formatear(mascota.getFechaNacimiento()) + " (" + mascota.getEdad() + ")");
        System.out.println("Propietario: " + mascota.getPropietario());

        System.out.print("Historial de peso (kg):");
        for (double peso : mascota.getHistorialPeso()) {
            System.out.printf(" %.2f", peso);
        }
        System.out.printf("  -> variación %+.1f %%%n", mascota.variacionPeso());

        System.out.println("Vacunas:");
        System.out.print(mascota.informeVacunas());
        String[] caducadas = mascota.vacunasCaducadas(LocalDate.now());
        if (caducadas.length > 0) {
            System.out.println("  ¡Caducadas!: " + String.join(", ", caducadas));
        }

        System.out.println("Citas:");
        Cita[] citas = clinica.citasDeMascota(mascota.getId());
        if (citas.length == 0) {
            System.out.println("  (ninguna)");
        }
        for (Cita cita : citas) {
            System.out.println("  " + cita);
        }
    }

    private static void mostrarMascotas(Mascota[] lista) {
        if (lista.length == 0) {
            System.out.println("No hay mascotas que mostrar.");
        }
        for (Mascota mascota : lista) {
            System.out.println(mascota);
        }
    }

    // =================================================================== CITAS

    private static void menuCitas() {
        int opcion;
        do {
            System.out.println();
            System.out.println("--- CITAS ---");
            System.out.println("1. Pedir cita");
            System.out.println("2. Cancelar cita");
            System.out.println("3. Reprogramar cita");
            System.out.println("4. Atender cita");
            System.out.println("5. Citas del día");
            System.out.println("0. Volver");
            opcion = Consola.leerEntero("Opción: ", 0, 5);

            switch (opcion) {
                case 1 -> pedirCita();
                case 2 -> {
                    int id = Consola.leerEntero("Id de la cita: ", 1, Integer.MAX_VALUE);
                    System.out.println(clinica.cancelarCita(id) ? "Cita cancelada." : "No existe o no está pendiente.");
                }
                case 3 -> reprogramarCita();
                case 4 -> atenderCita();
                case 5 -> citasDelDia();
            }
        } while (opcion != 0);
    }

    private static LocalDateTime leerFechaHora() {
        LocalDate dia = Consola.leerFecha("Día", LocalDate.now());
        return LocalDateTime.of(dia, Consola.leerHora("Hora (HH:mm, en punto o y media): "));
    }

    /** Menú generado con TipoConsulta.values(). */
    private static TipoConsulta elegirTipoConsulta() {
        TipoConsulta[] tipos = TipoConsulta.values();
        for (int i = 0; i < tipos.length; i++) {
            System.out.println("  " + (i + 1) + ". " + tipos[i]);
        }
        return tipos[Consola.leerEntero("Tipo de consulta: ", 1, tipos.length) - 1];
    }

    private static void pedirCita() {
        int idMascota = Consola.leerEntero("Id de la mascota: ", 1, Integer.MAX_VALUE);
        if (clinica.buscarMascota(idMascota) == null) {
            System.out.println("No existe ninguna mascota con ese id.");
            return;
        }
        TipoConsulta tipo = elegirTipoConsulta();
        LocalDateTime fechaHora = leerFechaHora();
        Cita cita = clinica.pedirCita(idMascota, fechaHora, tipo);

        if (cita == null && clinica.franjaOcupada(fechaHora)) {
            LocalDateTime libre = clinica.primeraFranjaLibre(fechaHora);
            if (libre == null) {
                System.out.println("Esa franja está ocupada y no hay huecos en las próximas 2 semanas.");
                return;
            }
            if (Consola.leerSiNo("Esa franja está ocupada. ¿Quieres la primera libre, el " + Fechas.formatear(libre) + "?")) {
                cita = clinica.pedirCita(idMascota, libre, tipo);
            }
        }
        if (cita == null) {
            System.out.println("No se ha dado la cita. Recuerda: no puede ser en el pasado, debe ser en punto o y media,");
            System.out.println("y salvo las urgencias, de lunes a viernes de 9:00 a 14:00 y de 16:00 a 20:00.");
        } else {
            System.out.println("Cita registrada: " + cita);
        }
    }

    private static void reprogramarCita() {
        int id = Consola.leerEntero("Id de la cita: ", 1, Integer.MAX_VALUE);
        System.out.println("Nueva fecha y hora:");
        Cita nueva = clinica.reprogramarCita(id, leerFechaHora());
        if (nueva == null) {
            System.out.println("No se ha reprogramado: la cita no existe, no está pendiente o la nueva fecha no es válida.");
        } else {
            System.out.println("Cita reprogramada: " + nueva);
        }
    }

    private static void atenderCita() {
        int id = Consola.leerEntero("Id de la cita: ", 1, Integer.MAX_VALUE);
        Cita cita = clinica.buscarCita(id);
        if (cita == null || !cita.estaPendiente()) {
            System.out.println("La cita no existe o no está pendiente.");
            return;
        }
        System.out.println(cita);
        String diagnostico = Consola.leerTexto("Diagnóstico: ");
        double peso = Consola.leerDecimal("Peso actual (kg): ", 0.01, 200);
        String vacuna = null;
        if (cita.getTipo() == TipoConsulta.VACUNACION) {
            vacuna = Consola.leerTexto("Vacuna administrada: ");
        }
        double importe = clinica.atenderCita(id, diagnostico, peso, vacuna);
        if (importe == -1) {
            System.out.println("No se ha podido atender la cita.");
        } else {
            System.out.printf("Cita atendida. Importe: %.2f €%n", importe);
        }
    }

    private static void citasDelDia() {
        LocalDate dia = Consola.leerFecha("Día", LocalDate.now());
        Cita[] citas = clinica.citasDelDia(dia);
        if (citas.length == 0) {
            System.out.println("No hay citas ese día.");
        }
        for (Cita cita : citas) {
            System.out.println(cita);
        }
    }

    // =================================================================== INFORMES

    private static void menuInformes() {
        int opcion;
        do {
            System.out.println();
            System.out.println("--- INFORMES ---");
            System.out.println("1. Facturación de un mes");
            System.out.println("2. Mascotas con vacunas caducadas");
            System.out.println("0. Volver");
            opcion = Consola.leerEntero("Opción: ", 0, 2);

            switch (opcion) {
                case 1 -> {
                    int anio = Consola.leerEntero("Año: ", 2000, 2100);
                    int mes = Consola.leerEntero("Mes (1-12): ", 1, 12);
                    System.out.println(clinica.facturacionMes(anio, mes));
                }
                case 2 -> vacunasCaducadas();
            }
        } while (opcion != 0);
    }

    private static void vacunasCaducadas() {
        LocalDate hoy = LocalDate.now();
        Mascota[] lista = clinica.mascotasConVacunasCaducadas(hoy);
        if (lista.length == 0) {
            System.out.println("Ninguna mascota tiene vacunas caducadas.");
        }
        for (Mascota mascota : lista) {
            System.out.println(mascota.getNombre() + " (" + mascota.getEspecie() + ") - "
                    + String.join(", ", mascota.vacunasCaducadas(hoy))
                    + " - avisar a " + mascota.getPropietario().getNombre()
                    + ", tel. " + mascota.getPropietario().getTelefono());
        }
    }
}
