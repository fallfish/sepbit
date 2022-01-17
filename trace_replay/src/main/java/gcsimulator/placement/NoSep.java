package gcsimulator.placement;

import gcsimulator.Log;
import gcsimulator.Configs;

public class NoSep extends Separator {
  public NoSep() {}

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);
  }
}
