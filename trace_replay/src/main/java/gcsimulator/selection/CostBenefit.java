package gcsimulator.selection;

import gcsimulator.BlockContainer;
import gcsimulator.Configs;
import gcsimulator.Segment;

import java.util.Comparator;
import java.util.List;

/**
 */
public class CostBenefit<T extends BlockContainer> extends SelectionAlgorithm<T> {

  CostBenefit() {
  }

  double score(T object) {
    double u = 1.0 - object.getGarbageProportion();

    double age = object.getAge();

    if (u == 0.0) return Double.MAX_VALUE;
    return (1.0 - u) / u * Math.sqrt(age);
  }


  @Override
  public SelectionAlgorithm pick(List<T> list) {
    list.sort(Comparator.comparing(this::score, Comparator.reverseOrder()));
    return this;
  }
}
