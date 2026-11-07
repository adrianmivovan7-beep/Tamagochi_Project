package org.cuatrovientos.dam.psp.Tamagochi_Proyecto;

import java.util.Random;

public class Tamagotchi implements Runnable {

    private static final int SIN_ACCION = 0;
    private static final int COMER = 1;
    private static final int JUGAR = 2;
    private static final int LIMPIAR = 3;

    private final String identificador;
    private final Configuracion configuracion;
    private final Random random = new Random();

    private boolean vivo = true;
    private Estado estado = Estado.OCIOSO;
    private int suciedad = 0;
    private int accionPendiente = SIN_ACCION;
    private boolean destruccionSolicitada = false;

    private boolean respuestaRecibida = false;
    private int respuestaJuego;
    private int numero1;
    private int numero2;

    private long inicioVida;
    private long proximoAumentoSuciedad;
    private boolean avisoSuciedadEnviado = false;

    public Tamagotchi(String identificador, Configuracion configuracion) {
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("El identificador no puede estar vacío.");
        }
        this.identificador = identificador;
        this.configuracion = configuracion;
    }

    @Override
    public void run() {
        inicioVida = System.currentTimeMillis();
        proximoAumentoSuciedad = inicioVida
                + configuracion.intervaloSuciedadSegundos() * 1_000L;
        System.out.println("[" + identificador + "] ¡Estoy vivo!");

        try {
            while (isVivo()) {
                comprobarNecesidadesVitales();
                int accion = esperarAccion();

                if (accion != SIN_ACCION && isVivo()) {
                    ejecutarAccion(accion);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[" + identificador + "] Hilo interrumpido.");
        }
    }

    public synchronized boolean solicitarComida() {
        return solicitarAccion(COMER);
    }

    public synchronized boolean solicitarJuego(int num1, int num2) {
        if (num1 < 0 || num1 > 9 || num2 < 0 || num2 > 9 || num1 + num2 >= 10) {
            return false;
        }
        numero1 = num1;
        numero2 = num2;
        respuestaRecibida = false;
        return solicitarAccion(JUGAR);
    }

    public synchronized boolean solicitarLimpieza() {
        return solicitarAccion(LIMPIAR);
    }

    private boolean solicitarAccion(int accion) {
        if (!vivo || destruccionSolicitada || accionPendiente != SIN_ACCION
                || estado != Estado.OCIOSO) {
            return false;
        }
        accionPendiente = accion;
        notifyAll();
        return true;
    }

    private synchronized int esperarAccion() throws InterruptedException {
        while (vivo && accionPendiente == SIN_ACCION && !destruccionSolicitada) {
            wait(200);
            comprobarNecesidadesVitales();
        }

        if (destruccionSolicitada && estado == Estado.OCIOSO) {
            morir("El cuidador ha solicitado mi destrucción.");
            return SIN_ACCION;
        }

        int accion = accionPendiente;
        accionPendiente = SIN_ACCION;
        return accion;
    }

    private void ejecutarAccion(int accion) throws InterruptedException {
        switch (accion) {
            case COMER -> comer();
            case JUGAR -> jugar();
            case LIMPIAR -> limpiar();
            default -> System.out.println("[" + identificador + "] Acción desconocida.");
        }
    }

    private void comer() throws InterruptedException {
        cambiarEstado(Estado.COMIENDO);
        int duracion = random.nextInt(configuracion.comerMinimoSegundos(),
                configuracion.comerMaximoSegundos() + 1);
        System.out.println("[" + identificador + "] Empiezo a comer.");

        if (esperar(duracion * 1_000L) && isVivo()) {
            System.out.println("[" + identificador + "] He terminado de comer.");
        }
        quedarOcioso();
    }

    private void jugar() throws InterruptedException {
        cambiarEstado(Estado.JUGANDO);
        System.out.println("[" + identificador + "] Empiezo a jugar.");

        while (isVivo() && !hayDestruccionSolicitada()) {
            Integer respuesta = esperarRespuesta();
            if (respuesta == null) {
                break;
            }

            if (respuesta == numero1 + numero2) {
                System.out.println("[" + identificador + "] Respuesta correcta. He terminado de jugar.");
                break;
            }

            System.out.println("[" + identificador + "] Respuesta incorrecta. Sigo jugando.");
        }
        quedarOcioso();
    }

    private synchronized Integer esperarRespuesta() throws InterruptedException {
        while (vivo && !respuestaRecibida && !destruccionSolicitada) {
            wait(200);
            comprobarNecesidadesVitales();
        }
        if (!vivo || destruccionSolicitada || !respuestaRecibida) {
            return null;
        }
        respuestaRecibida = false;
        return respuestaJuego;
    }

    public synchronized boolean responderSuma(int respuesta) {
        if (!vivo || estado != Estado.JUGANDO || respuestaRecibida) {
            return false;
        }
        respuestaJuego = respuesta;
        respuestaRecibida = true;
        notifyAll();
        return true;
    }

    private void limpiar() throws InterruptedException {
        cambiarEstado(Estado.LIMPIANDO);
        System.out.println("[" + identificador + "] Empiezo a bañarme.");

        if (esperar(configuracion.tiempoLimpiezaSegundos() * 1_000L) && isVivo()) {
            synchronized (this) {
                suciedad = 0;
                avisoSuciedadEnviado = false;
                proximoAumentoSuciedad = System.currentTimeMillis()
                        + configuracion.intervaloSuciedadSegundos() * 1_000L;
            }
            System.out.println("[" + identificador + "] He terminado de bañarme. Suciedad: 0/10.");
        }
        quedarOcioso();
    }

    private boolean esperar(long milisegundos) throws InterruptedException {
        long fin = System.currentTimeMillis() + milisegundos;
        while (isVivo() && System.currentTimeMillis() < fin) {
            comprobarNecesidadesVitales();
            Thread.sleep(200);
        }
        return isVivo();
    }

    private synchronized void comprobarNecesidadesVitales() {
        if (!vivo || inicioVida == 0) {
            return;
        }

        long ahora = System.currentTimeMillis();
        if (ahora - inicioVida >= configuracion.tiempoVidaSegundos() * 1_000L) {
            morir("He cumplido mi tiempo máximo de vida.");
            return;
        }

        while (vivo && ahora >= proximoAumentoSuciedad) {
            suciedad++;
            proximoAumentoSuciedad += configuracion.intervaloSuciedadSegundos() * 1_000L;
            if (suciedad == 5 && !avisoSuciedadEnviado) {
                avisoSuciedadEnviado = true;
                System.out.println("[" + identificador + "] Estoy muy sucio (5/10).");
            }
            if (suciedad >= 10) {
                morir("He muerto por exceso de suciedad (10/10).");
            }
        }
    }

    private synchronized void cambiarEstado(Estado nuevoEstado) {
        if (vivo) {
            estado = nuevoEstado;
        }
    }

    private synchronized void quedarOcioso() {
        if (vivo) {
            estado = Estado.OCIOSO;
        }
        notifyAll();
    }

    public synchronized boolean solicitarDestruccion() {
        if (!vivo || destruccionSolicitada) {
            return false;
        }
        destruccionSolicitada = true;
        accionPendiente = SIN_ACCION;
        notifyAll();
        return true;
    }

    private synchronized boolean hayDestruccionSolicitada() {
        return destruccionSolicitada;
    }

    private synchronized void morir(String motivo) {
        if (!vivo) {
            return;
        }
        vivo = false;
        estado = Estado.MUERTO;
        accionPendiente = SIN_ACCION;
        System.out.println("[" + identificador + "] " + motivo);
        System.out.println("[" + identificador + "] Estado final: MUERTO.");
        notifyAll();
    }

    public String getIdentificador() {
        return identificador;
    }

    public synchronized boolean isVivo() {
        return vivo;
    }

    public synchronized Estado getEstado() {
        return estado;
    }

    public synchronized int getSuciedad() {
        return suciedad;
    }

    public synchronized boolean tieneAccionPendiente() {
        return accionPendiente != SIN_ACCION;
    }

    public synchronized long getEdadSegundos() {
        if (inicioVida == 0) {
            return 0;
        }
        return (System.currentTimeMillis() - inicioVida) / 1_000L;
    }
}
