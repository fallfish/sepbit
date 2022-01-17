package gcsimulator.selection;

import gcsimulator.BlockContainer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static gcsimulator.Configs.*;

/**
 * Random Greedy GC Algorithm:
 */
public class RandomGreedy<T extends BlockContainer> extends SelectionAlgorithm<T> {
  private static final int    RANDOM_PERCENTAGE = 20; // Choose a small number

  RandomGreedy() {
  }

  public SelectionAlgorithm pick(List<T> list) {
    return this;
  }

  public SelectionAlgorithm pickInternal(List<T> list) {
    pickKRandomly(list, Math.max((int)Math.ceil(0.01 * RANDOM_PERCENTAGE * list.size()), 2));
    list.sort(Comparator.comparing(BlockContainer::getGarbageProportion, Comparator.reverseOrder()));
    return this;
  }

  @Override
  public SelectionAlgorithm pickUntilReadBytes(List<T> list, long untilBytes) {
    List<T> candidates = new ArrayList<>(list);
    list.clear();
    long accumulatedCapacity = 0;

    while (!candidates.isEmpty() && accumulatedCapacity < untilBytes) {
      List<T> _candidates = new ArrayList<>(candidates);
      pickInternal(_candidates);
      T candidate = _candidates.get(0);
      accumulatedCapacity += candidate.getnBlocks() * BLOCK_SIZE;
      list.add(candidate);
      candidates.remove(candidate);
    }
    return this;
  }

  @Override
  public SelectionAlgorithm<T> pickFirstK(List<T> list, int k) {
    if (k < 0) return this;
    List<T> candidates = new ArrayList<>(list);
    list.clear();

    while (!candidates.isEmpty() && list.size() < k) {
      List<T> _candidates = new ArrayList<>(candidates);
      pickInternal(_candidates);
      T candidate = _candidates.get(0);
      list.add(candidate);
      candidates.remove(candidate);
    }

    return this;
  }
}
