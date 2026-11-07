# Proyecto Tamagotchi

Proyecto Java 21 con Maven. La clase `Cuidador` contiene el `main`, crea los Tamagotchis indicados en `src/main/resources/configuracion.properties` y muestra el menú para interactuar con ellos.

## Ejecución

Desde la carpeta del proyecto:

```bash
mvn clean compile
mvn exec:java
```

## Funcionamiento

- Cada Tamagotchi tiene su propio hilo y un identificador.
- El cuidador es quien envía las órdenes y consulta los estados.
- Los Tamagotchis no llaman al cuidador ni le envían datos mediante interfaces o métodos de aviso.
- Los mensajes de inicio y final de las acciones se muestran directamente por consola desde el hilo del Tamagotchi; son solo mensajes informativos.
- Para jugar, el cuidador genera la suma, la muestra por consola y envía los números al Tamagotchi. Después le envía la respuesta introducida.
- Solo se guarda una acción pendiente por Tamagotchi. Se utiliza un número entero para identificarla; no se utilizan colas.
- La comunicación entre el cuidador y cada hilo se coordina con `synchronized`, `wait()` y `notifyAll()`.
- No se utilizan `volatile` ni `CompletableFuture`.
- La suciedad aumenta cada intervalo configurado, avisa al llegar a 5 y provoca la muerte al llegar a 10.
- La vida termina al cumplirse el tiempo configurado.
- Una orden de destrucción se atiende cuando el Tamagotchi queda ocioso.

## Configuración

Editar `src/main/resources/configuracion.properties` para cambiar la cantidad de Tamagotchis y los tiempos de vida, suciedad, limpieza y comida.
