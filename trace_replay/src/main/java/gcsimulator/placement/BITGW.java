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

public class BITGW extends Separator {
  public IndexMap lastAccess;

  // used to illustrate the memory overhead of BITGW with simulation
  public OnDiskFIFO fifo;
  public IndexMap lba2fifo;
  //

  public long currentUd = 0;
  public int currentCollectedTemp = 0;

  public double []thresholds;
  public double []totLifespans;
  public int []numCollectedSegs;

  public BITGW() {}

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);

    lastAccess = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    lastAccess.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024));

    fifo = new OnDiskFIFO();
    fifo.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024));

    lba2fifo = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    lba2fifo.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024));

    thresholds = new double[numOpenSegments];
    totLifespans = new double[numOpenSegments];
    numCollectedSegs = new int[numOpenSegments];
    for (int i = 0; i < numOpenSegments; ++i) {
      thresholds[i] = Double.MAX_VALUE;
      totLifespans[i] = 0;
      numCollectedSegs[i] = 0;
    }
  }

  @Override
  public void collectSegment(Segment segment) {
    int updateThreshold = 16;
    super.collectSegment(segment);

    int temp = segment.meta.temperature;
    currentCollectedTemp = temp;

    //if (temp == 0) {
    totLifespans[temp] += (double)log.accessId - segment.meta.createdAccessId;
    numCollectedSegs[temp] += 1;

    if (numCollectedSegs[temp] == updateThreshold) {
      thresholds[temp] = totLifespans[temp] / updateThreshold;
      numCollectedSegs[temp] = 0;
      totLifespans[temp] = 0;
      if (temp == 0) {
        System.out.println("Log id: " + log.getId() + ", current threshold: " + thresholds[temp]);
        System.out.println("Log id: " + log.getId() + ", current FIFO size: " + fifo.size() + ", num LBA: " + lba2fifo.size());
      }
    }
    //}
  }

  @Override   
  public int classify(boolean isGcAppend, long lba) {
    int level = 0;

    if (!isGcAppend) {
      if (log.accessId - lba2fifo.get(lba) < Double.min(thresholds[0], fifo.size())) {
      // lbas that do not exist in FIFO will have a value of -1 and thus must exceed the rhs
        level = 0;
      } else {
        level = 1;
      }

      lastAccess.put(lba, log.accessId);
      lba2fifo.put(lba, log.accessId);
      fifo.add(lba);
      if (fifo.size() > Double.min(log.getnValidBlocks(), thresholds[0])) {
        long accessId = log.accessId - fifo.size() + 1;
        long oldLba = fifo.removeFirst();
        if (lba2fifo.get(oldLba) == accessId) {
          lba2fifo.put(oldLba, -1L);
        }
        // amortize the deque overhead to each append
        if (fifo.size() > thresholds[0]) {
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
        if (age <= thresholds[3]) {
          level = 3;
        } else if (age <= thresholds[3] + thresholds[4]) {
          level = 4;
        } else {
          level = 5;
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
