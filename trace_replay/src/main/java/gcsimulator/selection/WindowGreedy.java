package gcsimulator.selection;

import gcsimulator.BlockContainer;

import java.util.Comparator;
import java.util.List;

/**
 * Random Greedy GC Algorithm:
 */
public class WindowGreedy<T extends BlockContainer> extends SelectionAlgorithm<T> {
  private static final int    WINDOW_PERCENTAGE = 90;

  WindowGreedy() {
  }

  public SelectionAlgorithm pick(List<T> list) {
    list.sort(Comparator.comparing(BlockContainer::getAge, Comparator.reverseOrder()));
    pickFirstK(list, (int)Math.ceil(0.01 * WINDOW_PERCENTAGE * list.size()));
    list.sort(Comparator.comparing(BlockContainer::getGarbageProportion, Comparator.reverseOrder()));

    return this;
  }
}
