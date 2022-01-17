package gcsimulator.selection;

import gcsimulator.BlockContainer;
import gcsimulator.Log;
import gcsimulator.Segment;
import gcsimulator.Configs;

import java.util.Comparator;
import java.util.List;

/**
 * LRU Algorithm for MultiLog only
 * MultiLog requires to adjust the size of each partition via a customized LRU algorithm
 */
public class MultiLog<T extends BlockContainer> extends SelectionAlgorithm<T> {

  public MultiLog() {
  }

  // Note this function computes W(-(1+alpha) * exp(-(1+alpha)))
  double computeLambertW(double alpha) {
    return -Math.exp(-0.9 * alpha);
  }

  // z1 = 1 + op_1 / s_1
  // z2 = 1 + op_2 / s_2 (s_2 = 1 - s_1, op_2 = (op - op_1 * s_1) / s_2)
  double computeDerivative(double alpha, double beta, double f1, double s1, double f2, double s2) {
    double z1 = 1 + beta / s1, z2 = 1 + (alpha - beta) / s2;
    double w1 = computeLambertW(z1 - 1);
    double w2 = computeLambertW(z2 - 1);
    double result = f1 * w1 / (s1 * (w1 + 1) * (w1 + z1)) - f2 * w2 / (s2 * (w2 + 1) * (w2 + z2));
    return result;
  }

  public SelectionAlgorithm pick(List<T> list) {
    Segment s = (Segment)list.get(0);
    gcsimulator.placement.MultiLog sep = (gcsimulator.placement.MultiLog)(s.getLog().separator);

    boolean exists[] = new boolean[sep.numOpenSegments];
    for (int i = 0; i < sep.numOpenSegments; ++i) exists[i] = false;

    for (T o : list) {
      Segment tmpSeg = (Segment) o;
      exists[tmpSeg.meta.temperature] = true;
    }

    int start = sep.lastUserWriteLevel;
    int targetClass = 0;
    double minDerivative = Double.MAX_VALUE;
    double alpha = Configs.SEGMENT_GARBAGE_PROPORTION / (1 - Configs.SEGMENT_GARBAGE_PROPORTION);
    for (int i = 0; i < sep.numOpenSegments; ++i) {
      if (!exists[i]) continue;
      double beta = sep.getOp(i);
      double s1 = sep.getValidBlockPercentage(i);
      double s2 = 1 - s1;
      double f1 = sep.getWritePercentage(i);
      double f2 = 1 - f1;
      double derivative = computeDerivative(alpha, beta, s1, f1, s2, f2);
      System.out.println("Current considered level: " + i + ", derivative: " + derivative);
      System.out.format("%f %f %f %f %f %f\n", alpha, beta, s1, s2, f1, f2);
      if (derivative < minDerivative) {
        targetClass = i;
        minDerivative = derivative;
      }
    }

    final int target = targetClass;
    list.removeIf(o -> ((Segment)o).meta.temperature != target);
    list.sort(Comparator.comparing(BlockContainer::getLastAccessedTime));

    return this;
  }
}
