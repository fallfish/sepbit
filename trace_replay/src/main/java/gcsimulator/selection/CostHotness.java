package gcsimulator.selection;

import gcsimulator.BlockContainer;
import gcsimulator.Configs;
import gcsimulator.Segment;
import gcsimulator.Simulator;

import java.util.Comparator;
import java.util.List;

/**
 */
public class CostHotness<T extends BlockContainer> extends SelectionAlgorithm<T> {

  CostHotness() {
  }

  double score(T object) {
    double u = 1.0 - object.getGarbageProportion();
    Segment segment = (Segment)object;

    double hotness = (double) segment.meta.totalWriteCount /
      ((double)segment.getnValidBlocks() * Simulator.globalTimestampInUs -
       segment.meta.totalLastModifiedTime);

    if (u == 0.0) return Double.MAX_VALUE;
    return (1.0 - u) / (u * hotness);
  }


  @Override
  public SelectionAlgorithm pick(List<T> list) {
    list.sort(Comparator.comparing(this::score, Comparator.reverseOrder()));
    return this;
  }
}
