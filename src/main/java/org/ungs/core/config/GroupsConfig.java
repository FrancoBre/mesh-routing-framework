package org.ungs.core.config;

import java.util.*;
import org.ungs.cli.SimulationConfigLoader;

public record GroupsConfig(Map<String, List<Integer>> groups // name -> node ids
    ) {

  public static GroupsConfig fromLoader(SimulationConfigLoader l) {
    List<String> names =
        (l.groups() == null)
            ? List.of()
            : l.groups().stream().map(String::trim).filter(s -> !s.isEmpty()).toList();

    Map<String, List<Integer>> map = new LinkedHashMap<>();

    for (String name : names) {
      String key = "groups." + name + ".nodes";
      String raw = l.getProperty(key);
      if (raw == null || raw.isBlank()) {
        throw new IllegalArgumentException(
            key + " must be set when group '" + name + "' is listed in groups");
      }
      map.put(name, SimulationConfigContext.parseIntCsv(raw));
    }

    return new GroupsConfig(Collections.unmodifiableMap(map));
  }

  public boolean isEmpty() {
    return groups.isEmpty();
  }

  public List<Integer> getOrThrow(String name) {
    List<Integer> ids = groups.get(name);
    if (ids == null) throw new IllegalArgumentException("Unknown group: " + name);
    return ids;
  }
}
