/*
 * AutoStream.systor17: SFR, sequential, frequency, recency;
 * Normal writes are separated into four classes (0 - 3);
 * GC writes are appended to class 4;
 */
package gcsimulator.placement;

import gcsimulator.indexmap.*;
import gcsimulator.Log;
import gcsimulator.Segment;
import gcsimulator.Configs;
import gcsimulator.Simulator;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class SFR extends Separator {
  public long chunkSize = 512L; // 2MiB
  public IndexMap chunkLastTime;
  public IndexMap chunkWrites;
  public long prevEndLba = 0L;
  public int prevLevel = 0;
  public int decayPeriod = 16384; // 64MiB writes
  public boolean isSequential = false;

  public SFR() {}

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);

    chunkLastTime = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    chunkLastTime.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024) / chunkSize);
    chunkWrites = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    chunkWrites.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024) / chunkSize);
  }

  public void promote(long chunkId) {
    long numWrites, lastTime;
    if (chunkWrites.containsKey(chunkId)) {
      numWrites = chunkWrites.get(chunkId);
      lastTime = chunkLastTime.get(chunkId);
    } else {
      numWrites = 0;
      lastTime = log.accessId;
    }

    numWrites /= Math.pow(2, (log.accessId - lastTime) / decayPeriod);
    numWrites += 1;

    chunkWrites.put(chunkId, numWrites);
    chunkLastTime.put(chunkId, log.accessId);
  }

  @Override
  public void addRequest(long lba, long length) {
    long chunkId = lba / chunkSize;
    isSequential = false;
    if (lba == prevEndLba) {
      isSequential = true;
    }

    prevEndLba = lba + length;
    for (; chunkId * chunkSize < lba + length; ++chunkId) {
      promote(chunkId);
    }
  }

  @Override
  public int classify(boolean isGcAppend, long lba) {
    if (isGcAppend) {
      return numOpenSegments - 1;
    } else {
      int level = 0;
      if (isSequential) {
        level = prevLevel;
      } else {
        long chunkId = lba / chunkSize;
        long numWrites = chunkWrites.get(chunkId);
        level = (int)Math.log((double)numWrites);
        if (level >= numOpenSegments - 1) {
          level = numOpenSegments - 2;
        }
        prevLevel = level;
      }
      return level;
    }
  }
}
