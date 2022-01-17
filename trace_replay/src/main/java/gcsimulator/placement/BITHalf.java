package gcsimulator.placement;

import gcsimulator.Log;
import gcsimulator.Configs;
import gcsimulator.Simulator;
import gcsimulator.Segment;
import gcsimulator.indexmap.IndexMap;
import gcsimulator.indexmap.IndexMapFactory;
import gcsimulator.fifo.OnDiskFIFO;

import java.util.Map;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.HashMap;

public class BITHalf extends Separator {
  public IndexMap lastAccess;

  // used to illustrate the memory overhead of BITHalf with simulation
  public OnDiskFIFO fifo;
  public IndexMap lba2fifo;

  public long currentUd = 0;
  public int currentCollectedTemp = 0;

  public double threshold = Double.MAX_VALUE;
  public double totLifespan = 0;
  public int numCollectedSegs = 0;

  public long volumeMaxLBA = 0;

  public BITHalf() {}

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);

    lastAccess = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    lastAccess.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024));

    fifo = new OnDiskFIFO();
    fifo.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024));

    lba2fifo = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    lba2fifo.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024));
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
        threshold = totLifespan / updateThreshold / 2.0;
        numCollectedSegs = 0;
        totLifespan = 0;
        System.out.println("Log id: " + log.getId() + ", current threshold: " + threshold);
        System.out.println("Log id: " + log.getId() + ", current FIFO size: " + fifo.size() + ", num LBA: " + lba2fifo.size());
      }
    }
  }

  @Override   
  public int classify(boolean isGcAppend, long lba) {
    int level = 0;

    if (!isGcAppend) {
      if (log.accessId - lba2fifo.get(lba) < Double.min(threshold, fifo.size())) {
      // lbas that do not exist in FIFO will have a value of -1 and thus must exceed the rhs
        level = 0;
      } else {
        level = 1;
      }

      lastAccess.put(lba, log.accessId);
      lba2fifo.put(lba, log.accessId);
      fifo.add(lba);
      if (fifo.size() > Double.min(log.getnValidBlocks(), threshold)) {
        long accessId = log.accessId - fifo.size() + 1;
        long oldLba = fifo.removeFirst();
        if (lba2fifo.get(oldLba) == accessId) {
          lba2fifo.put(oldLba, -1L);
        }
        // amortize the deque overhead to each append
        if (fifo.size() > threshold) {
          accessId += 1;
          oldLba = fifo.removeFirst();
          if (lba2fifo.get(oldLba) == accessId) {
            lba2fifo.put(oldLba, -1L);
          }
        }
      }

      addBlocksWritten(level);
    } else {
      double age = log.accessId - lastAccess.get(lba);
      if (currentCollectedTemp == 0) {
        level = 2;
      } else {
        int numPoints = (numOpenSegments - 3) - 1;
        double exp = 4;

        level = 3;
        double base = threshold * exp;
        while (age >= base && level < numOpenSegments - 1) {
          base *= exp;
          level += 1;
        }
      }
    }
    nValidBlocks[level] += 1;
    nTotalBlocks[level] += 1;

    return level;
  }

  public void shutdown() {
    System.out.println("Log id: " + log.getId() + ", final FIFO size: " + fifo.size() + ", final number of LBAs: " + lba2fifo.size());
    super.shutdown();
  }

}
