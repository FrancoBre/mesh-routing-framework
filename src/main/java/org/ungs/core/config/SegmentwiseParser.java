package org.ungs.core.config;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
final class SegmentwiseParser {

  // spec example:
  // {{3000,0.0~3.5},{3000,3.5},{3000,3.5~1.2},{3000,1.2}}
  public static List<InjectionScheduleConfig.SegmentwiseLoadLevel.Segment> parse(String spec) {
    if (spec == null) throw new IllegalArgumentException("Segmentwise segments spec is null");

    String s = spec.trim();
    if (s.isEmpty()) return List.of();

    // strip outer braces if present
    if (s.startsWith("{") && s.endsWith("}")) {
      s = s.substring(1, s.length() - 1).trim();
    }

    // now s looks like: {3000,0.0~3.5},{3000,3.5},...
    List<String> parts = splitTopLevelSegments(s);

    List<InjectionScheduleConfig.SegmentwiseLoadLevel.Segment> out = new ArrayList<>();
    for (String part : parts) {
      String p = part.trim();
      if (p.startsWith("{") && p.endsWith("}")) {
        p = p.substring(1, p.length() - 1).trim();
      }
      if (p.isEmpty()) continue;

      // "ticks,value" where value is either "L" or "from~to"
      String[] fields = p.split(",", 2);
      if (fields.length != 2) {
        throw new IllegalArgumentException(
            "Invalid segment: '" + part + "'. Expected '{ticks,value}'.");
      }

      long ticks = Long.parseLong(fields[0].trim());
      String value = fields[1].trim();

      if (value.contains("~")) {
        String[] lr = value.split("~", 2);
        double fromL = Double.parseDouble(lr[0].trim());
        double toL = Double.parseDouble(lr[1].trim());
        out.add(new InjectionScheduleConfig.SegmentwiseLoadLevel.Ramp(ticks, fromL, toL));
      } else {
        double L = Double.parseDouble(value);
        out.add(new InjectionScheduleConfig.SegmentwiseLoadLevel.Plateau(ticks, L));
      }
    }

    return out;
  }

  private static List<String> splitTopLevelSegments(String s) {
    List<String> out = new ArrayList<>();

    int depth = 0;
    int last = 0;

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '{') depth++;
      else if (c == '}') depth--;
      else if (c == ',' && depth == 0) {
        // top-level comma separates segments
        String part = s.substring(last, i).trim();
        if (!part.isEmpty()) out.add(part);
        last = i + 1;
      }
    }

    String tail = s.substring(last).trim();
    if (!tail.isEmpty()) out.add(tail);

    return out;
  }
}
