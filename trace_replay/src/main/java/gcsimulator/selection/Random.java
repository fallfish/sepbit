package gcsimulator.selection;

import gcsimulator.BlockContainer;

import java.util.List;

/**
 * Random Algorithm:
 */
public class Random<T extends BlockContainer> extends SelectionAlgorithm<T> {

  Random() {
  }

  public SelectionAlgorithm<T> pick(List<T> list) {
    java.util.Collections.shuffle(list, random);
    return this;
  }
}
