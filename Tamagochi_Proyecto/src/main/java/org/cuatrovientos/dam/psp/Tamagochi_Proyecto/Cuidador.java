package org.cuatrovientos.dam.psp.Tamagochi_Proyecto;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class Cuidador {

    private final Configuracion configuracion;
    private final Map<String, Tamagotchi> tamagotchis = new HashMap<>();
    private final Map<String, Thread> hilos = new HashMap<>();
    private final Map<String, int[]> sumas = new HashMap<>();
    private final Random random = new Random();

    public Cuidador(Configuracion configuracion) {
        this.configuracion = configuracion;
    }

    public static void main(String[] args) {
        try {
            Cuidador cuidador = new Cuidador(Configuracion.cargar());
            cuidador.arrancarTamagotchis();

            try (Scanner scanner = new Scanner(System.in)) {
                cuidador.mostrarMenu(scanner);
            } finally {
                cuidador.cerrar();
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("[ERROR] No se ha podido iniciar el cuidador: " + e.getMessage());
        }
    }

    private void arrancarTamagotchis() {
        for (int i = 1; i <= configuracion.numeroTamagotchis(); i++) {
            String id = "Tama-" + i;
            Tamagotchi tamagotchi = new Tamagotchi(id, configuracion);
            Thread hilo = new Thread(tamagotchi, id);
            tamagotchis.put(id, tamagotchi);
            hilos.put(id, hilo);
            hilo.start();
            System.out.println("[CUIDADOR] He puesto las pilas a " + id + ".");
        }
    }

    private void mostrarMenu(Scanner scanner) {
        boolean continuar = true;
        while (continuar) {
            System.out.println("""

                    ========== CUIDADOR DE TAMAGOTCHIS ==========
                    1. Consultar estado de todos
                    2. Dar de comer
                    3. Jugar
                    4. Limpiar
                    5. Responder a una suma
                    6. Destruir un Tamagotchi
                    0. Salir
                    =============================================
                    """);
            System.out.print("Selecciona una opción: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> consultarEstados();
                case "2" -> darDeComer(scanner);
                case "3" -> mandarJugar(scanner);
                case "4" -> mandarLimpiar(scanner);
                case "5" -> responderSuma(scanner);
                case "6" -> destruirTamagotchi(scanner);
                case "0" -> continuar = false;
                default -> System.out.println("[CUIDADOR] Opción no válida.");
            }
        }
    }

    private void consultarEstados() {
        System.out.println("\n---------------- ESTADO DE LOS TAMAGOTCHIS ----------------");
        for (Tamagotchi t : tamagotchis.values()) {
            System.out.printf("%s | estado: %s | vivo: %s | suciedad: %d/10 | edad: %d s | acción pendiente: %s%n",
                    t.getIdentificador(), t.getEstado(), t.isVivo(), t.getSuciedad(),
                    t.getEdadSegundos(), t.tieneAccionPendiente() ? "sí" : "no");
        }
    }

    private void darDeComer(Scanner scanner) {
        Tamagotchi t = seleccionarTamagotchi(scanner);
        if (t != null && !t.solicitarComida()) {
            System.out.println("[CUIDADOR] No se ha podido enviar la orden.");
        }
    }

    private void mandarJugar(Scanner scanner) {
        Tamagotchi t = seleccionarTamagotchi(scanner);
        if (t == null) {
            return;
        }

        int n1 = random.nextInt(10);
        int n2 = random.nextInt(10 - n1);
        if (!t.solicitarJuego(n1, n2)) {
            System.out.println("[CUIDADOR] No se ha podido iniciar el juego.");
            return;
        }

        sumas.put(t.getIdentificador(), new int[]{n1, n2});
        System.out.println("[CUIDADOR] Suma para " + t.getIdentificador() + ": ¿cuánto es "
                + n1 + " + " + n2 + "?");
        System.out.println("[CUIDADOR] Usa la opción 5 para responder.");
    }

    private void mandarLimpiar(Scanner scanner) {
        Tamagotchi t = seleccionarTamagotchi(scanner);
        if (t != null && !t.solicitarLimpieza()) {
            System.out.println("[CUIDADOR] No se ha podido enviar la orden.");
        }
    }

    private void responderSuma(Scanner scanner) {
        System.out.print("Identificador del Tamagotchi: ");
        String id = scanner.nextLine().trim();
        Tamagotchi t = buscar(id);
        int[] suma = sumas.get(id);

        if (t == null || suma == null) {
            System.out.println("[CUIDADOR] Ese Tamagotchi no tiene una partida iniciada.");
            return;
        }

        System.out.print("Resultado de la suma: ");
        try {
            int respuesta = Integer.parseInt(scanner.nextLine().trim());
            if (!t.responderSuma(respuesta)) {
                System.out.println("[CUIDADOR] No se ha podido enviar la respuesta.");
            } else if (respuesta != suma[0] + suma[1]) {
                System.out.println("[CUIDADOR] Resultado incorrecto. Puedes volver a intentarlo.");
            } else {
                System.out.println("[CUIDADOR] Resultado correcto.");
                sumas.remove(id);
            }
        } catch (NumberFormatException e) {
            System.out.println("[CUIDADOR] Debes introducir un número entero.");
        }
    }

    private void destruirTamagotchi(Scanner scanner) {
        Tamagotchi t = seleccionarTamagotchi(scanner);
        if (t != null && !t.solicitarDestruccion()) {
            System.out.println("[CUIDADOR] No se ha podido solicitar la destrucción.");
        }
    }

    private Tamagotchi seleccionarTamagotchi(Scanner scanner) {
        System.out.print("Identificador (por ejemplo, Tama-1): ");
        Tamagotchi t = buscar(scanner.nextLine().trim());
        if (t == null) {
            System.out.println("[CUIDADOR] No existe ese Tamagotchi.");
        }
        return t;
    }

    private Tamagotchi buscar(String id) {
        return id == null || id.isBlank() ? null : tamagotchis.get(id);
    }

    private void cerrar() {
        System.out.println("[CUIDADOR] Cerrando la aplicación...");
        for (Tamagotchi t : tamagotchis.values()) {
            if (t.isVivo()) {
                t.solicitarDestruccion();
            }
        }
        for (Thread hilo : hilos.values()) {
            try {
                hilo.join(2_000);
                if (hilo.isAlive()) {
                    hilo.interrupt();
                    hilo.join(1_000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[CUIDADOR] Se interrumpió la espera de los hilos.");
                return;
            }
        }
        System.out.println("[CUIDADOR] Todos los Tamagotchis han terminado.");
    }
}
