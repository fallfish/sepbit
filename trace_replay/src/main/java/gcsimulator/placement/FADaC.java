package gcsimulator.placement;

import gcsimulator.indexmap.*;
import gcsimulator.Log;
import gcsimulator.Segment;
import gcsimulator.Configs;
import gcsimulator.Simulator;

public class FADaC extends Separator {
  public IndexMap writeFrequency;
  double fadingAvg = 0.0;

  public FADaC() {}

  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);

    writeFrequency = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    writeFrequency.setSize(Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024);
  }

  @Override
  public void append(long lba) {
    if (writeFrequency.containsKey(lba)) {
      writeFrequency.put(lba, writeFrequency.get(lba) + 1);
    } else {
      writeFrequency.put(lba, 1L);
    }
  }

  @Override
  public void gcAppend(long lba) {
    if (writeFrequency.containsKey(lba)) {
      if (writeFrequency.get(lba) > 0) {
        writeFrequency.put(lba, writeFrequency.get(lba) - 1);
      }
    }
  }

  @Override
  public void addRequest(long lba, long length) {
    long nValidBlocks = log.getnValidBlocks();
    if (nValidBlocks == 0) return;
    fadingAvg = fadingAvg + (double)length / nValidBlocks - (double)fadingAvg / nValidBlocks;
  }

  @Override
  public int classify(boolean isGcAppend, long lba) {
    double base = fadingAvg;
    int level = 0;
    long count = 0;
    if (writeFrequency.containsKey(lba)) {
      count = writeFrequency.get(lba);
    }
    if (base != 0.0) {
      while (count > base) {
        base *= 10;
        level += 1;
      }
    }
    if (level >= numOpenSegments) {
      level = numOpenSegments - 1;
    }

    return level;
  }
}
