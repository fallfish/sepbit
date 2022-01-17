package gcsimulator.selection;

import gcsimulator.*;
import java.util.*;
import java.util.Random;
import static gcsimulator.Configs.*;

public abstract class SelectionAlgorithm <T extends BlockContainer> {
  Random random = new Random(Configs.randomSeed);

  SelectionAlgorithm() {}

  /**
   * API - pick segments / logs
   */
  public abstract SelectionAlgorithm<T> pick(List<T> list);

  /**
   * API - get sealed segments
   */
  public SelectionAlgorithm<T> filterNonSealedSegments(List<Segment> segments) {
    segments.removeIf(o -> !o.isSealed());
    return this;
  }

  /**
   * API - Pick first K
   * @param list
   * @param k
   * @return a list
   */
  public SelectionAlgorithm<T> pickFirstK(List<T> list, int k) {
    if (k < 0) return this;

    while (list.size() > k) {
      list.remove(k);
    }
    return this;
  }

  /**
   * API - Filter by GP threshold
   */
  public SelectionAlgorithm<T> filterSmallerThanThreshold(List<T> list, double threshold) {
    assert threshold > 0;

    list.removeIf(o -> o.getGarbageProportion() < threshold);
    return this;
  }

  private int[] getKFromNRandomly(int n, int k) {
    int[] ints;

    if (k >= n) {
      ints = new int[n];
      for (int i = 0; i < n; i++) ints[i] = i;
      return ints;
    }

    ints = new int[k];

    int[] indices = new int[n];

    for (int i = 0; i < n; i++) {
      indices[i] = i;
    }

    int count = 0;
    int id;
    int tmpN = n;
    while (count < k) {
      id = random.nextInt(tmpN);
      ints[count++] = indices[id];

      indices[id] = indices[tmpN-1];
      tmpN--;
    }

    return ints;
  }

  /**
   * API - Pick k randomly
   * @param list
   * @param k
   * @return
   */
  public SelectionAlgorithm pickKRandomly(List<T> list, int k) {
    assert k > 0;

    int[] ints = getKFromNRandomly(list.size(), k);
    int i = 0;
    for (Iterator<T> iter = list.listIterator(); iter.hasNext(); ) {
      T object = iter.next();
      boolean delete = true;
      for (int j = 0; j < k; ++j) {
        if (ints[j] == i) {
          delete = false;
          break;
        }
      }
      if (delete) {
        iter.remove();
      }
      ++i;
    }
    return this;
  }

  /**
   * Choose the non-empty logs / segments
   * @param list
   * @return A list of resulting segments
   */
  public SelectionAlgorithm<T> filterEmptySegments(List<T> list) {
    list.removeIf(o -> o.getnInvalidBlocks() == 0);

    return this;
  }

  /**
   *
   * @param list
   * @param untilBytes
   * @return
   */
  public SelectionAlgorithm pickUntilReadBytes(List<T> list, long untilBytes) {
    long currentCapacity;
    long accumulatedCapacity = 0;
    for (Iterator<T> iter = list.listIterator(); iter.hasNext(); ) {
      T object = iter.next();
      currentCapacity = object.getnBlocks() * BLOCK_SIZE;
      if (accumulatedCapacity >= untilBytes) {
        iter.remove();
      } 
      accumulatedCapacity += currentCapacity;
    }

    return this;
  }

  /**
   *
   * @param list
   * @param untilBytes
   * @return
   */
  public SelectionAlgorithm pickUntilValidBytes(List<T> list, long untilBytes) {
    long currentCapacity;
    long accumulatedCapacity = 0;
    boolean flag = false;
    for (Iterator<T> iter = list.listIterator(); iter.hasNext(); ) {
      T object = iter.next();
      currentCapacity = object.getnValidBlocks() * BLOCK_SIZE;
      if (accumulatedCapacity + currentCapacity > untilBytes) {
        flag = true;
      } 
      if (flag) {
        iter.remove();
      } else {
        accumulatedCapacity += currentCapacity;
      }
    }

    return this;
  }
}

