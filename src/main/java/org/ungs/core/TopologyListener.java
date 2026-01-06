package org.ungs.core;

public interface TopologyListener {

  void onNodeAdded(Node node);

  // TODO:
  //  void onLinkAdded(Node from, Node to);
  //  void onLinkRemoved(...)
}
