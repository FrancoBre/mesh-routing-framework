# Mesh Routing Framework

A simulation framework for comparing network routing algorithms (Q-Routing, Dijkstra's Shortest-Path, etc.) across different topologies and network conditions.

![Java](https://img.shields.io/badge/java-21-orange.svg)
![Maven](https://img.shields.io/badge/maven-3.x-blue.svg)

## Overview

This framework enables experimentation with adaptive routing algorithms in mesh networks. It implements a tick-based discrete event simulation that models packet queuing, transmission delays, and network congestion.

The simulation conditions are based on the experiments from Boyan & Littman's 1994 paper *"Packet Routing in Dynamically Changing Networks: A Reinforcement Learning Approach"*:

- **Incremental network load** — increasing packet injection causes congestion on certain paths
- **Constant transmission time** — the focus is on queue processing delays at each node, not link latency
- **Same conditions for all algorithms** — enables fair, direct comparison

The architecture is modular and extensible: new routing algorithms, metrics, topologies, and traffic patterns can be added without modifying the core simulation engine.

**Key features:**
- Compare multiple routing algorithms side-by-side under identical conditions
- Configurable network topologies (irregular grids, hypercubes, LATA networks)
- Flexible traffic injection schedules (constant, ramping, oscillating, segmented)
- Extensible metrics and visualization system
- Tick-based simulation (no threads) for deterministic, reproducible experiments

## Quick Start

**Prerequisites:** Java 21+, Maven 3.x

```bash
# Clone the repository
git clone https://github.com/yourusername/meshroutingframework.git
cd meshroutingframework

# Build
mvn clean package

# Run simulation
java -cp target/meshroutingframework-1.0-SNAPSHOT.jar org.ungs.cli.Main

# Or use the Makefile
make run
```

### Pre-run Experiments

The `results/` folder contains experiments already run under various configurations, implementing the experiments from Boyan & Littman's paper. You can explore these to see example outputs (heatmaps, delivery time charts, comparisons) or use their configurations as templates for your own experiments.

## Supported Algorithms

| Algorithm | Description |
|-----------|-------------|
| **Q-Routing** | Reinforcement learning approach that learns optimal routes by updating Q-values based on delivery feedback |
| **Shortest-Path** | Dijkstra's algorithm - computes static shortest paths from each node to all destinations |
| **Full-Echo Q-Routing** | Q-Routing variant that requests Q-table info from neighbors *(planned)* |

The framework is designed for easy addition of new routing algorithms. Full-Echo Q-Routing is a candidate for testing the second experiment from Boyan & Littman's paper (link disconnection scenarios).

## Configuration

All simulation parameters are defined in `src/main/resources/application.properties`.

### How the Simulation Works

The simulation runs in discrete **ticks**. Each tick:

1. New packets are injected into the network (origin → destination pairs)
2. Each node forwards one packet from its queue to a neighbor
3. Packets hop node-to-node until they reach their destination
4. Metrics are recorded (delivery times, queue lengths, etc.)

```mermaid
flowchart LR
    subgraph sim[" "]
        direction LR
        T["TOPOLOGY<br/><i>Network structure</i>"]
        TR["TRAFFIC<br/><i>Injection rate</i>"]
        R["ROUTING<br/><i>Forwarding decisions</i>"]
        M["METRICS<br/><i>Performance data</i>"]
        
        T --> TR --> R --> M
    end
```

The framework runs the same simulation once per algorithm, allowing direct comparison under identical conditions.

---

### Lifecycle Hooks & Event System

To enable extensibility, the simulation implements **lifecycle hooks** and an **event sink** that components can use to react to simulation state changes.

**Lifecycle Hooks** (`SimulationObserver` interface):

| Hook | When it fires | Example use case |
|------|---------------|------------------|
| `onSimulationStart` | Once, before any algorithm runs | Initialize shared resources |
| `onAlgorithmStart` | Before each algorithm's run begins | Reset metric accumulators, clear routing tables |
| `onAlgorithmEnd` | After each algorithm's run completes | Save collected data series, render per-algorithm charts |
| `onSimulationEnd` | Once, after all algorithms finish | Generate comparison charts across algorithms |

**Event Sink** (`EventSink` interface):

Components can listen to fine-grained events emitted during simulation via `onEvent()`:

| Event | Description |
|-------|-------------|
| `TickEvent` | A simulation tick occurred |
| `PacketDepartedEvent` | A packet was injected into the network |
| `HopEvent` | A packet moved from one node to another |
| `PacketDeliveredEvent` | A packet reached its destination |
| `LoadLevelUpdatedEvent` | The injection load level changed |

**Context Objects:**

Components receive context objects to access simulation state and configuration:

| Context | Type | Description |
|---------|------|-------------|
| `SimulationConfigContext` | Static | Experiment configuration loaded from CLI/properties file. Contains topology, algorithms, termination policy, traffic settings, etc. Immutable — does not change during simulation. Used to build all components for the experiment. |
| `SimulationRuntimeContext` | Dynamic | Current simulation state. Contains current tick, RNG, network reference, current algorithm, in-flight packets, delivered packets, event sink. Updated every tick. Passed to hooks and event handlers for calculations. Reset between algorithm runs. |

This architecture allows metrics, visualizers, and routing applications to hook into any part of the simulation lifecycle without coupling to the core engine.

---

### Quick Example Configuration

```properties
# Topology: _6X6_GRID, _7_HYPERCUBE, _116_NODE_LATA
topology=_6X6_GRID

# Algorithms to compare (runs one simulation per algorithm)
algorithms=Q_ROUTING,SHORTEST_PATH

# Traffic injection schedule
injection-schedule=LOAD_LEVEL
injection-schedule.load-level.L=0.5

# Termination policy
termination-policy=FIXED_TICKS
termination-policy.fixed-ticks.total-ticks=50000

# Metrics to collect
metrics=AVG_DELIVERY_TIME,AVG_DELIVERY_TIME_VS_LOAD_LEVEL_VS_TICK

# Outputs
outputs=HEAT_MAP,CONFIG_DUMP
```

---

### 1. General Settings

| Property | Description | Default |
|----------|-------------|---------|
| `seed` | RNG seed for reproducibility | `42` |
| `max-active-packets` | Cap on concurrent in-flight packets (backpressure) | unlimited |
| `warmup-ticks` | Ticks to exclude from metrics (transient phase) | `0` |
| `experiment-name` | Output folder name | auto-generated |
| `output-folder` | Results directory | `./results/` |

**Note:** Set `warmup-ticks` to exclude Q-Routing's initial learning phase from measurements.

---

### 2. Topology

Defines the network structure: nodes and their connections.

| Topology | Description |
|----------|-------------|
| `_6X6_GRID` | 36-node irregular grid with 4-connectivity (from Boyan & Littman 1994) |
| `_7_HYPERCUBE` | 128-node 7-dimensional hypercube *(TODO)* |
| `_116_NODE_LATA` | 116-node LATA telephone network *(TODO)* |
| `FILE` | Load from external file *(TODO)* |

---

### 3. Termination Policy

Controls when the simulation stops.

| Policy | Condition |
|--------|-----------|
| `FIXED_TICKS` | After N ticks |
| `TOTAL_PACKETS_DELIVERED` | After N packets delivered |
| `COMPOSITE` | Combine policies with AND/OR logic |

**Examples:**

```properties
# Stop after 50,000 ticks
termination-policy=FIXED_TICKS
termination-policy.fixed-ticks.total-ticks=50000

# Stop after delivering 100,000 packets
termination-policy=TOTAL_PACKETS_DELIVERED
termination-policy.packets-delivered.total-packets=100000

# Stop when EITHER 50,000 ticks pass OR 100,000 packets delivered (whichever comes first)
termination-policy=COMPOSITE
termination-policy.composite.mode=OR
termination-policy.composite.policies=FIXED_TICKS,TOTAL_PACKETS_DELIVERED
```

---

### 4. Injection Schedule

Controls the packet injection rate over time.

**Load Level (L):** Average packets injected per tick. Implementation: `floor(L) + Bernoulli(frac(L))`.

| Schedule | Description | Use Case |
|----------|-------------|----------|
| `LOAD_LEVEL` | Constant L | Steady-state analysis |
| `TRIANGULAR_LOAD_LEVEL` | L oscillates min→max→min | Adaptation testing |
| `LINEAR_LOAD_LEVEL` | L ramps min→max | Stress testing |
| `SEGMENTWISE_LOAD_LEVEL` | Custom piecewise pattern | Complex scenarios |
| `PROB_PER_TICK` | Bernoulli(p) per tick | Low traffic |
| `GAP` | Batch every N ticks | Bursty traffic |
| `WINDOWED_LOAD` | Three fixed phases | Multi-phase experiments |
| `PLATEAU_RAMP_PLATEAU` | Low→ramp→high | Congestion onset |

#### Schedule Parameters

**LOAD_LEVEL** (constant load)
```properties
injection-schedule=LOAD_LEVEL
injection-schedule.load-level.L=0.5   # Average 0.5 packets per tick
```

**TRIANGULAR_LOAD_LEVEL** (oscillating)

```mermaid
xychart-beta
    title "Triangular Load Pattern"
    x-axis "Time (ticks)" [0, 2500, 5000, 7500, 10000]
    y-axis "Load Level" 0 --> 6
    line "Load" [0.5, 2.75, 5.0, 2.75, 0.5]
```
```properties
injection-schedule=TRIANGULAR_LOAD_LEVEL
injection-schedule.minL=0.5                          # Valley (minimum load)
injection-schedule.maxL=5.0                          # Peak (maximum load)
injection-schedule.load-level-change.period-ticks=10000  # Full up-then-down cycle
```

**LINEAR_LOAD_LEVEL** (ramp)

```mermaid
xychart-beta
    title "Linear Load Pattern"
    x-axis "Time (ticks)" [0, 2500, 5000, 7500, 10000]
    y-axis "Load Level" 0 --> 6
    line "Load" [0.5, 1.625, 2.75, 3.875, 5.0]
```
```properties
injection-schedule=LINEAR_LOAD_LEVEL
injection-schedule.minL=0.5                          # Starting load
injection-schedule.maxL=5.0                          # Ending load
injection-schedule.load-level-change.period-ticks=10000  # Time to reach max
```

**SEGMENTWISE_LOAD_LEVEL** (custom piecewise)

Example: `{{4000,1.5~5.5},{20000,5.5},{4000,5.5~1.8},{22000,1.8}}`

```mermaid
xychart-beta
    title "Segmentwise Load Pattern"
    x-axis "Time (ticks)" [0, 4000, 24000, 28000, 50000]
    y-axis "Load Level" 0 --> 6
    line "Load" [1.5, 5.5, 5.5, 1.8, 1.8]
```
```properties
injection-schedule=SEGMENTWISE_LOAD_LEVEL
# Format: {{duration,load_or_ramp},...}
# - {4000,1.5~5.5} = 4000 ticks ramping from 1.5 to 5.5
# - {20000,5.5} = 20000 ticks at constant 5.5
injection-schedule.segmentwise.segments={{4000,1.5~5.5},{20000,5.5},{4000,5.5~1.8},{22000,1.8}}
```

**Other schedules:**

```properties
# PROB_PER_TICK: Bernoulli(0.3) per tick
injection-schedule=PROB_PER_TICK
injection-schedule.prob-per-tick.p=0.3

# GAP: batch of 2 every 5 ticks
injection-schedule=GAP
injection-schedule.gap.inject-every-n-ticks=5
injection-schedule.gap.batch-size=2

# WINDOWED_LOAD: phase A → B → C
injection-schedule=WINDOWED_LOAD
injection-schedule.windowed-load.phase-a.ticks=200
injection-schedule.windowed-load.phase-a.batch=2
injection-schedule.windowed-load.phase-b.ticks=800
injection-schedule.windowed-load.phase-b.batch=10
injection-schedule.windowed-load.phase-c.batch=0

# PLATEAU_RAMP_PLATEAU
injection-schedule=PLATEAU_RAMP_PLATEAU
injection-schedule.plateau-ramp-plateau.p1.ticks=200
injection-schedule.plateau-ramp-plateau.p1.inject-every-n-ticks=5
injection-schedule.plateau-ramp-plateau.p1.batch-size=2
injection-schedule.plateau-ramp-plateau.ramp.ticks=600
injection-schedule.plateau-ramp-plateau.ramp.start-batch-size=2
injection-schedule.plateau-ramp-plateau.ramp.max-batch-size=10
injection-schedule.plateau-ramp-plateau.p3.batch-size=10

# FIXED_LOAD_STEP: cycle through batch sizes
injection-schedule=FIXED_LOAD_STEP
injection-schedule.fixed-load-step.step-ticks=200
injection-schedule.fixed-load-step.batch-sizes=1,2,3,4,5,6,7
```

---

### 5. Pair Selection

Determines how origin/destination pairs are chosen for injected packets.

```mermaid
flowchart LR
    subgraph random["RANDOM: Any node → Any other node"]
        direction LR
        r1((A)) --> r2((B))
        r3((C)) --> r4((D))
        r5((E)) --> r6((F))
    end
    
    subgraph osc["OSCILLATING: LEFT ↔ RIGHT groups"]
        direction LR
        subgraph left[LEFT]
            l1((○))
            l2((○))
        end
        subgraph right[RIGHT]
            rr1((○))
            rr2((○))
        end
        l1 <--> rr1
        l2 <--> rr2
    end
```

| Strategy | Description |
|----------|-------------|
| `RANDOM` | Uniform random selection from all nodes |
| `RANDOM_IN_GROUPS` | Origin from group A, destination from group B |
| `OSCILLATING_BETWEEN_GROUPS` | Alternates A→B and B→A every N ticks |

```properties
pair-selection=RANDOM

# Or: oscillating between groups
pair-selection=OSCILLATING_BETWEEN_GROUPS
pair-selection.oscillating.period-ticks=200        # Switch direction every 200 ticks
pair-selection.oscillating.groups.a=LEFT           # First group name
pair-selection.oscillating.groups.b=RIGHT          # Second group name
```

**Constraints:**
```properties
pair-selection.constraints.disallow-self=true      # origin ≠ destination
pair-selection.constraints.disallow-neighbor=true  # no trivial 1-hop routes
```

---

### 6. Node Groups

Named node subsets used by group-based pair selection strategies.

```mermaid
block-beta
    columns 2
    
    block:topleft["TOP-LEFT\n(nodes 0-2, 6-8, 12-14)"]:1
        tl["0  1  2\n6  7  8\n12 13 14"]
    end
    
    block:topright["TOP-RIGHT\n(nodes 3-5, 9-11, 15-17)"]:1
        tr["3  4  5\n9 10 11\n15 16 17"]
    end
    
    block:botleft["BOTTOM-LEFT\n(nodes 18-20, 24-26, 30-32)"]:1
        bl["18 19 20\n24 25 26\n30 31 32"]
    end
    
    block:botright["BOTTOM-RIGHT\n(nodes 21-23, 27-29, 33-35)"]:1
        br["21 22 23\n27 28 29\n33 34 35"]
    end
    
    space:2
    
    left1["⬅️ LEFT = columns 0-2"]
    right1["➡️ RIGHT = columns 3-5"]
    
    space:2
    
    top1["⬆️ TOP = rows 0-2"]
    bot1["⬇️ BOTTOM = rows 3-5"]
```

```properties
# Define which groups exist
groups=TOP,BOTTOM,LEFT,RIGHT

# Define which nodes belong to each group
groups.TOP.nodes=0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17
groups.BOTTOM.nodes=18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35
groups.LEFT.nodes=0,1,2,6,7,8,12,13,14,18,19,20,24,25,26,30,31,32
groups.RIGHT.nodes=3,4,5,9,10,11,15,16,17,21,22,23,27,28,29,33,34,35
```

---

### 7. Network Dynamics

Topology changes during simulation (link/node failures, mobility).

| Type | Description |
|------|-------------|
| `NONE` | Static topology (default) |
| `SCHEDULED_LINK_FAILURES` | Disconnect specified links at a given tick |
| `NODE_FAILURES` | Stochastic node failures *(WIP)* |
| `MOBILITY` | Dynamic node positions *(TODO)* |

**Scheduled Link Failures** (replicates Boyan & Littman 1994):

```mermaid
flowchart TB
    subgraph before["Before tick 6000"]
        direction TB
        b8((8)) --- b14((14)) --- b20((20))
        b14 --- b15((15))
    end
    
    subgraph after["After tick 6000 (links cut!)"]
        direction TB
        a8((8)) -.-x a14((14)) -.-x a20((20))
        a14 -.-x a15((15))
    end
    
    before -.->|"tick 6000"| after
```

```properties
network-dynamics=SCHEDULED_LINK_FAILURES
network-dynamics.scheduled-link-failures.disconnect-at-tick=6000
network-dynamics.scheduled-link-failures.reconnect-at-tick=0      # 0 = never reconnect
network-dynamics.scheduled-link-failures.links=14-15,8-14,14-20   # Links to cut (node-node pairs)
```

---

### 8. Metrics

Performance measurements collected during simulation.

| Metric | Description |
|--------|-------------|
| `AVG_DELIVERY_TIME` | Mean packet delivery time (ticks) |
| `AVG_DELIVERY_TIME_VS_LOAD_LEVEL` | Delivery time as function of load |
| `AVG_DELIVERY_TIME_VS_LOAD_LEVEL_VS_TICK` | Time series of delivery time and load |
| `P95_DELIVERY_TIME` | 95th percentile latency *(TODO)* |
| `PACKETS_IN_FLIGHT` | Concurrent packet count *(TODO)* |
| `QUEUE_LENGTH` | Per-node queue depth *(TODO)* |

```properties
metrics=AVG_DELIVERY_TIME,AVG_DELIVERY_TIME_VS_LOAD_LEVEL_VS_TICK
```

---

### 9. Outputs

Generated visualizations and exports.

| Output | Description |
|--------|-------------|
| `HEAT_MAP` | Link utilization heatmap |
| `GIF_ROUTE` | Animated routing visualization |
| `ROUTE_FRAMES` | Individual PNG frames |
| `CONFIG_DUMP` | Configuration snapshot |

```properties
outputs=HEAT_MAP,CONFIG_DUMP

# How often to sample data for visualizations (every 10 ticks)
output.sample-every-ticks=10

# Optional: Only include hops from tick 30000 onwards in heatmap
output.heatmap.from-tick=30000
output.heatmap.to-tick=            # Empty = no upper limit
```

---

### Configuration Summary

```mermaid
flowchart TD
    subgraph config["SIMULATION CONFIGURATION"]
        T["<b>TOPOLOGY</b><br/>_6X6_GRID"]
        A["<b>ALGORITHMS</b><br/>Q_ROUTING, SHORTEST_PATH"]
        I["<b>INJECTION</b><br/>LOAD_LEVEL (L=2.0)"]
        P["<b>PAIR SELECTION</b><br/>RANDOM"]
        D["<b>DYNAMICS</b><br/>NONE"]
        S["<b>TERMINATION</b><br/>FIXED_TICKS (50000)"]
        M["<b>METRICS</b><br/>AVG_DELIVERY_TIME"]
        O["<b>OUTPUTS</b><br/>HEAT_MAP, CONFIG_DUMP"]
        
        T --> A --> I --> P --> D --> S --> M --> O
    end
```

**Complete Example:**

```properties
seed=42
topology=_6X6_GRID
algorithms=Q_ROUTING,SHORTEST_PATH

injection-schedule=LOAD_LEVEL
injection-schedule.load-level.L=2.0

pair-selection=RANDOM
network-dynamics=NONE

termination-policy=FIXED_TICKS
termination-policy.fixed-ticks.total-ticks=20000
warmup-ticks=5000

metrics=AVG_DELIVERY_TIME
outputs=HEAT_MAP,CONFIG_DUMP
```

## Architecture

```mermaid
flowchart TD
    Main --> Simulation
    Simulation --> Network
    Simulation --> Registry
    Network --> Nodes
    Registry --> Metrics
    Nodes --> RoutingApplication
    Metrics --> GraphGenerator
    
    RoutingApplication -.- QRouting[QRoutingApplication]
    RoutingApplication -.- ShortestPath[ShortestPathApplication]
```

### Core Components

- **SimulationEngine** — Manages the tick-based event loop, initializes network topology, and coordinates packet injection. The tick system processes events (packet send/receive, queue updates) in discrete steps, avoiding thread synchronization issues.
- **Network** — Represents the topology: nodes and links. Provides methods for sending packets between nodes.
- **Node** — Maintains a packet queue and delegates routing decisions to its `RoutingApplication`.
- **RoutingApplication** — Abstract base class for routing algorithms. Each node has one application instance that decides the next hop for each packet.
- **Registry** — Singleton that logs all simulation events (sends, receives, delivery times, queue states). Called from relevant parts of the code to record events.
- **Metrics** — Computes statistics from Registry data (avg delivery time, throughput, etc.). Metrics are configurable to allow future extensions.
- **GraphGenerator** — Creates comparative visualizations (heatmaps, delivery time charts). Uses XChart library.

### Algorithm Implementations

**QRoutingApplication:**
- Maintains a Q-table to make routing decisions
- Updates Q-values based on feedback from packet deliveries (reinforcement learning)
- Selects the next hop based on learned Q-values

**ShortestPathApplication:**
- Computes shortest paths from the node to all destinations using Dijkstra's algorithm
- Selects the next hop based on precomputed routes
- Recomputes routes when topology changes (implements `TopologyListener`)

## Extending the Framework

All pluggable components follow a unified **Factory → Preset → Type** pattern:

```mermaid
flowchart LR
    Type["<b>Type</b><br/>(enum value)"] --> Preset["<b>Preset</b><br/>(creates instances)"] --> Factory["<b>Factory</b><br/>(registry)"]
```

| Component | Type Enum | Preset Interface | Factory |
|-----------|-----------|------------------|---------|
| Routing Algorithm | `AlgorithmType` | `RoutingApplicationPreset` | `RoutingApplicationFactory` |
| Metric | `MetricType` | `MetricPreset<T>` | `ObserverHubFactory` |
| Output | `OutputType` | `OutputPreset` | `ObserverHubFactory` |
| Injection Schedule | `InjectionScheduleType` | `InjectionSchedulePreset` | `InjectionScheduleFactory` |
| Pair Selector | `PairSelectionType` | `PairSelectorPreset` | `PairSelectorFactory` |
| Termination Policy | `TerminationPolicyType` | `TerminationPolicyPreset` | `TerminationPolicyFactory` |
| Network Dynamics | `NetworkDynamicsType` | `NetworkDynamicsPreset` | `NetworkDynamicsFactory` |
| Topology | `TopologyType` | `TopologyPreset` | `TopologyFactory` |

---

### Adding a New Routing Algorithm

1. Add a new value to `AlgorithmType` enum
2. Create your `RoutingApplication` implementation
3. Create a `RoutingApplicationPreset` that instantiates it
4. Register the preset in `RoutingApplicationFactory`

**Step 1: Add enum value** (`AlgorithmType.java`)
```java
public enum AlgorithmType {
    Q_ROUTING,
    SHORTEST_PATH,
    MY_CUSTOM  // ← add this
}
```

**Step 2: Create the application** (`MyCustomApplication.java`)
```java
public class MyCustomApplication extends RoutingApplication {

    public MyCustomApplication(Node node) {
        super(node);
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.MY_CUSTOM;
    }

    @Override
    public void onTick(SimulationRuntimeContext ctx) {
        getNextPacket().ifPresent(packet -> {
            Node.Id nextHop = decideNextHop(packet);
            getNode().send(packet, nextHop);
        });
    }
}
```

**Step 3: Create the preset** (`MyCustomApplicationPreset.java`)
```java
public class MyCustomApplicationPreset implements RoutingApplicationPreset {

    @Override
    public AlgorithmType type() {
        return AlgorithmType.MY_CUSTOM;
    }

    @Override
    public RoutingApplication createRoutingApplication(Node node) {
        return new MyCustomApplication(node);
    }
}
```

**Step 4: Register in factory** (`RoutingApplicationFactory.java`)
```java
static {
    register(new QRoutingApplicationPreset());
    register(new ShortestPathApplicationPreset());
    register(new MyCustomApplicationPreset());  // ← add this
}
```

---

### Adding a New Metric

1. Add a new value to `MetricType` enum
2. Create your `Metric<T>` implementation (collects data via events)
3. Create renderer(s) for per-algorithm and comparison charts
4. Create a `MetricPreset<T>` that bundles them together
5. Register the preset in `ObserverHubFactory`

**Step 1: Add enum value** (`MetricType.java`)
```java
public enum MetricType {
    AVG_DELIVERY_TIME,
    AVG_DELIVERY_TIME_VS_LOAD_LEVEL,
    MY_CUSTOM_METRIC  // ← add this
}
```

**Step 2: Create the metric** (`MyCustomMetric.java`)
```java
public class MyCustomMetric implements Metric<MyDataType> {

    @Override
    public void reset() {
        // Called on algorithm start - clear accumulated data
    }

    @Override
    public void onEvent(SimulationEvent e, SimulationRuntimeContext ctx) {
        // Called for each event - collect data
        if (e instanceof PacketDeliveredEvent delivered) {
            // record delivery time, etc.
        }
    }

    @Override
    public MyDataType snapshot() {
        // Called on algorithm end - return collected data
        return collectedData;
    }
}
```

**Step 3: Create the preset** (`MyCustomMetricPreset.java`)
```java
public class MyCustomMetricPreset implements MetricPreset<MyDataType> {

    @Override
    public MetricType type() {
        return MetricType.MY_CUSTOM_METRIC;
    }

    @Override
    public MetricBundle<MyDataType> createBundle(SimulationConfigContext cfg, Network network) {
        var metric = new MyCustomMetric();
        var perAlgoRenderer = new MyCustomRenderer();
        var comparisonRenderer = new MyCustomComparisonRenderer();
        
        return new MetricBundle<>(
            MetricType.MY_CUSTOM_METRIC.name(),
            metric,
            perAlgoRenderer,
            comparisonRenderer
        );
    }
}
```

**Step 4: Register in factory** (`ObserverHubFactory.java`)
```java
static {
    registerMetric(new AvgDeliveryTimePreset());
    registerMetric(new MyCustomMetricPreset());  // ← add this
}
```

## References

Based on the experiments from:
> Boyan, J. A., & Littman, M. L. (1994). *Packet Routing in Dynamically Changing Networks: A Reinforcement Learning Approach*
