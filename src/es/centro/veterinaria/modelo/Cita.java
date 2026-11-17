package es.centro.veterinaria.modelo;

import es.centro.veterinaria.util.Fechas;
import java.time.LocalDateTime;

/** Una cita de una mascota en la clínica. */
public class Cita {
    private static int ultimoId = 0;

    private final int id;
    private final Mascota mascota;
    private final LocalDateTime fechaHora;
    private final TipoConsulta tipo;
    private EstadoCita estado;
    private String diagnostico;   // null hasta que se atiende
    private double importe;       // 0 hasta que se atiende

    /**
     * Crea una cita pendiente. Solo registra los datos: es la clínica quien comprueba
     * antes que la mascota existe y que la fecha y la hora son válidas.
     */
    public Cita(Mascota mascota, LocalDateTime fechaHora, TipoConsulta tipo) {
        ultimoId++;
        this.id = ultimoId;
        this.mascota = mascota;
        this.fechaHora = fechaHora;
        this.tipo = (tipo != null) ? tipo : TipoConsulta.REVISION;
        this.estado = EstadoCita.PENDIENTE;
        this.diagnostico = null;
        this.importe = 0;
    }

    /**
     * Constructor de copia para reprogramar: crea una cita NUEVA, con otro identificador,
     * para la misma mascota y el mismo tipo de consulta, en estado PENDIENTE.
     */
    public Cita(Cita original, LocalDateTime nuevaFechaHora) {
        this(original.mascota, nuevaFechaHora, original.tipo);
    }

    /** Cancela la cita. Devuelve false si no estaba pendiente. */
    public boolean cancelar() {
        if (estado != EstadoCita.PENDIENTE) {
            return false;
        }
        estado = EstadoCita.CANCELADA;
        return true;
    }

    /**
     * Marca la cita como atendida con su diagnóstico y su importe. Devuelve false, sin
     * modificar nada, si no estaba pendiente, el diagnóstico está vacío o el importe es negativo.
     */
    public boolean atender(String diagnostico, double importe) {
        if (estado != EstadoCita.PENDIENTE || !esDiagnosticoValido(diagnostico) || importe < 0) {
            return false;
        }
        this.estado = EstadoCita.ATENDIDA;
        this.diagnostico = diagnostico.trim();
        this.importe = importe;
        return true;
    }

    private boolean esDiagnosticoValido(String diagnostico) {
        return diagnostico != null && !diagnostico.isBlank();
    }

    public boolean estaPendiente() {
        return estado == EstadoCita.PENDIENTE;
    }

    /** Una cita ocupa su franja salvo que esté cancelada. */
    public boolean ocupaFranja() {
        return estado != EstadoCita.CANCELADA;
    }

    public int getId() {
        return id;
    }

    public Mascota getMascota() {
        return mascota;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public TipoConsulta getTipo() {
        return tipo;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public double getImporte() {
        return importe;
    }

    @Override
    public String toString() {
        String texto = "Cita " + id + " | " + Fechas.formatear(fechaHora) + " | " + tipo + " | "
                + mascota.getNombre() + " (" + mascota.getEspecie() + ") | " + estado;
        if (estado == EstadoCita.ATENDIDA) {
            texto += String.format(" | %s | %.2f €", diagnostico, importe);
        }
        return texto;
    }
}
