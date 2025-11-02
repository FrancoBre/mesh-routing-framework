# Especificación General

## Objetivo del framework
Permitir realizar experimentos de red para comparar algoritmos como Q-Routing y algoritmos shortest-path en diferentes topologías y condiciones de red.

## Condiciones de red
Las condiciones de red serían, en principio aquellas expuestas en el primer experimento del paper "Q-Routing: A Reinforcement Learning Approach to Adaptive Routing in Dynamic Networks" de Boyan y Littman, es decir:

- Carga de red incremental (cantidad creciente de paquetes queriendo ser entregados al mismo tiempo, lo que provoca que algunos caminos se congestionen).
- El tiempo de transmisión entre nodos es un tiempo constante de unidades enteras, ya que el foco del algoritmo está en el tiempo incremental de procesamiento de los paquetes en las colas de cada nodo, que aumenta junto con la carga de red.

El segundo experimento del paper, que incluye desconexión manual de enlaces, y envío de paquetes "pendular" en la grilla, no será implementado en esta primera versión del framework, pero la arquitectura del código debe permitir su inclusión en el futuro.

## Métricas
La métrica a medir sería principalmente el tiempo promedio de entrega de paquetes, con la posibilidad de extender a otras métricas como el throughput o la cantidad de paquetes entregados en un tiempo determinado. Estas métricas van a ser configurables para permitir la inclusión de nuevas métricas en el futuro.

Con estas métricas se van a generar gráficos comparativos entre los diferentes algoritmos de routing bajo las mismas condiciones de red y topologías. La cantidad y tipo de gráficos también van a ser escalable y configurable.

## Algoritmos soportados
El framework debe permitir la inclusión de diferentes algoritmos de routing. En principio, se van a implementar los siguientes algoritmos:
- Q-Routing
- Shortest-Path (Dijkstra)

Con estos dos algoritmos se podrán realizar comparaciones iniciales. El framework debe permitir la inclusión de nuevos algoritmos de routing en el futuro, por lo que la arquitectura del código debe ser modular y extensible.

Un candidato, con el que se podría probar el segundo experimento del paper, es la variante full-echo de Q-Routing, que incluye la solicitud de información de las tablas Q de los nodos vecinos.

# Especificación de Clases

## Main
Parsea los parámetros de entrada, que en principio van a ser:
- Topología de red (6x6 irregular grid, 7 hypercube, 116-node LATA telephone network, etc).
- Algoritmos de routing a comparar (Q-Routing, Shortest-Path, etc).
- Cantidad de paquetes a enviar.
- Tiempo entre cada paquete.
- Métricas a medir.
- Generación de gráficos (sí/no, cuáles).

Estos parámetros se pueden pasar por línea de comandos o por un archivo de configuración, y se van a encapsular en un record SimulationConfig para facilitar su manejo.

## Simulation
- Inicializa la topología de red, los nodos y los enlaces según la configuración recibida.
- Crea los paquetes y los envía a la red según los parámetros de tiempo especificados, por cada algoritmo de routing a comparar.
- Implementa el sistema de ticks para simular el paso del tiempo en la red, procesando los eventos en cada tick (envío y recepción de paquetes, actualización de colas, etc). Esto nos previene de usar hilos y facilita la simulación.

## Registry
- Lleva un registro de todos los eventos que ocurren en la simulación (envío y recepción de paquetes, tiempos de entrega, estados de las colas, etc).
- Proporciona métodos para consultar estos datos y calcular las métricas solicitadas.
- Se llama desde partes relevantes del código para registrar eventos.
- Implementa un singleton para asegurar que todas las partes del código acceden al mismo registro.

## Metrics
- Define una interfaz para las métricas a medir (tiempo promedio de entrega, throughput, etc).
- Implementa las métricas solicitadas en la configuración.
- Proporciona métodos para calcular y devolver los resultados de las métricas basándose en los datos del Registry.

## GraphGenerator
- Toma los resultados de las métricas y genera gráficos comparativos.
- Utiliza una biblioteca de gráficos para crear visualizaciones claras y comparativas entre los algoritmos

## Network
- Representa la topología de red, incluyendo nodos y enlaces.
- Proporciona métodos para enviar paquetes entre nodos.

## Node
- Representa un nodo en la red.
- Mantiene una cola de paquetes a procesar.
- Tiene una aplicación que implementa el algoritmo de routing asignado (Q-Routing, Shortest-Path, etc).
- Proporciona interfaces para recibir paquetes, procesarlos y enviarlos al siguiente nodo, que van a ser implementadas por las aplicaciones de routing.

## Application
- Define una interfaz para las aplicaciones de routing.
- Implementa los métodos necesarios para procesar paquetes y decidir el siguiente salto basado en el algoritmo de routing.

## QRoutingApplication
- Implementa el algoritmo Q-Routing.
- Mantiene una tabla Q para tomar decisiones de routing.
- Actualiza la tabla Q basándose en las recompensas recibidas por la entrega de paquetes
- Implementa la lógica para seleccionar el siguiente salto basándose en la tabla Q.

## ShortestPathApplication
- Implementa el algoritmo Shortest-Path (Dijkstra).
- Calcula las rutas más cortas desde el nodo a todos los demás nodos en la red.
- Selecciona el siguiente salto basándose en las rutas calculadas.

# Cosas para extender a futuro
- Implementar más algoritmos de routing.
- Incluir más condiciones de red, como enlaces que se desconectan o tiempos de transmisión variables.
- Agregar más métricas para evaluar el rendimiento de los algoritmos.
- Agregar estrategias de envío de paquetes incrementales (gaps variables) o basadas en patrones de tráfico específicos (origen y destino variables).
- Agregar cálculo de latencia de envío entre nodos basados en distancias (habría que agregar time in flight a los paquetes, ya no sería 1 por tick).
- Agregar variaciones en la posición de los nodos en la topología (por ejemplo, nodos móviles).
- Agregar un tiempo máximo o hops máximos para la entrega de paquetes, para evitar que queden "perdidos" en la red indefinidamente.
