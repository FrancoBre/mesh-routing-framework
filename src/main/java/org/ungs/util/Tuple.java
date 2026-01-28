package org.ungs.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Tuple<A, B> {

  public final A first;
  public final B second;
}
