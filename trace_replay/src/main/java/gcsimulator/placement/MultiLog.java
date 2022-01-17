// MultiLog stoica.vldb13
package gcsimulator.placement;

import gcsimulator.indexmap.*;
import gcsimulator.Log;
import gcsimulator.Segment;
import gcsimulator.Configs;
import gcsimulator.Simulator;

import java.util.Random;

public class MultiLog extends Separator {
  public IndexMap levels;
  public IndexMap lastAccess;
  public int currentMaxLevel = 0;
  public int lastUserWriteLevel = 0;
  public int lastGcWriteLevel = 0;

  public Random rand;

  public void MultiLog() {}

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);

    levels = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    levels.setSize(Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024);
    lastAccess = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    lastAccess.setSize(Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024);

    nValidBlocks = new long[numOpenSegments];
    rand = new Random();
  }

  @Override
  public void invalidate(Segment segment, long lba) {
    if (segment.meta.temperature >= numOpenSegments) {
      return;
    }
    nValidBlocks[segment.meta.temperature] -= 1;
    if (nValidBlocks[segment.meta.temperature] == 0) {
      System.out.println("Temperature " + segment.meta.temperature + " has no valid blocks!");
    }
  }

  @Override
  public void append(long lba) {
    long ud = 0;
    if (lastAccess.containsKey(lba)) {
      ud = log.accessId - lastAccess.get(lba);
    } else {
      ud = Long.MAX_VALUE;
    }
    lastAccess.put(lba, log.accessId);

    int level = 0;
    if (levels.containsKey(lba)) {
      level = Math.toIntExact(levels.get(lba));
      double udExpected = nValidBlocks[level] * (1 - (double)
          nInvalidBlocksCollected[level] / nBlocksCollected[level]) / 2.0;
      double prob = (udExpected - ud) / udExpected;
      if (prob > 0 && level > 0 && rand.nextDouble() < prob) {
        levels.put(lba, level - 1);
      }
    } else {
      levels.put(lba, 0);
    }
  }

  @Override
  public void gcAppend(long lba) {
    int level = Math.toIntExact(levels.get(lba));
    if (level == currentMaxLevel) {
      if (currentMaxLevel + 1 < numOpenSegments &&
          nValidBlocks[level] >= Configs.getSegmentMaxLen()) {
        level += 1;
        currentMaxLevel += 1;
      }
    } else {
      level += 1;
    }
    levels.put(lba, level);
  }

  @Override
  public int classify(boolean isGcAppend, long lba) {
    int level = Math.toIntExact(levels.get(lba));
    nValidBlocks[level] += 1;
    nTotalBlocks[level] += 1;
    if (!isGcAppend) {
      nBlocksWritten[level] += 1;
    }

    if (isGcAppend) {
      lastUserWriteLevel = level;
    } else {
      lastGcWriteLevel = level;
    }

    return level;
  }
}
