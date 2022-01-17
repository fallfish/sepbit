package gcsimulator.selection;

import gcsimulator.BlockContainer;

import java.util.Comparator;
import java.util.List;

/**
 * LRU Algorithm
 */
public class Lru<T extends BlockContainer> extends SelectionAlgorithm<T> {

  Lru() {
  }

  public SelectionAlgorithm pick(List<T> list) {
    list.sort(Comparator.comparing(BlockContainer::getLastAccessedTime));
    return this;
  }
}
