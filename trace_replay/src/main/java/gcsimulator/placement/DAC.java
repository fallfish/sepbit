package gcsimulator.placement;

import gcsimulator.Log;
import gcsimulator.Configs;
import gcsimulator.indexmap.IndexMapFactory;
import gcsimulator.indexmap.IndexMap;

public class DAC extends Separator {
  public IndexMap levels;

  public DAC() {}

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);

    levels = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    levels.setSize(Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024);
  }

  @Override
  public void append(long lba) {
    if (!levels.containsKey(lba)) {
      levels.put(lba, 0);
    } else {
      long level = levels.get(lba);
      if (level < numOpenSegments - 1) levels.put(lba, level + 1);
    }
  }

  @Override
  public void gcAppend(long lba) {
    long level = levels.get(lba);
    if (level > 0) levels.put(lba, level - 1);
  }

  @Override
  public int classify(boolean isGcAppend, long lba) {
    return Math.toIntExact(levels.get(lba));
  }
}
