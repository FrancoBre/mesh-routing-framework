package org.ungs.core.topology.api;

import org.ungs.core.network.Node;

public interface TopologyListener {

  void onNodeAdded(Node node);

  // TODO:
  //  void onLinkAdded(Node from, Node to);
  //  void onLinkRemoved(...)
}
