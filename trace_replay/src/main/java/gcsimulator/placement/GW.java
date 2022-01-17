package gcsimulator.placement;

import gcsimulator.Log;
import gcsimulator.Configs;
import gcsimulator.Simulator;
import gcsimulator.Segment;
import gcsimulator.indexmap.IndexMap;
import gcsimulator.indexmap.IndexMapFactory;

import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class GW extends Separator {
  public IndexMap lastAccess;

  public long currentUd = 0;
  public int currentCollectedTemp = 0;

  public double threshold = Double.MAX_VALUE;
  public double totLifespan = 0;
  public int numCollectedSegs = 0;

  public GW() {}

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);

    lastAccess = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    lastAccess.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024));
  }

  @Override
  public void collectSegment(Segment segment) {
    int updateThreshold = 16;
    super.collectSegment(segment);

    int temp = segment.meta.temperature;
    currentCollectedTemp = temp;

    if (temp == 0) {
      totLifespan += (double)log.accessId - segment.meta.createdAccessId;
      numCollectedSegs += 1;

      if (numCollectedSegs == updateThreshold) {
        threshold = totLifespan / updateThreshold;
        numCollectedSegs = 0;
        totLifespan = 0;
        System.out.println("Log id: " + log.getId() + ", current threshold: " + threshold);
      }
    }
  }

  @Override
  public void append(long lba) {
    if (lastAccess.containsKey(lba)) {
      currentUd = log.accessId - lastAccess.get(lba);
    } else {
      currentUd = Long.MAX_VALUE;
    }

    lastAccess.put(lba, log.accessId);
  }

  @Override
  public void gcAppend(long lba) {
    currentUd = log.accessId - lastAccess.get(lba);
  }

  @Override   
  public int classify(boolean isGcAppend, long lba) {
    int level = 0;

    if (!isGcAppend) {
      level = 0;
    } else {
      double age = log.accessId - lastAccess.get(lba);
      double base = threshold * 4;

      level = 1;
      while (age >= base && level < numOpenSegments - 1) {
        base *= 4;
        level += 1;
      }
    }
    nValidBlocks[level] += 1;
    nTotalBlocks[level] += 1;

    return level;
  }

}
