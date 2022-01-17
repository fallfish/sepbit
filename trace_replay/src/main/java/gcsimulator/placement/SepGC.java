package gcsimulator.placement;

import gcsimulator.Log;
import gcsimulator.Configs;

public class SepGC extends Separator {

  public SepGC() {}

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);
  }

  @Override
  public int classify(boolean isGcAppend, long Lba) {
    if (isGcAppend) return 1;
    else return 0;
  }
}
