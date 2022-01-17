package gcsimulator.selection;

import gcsimulator.BlockContainer;

import java.util.*;

/**
 * Basic Greedy Algorithm:
 */
public class Greedy<T extends BlockContainer> extends SelectionAlgorithm<T>{

  public Greedy() {
  }

  public SelectionAlgorithm pick(List<T> list) {
    list.sort(Comparator.comparing(BlockContainer::getGarbageProportion, Comparator.reverseOrder()));

    return this;
  }

}
