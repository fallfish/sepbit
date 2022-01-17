// warcip.systor19
// group the write requests according to their rewrite interval
// for the gc procedure, no classification is executed
// 1. During append, compute its rewrite interval based on last access time.
// 2. During gcAppend, penalty next rewrite interval via deducting last access
// time by a value.
// 3. Periodically, merge inactive segments and split active segments to
// adapt to workload variation.
package gcsimulator.placement;

import gcsimulator.indexmap.*;
import gcsimulator.Log;
import gcsimulator.Segment;
import gcsimulator.Configs;
import gcsimulator.Simulator;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class Warcip extends Separator {

  public IndexMap lastTime;
  public IndexMap penaltyTime;

  public ArrayList<Double> centersOfSegments;
  public ArrayList<Long> numBlocksInSegments;
  public ArrayList<Long> numWritesOfClusters;
  public long totNumWrites = 0;
  public long numCollectedSegment = 0;
  public long numSealedUserWrittenSegments = 0;

  public int nextMerge = -1;

  public long ui = 0;

  public Warcip() {}

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);

    lastTime = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    lastTime.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024));
    penaltyTime = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    penaltyTime.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024));

    centersOfSegments = new ArrayList<>();
    numBlocksInSegments = new ArrayList<>();
    numWritesOfClusters = new ArrayList<>();
    for (int i = 0; i < numOpenSegments; ++i) {
      centersOfSegments.add(0d);
      numBlocksInSegments.add(0L);
      numWritesOfClusters.add(0L);
    }
  }

  public void split(int id) {
    System.out.println("Split: " + id);
    centersOfSegments.add(null);
    numBlocksInSegments.add(null);
    numWritesOfClusters.add(null);
    log.openSegments.add(null);
    for (int i = centersOfSegments.size() - 1; i > id; --i) {
      centersOfSegments.set(i, centersOfSegments.get(i - 1));
      numBlocksInSegments.set(i, numBlocksInSegments.get(i - 1));
      numWritesOfClusters.set(i, numWritesOfClusters.get(i - 1));
      log.openSegments.set(i, log.openSegments.get(i - 1));
    }
    initCenter(id);
    numWritesOfClusters.set(id, 0L);
    numBlocksInSegments.set(id, 0L);
    log.openSegment(id);
  }

  public void merge(int id) {
    log.openSegments.remove(id);
    centersOfSegments.remove(id);
    numBlocksInSegments.remove(id);
    numWritesOfClusters.remove(id);
  }

  public void splitAndMerge() {
    boolean hasSplitted = false;
    for (int i = 1; i < numWritesOfClusters.size(); ++i) {
      if (numWritesOfClusters.get(i) > totNumWrites / 2) {
        if (log.openSegments.size() < numOpenSegments) {
          split(i);
          hasSplitted = true;
          break;
        }
      }
    }

    if (!hasSplitted) {
      for (int i = 1; i < numWritesOfClusters.size(); ++i) {
        if (numWritesOfClusters.get(i) < Configs.getSegmentMaxLen()) {
          if (nextMerge == -1) {
            nextMerge = i;
          }
          break;
        }
      }
    }

    numCollectedSegment = 0;
    totNumWrites = 0;
    for (int i = 0; i < numWritesOfClusters.size(); ++i) {
      numWritesOfClusters.set(i, 0L);
    }
  }

  public void initCenter(int id) {
    double center = 0;
    center = (centersOfSegments.get(id - 1) + centersOfSegments.get(id + 1)) / 2;
    centersOfSegments.set(id, center);
  }

  @Override
  public void collectSegment(Segment segment) {
  }

  @Override
  public void sealSegment(int id) {
    if (id != 0) {
      numSealedUserWrittenSegments ++;
    }
    
    if (nextMerge == id) {
      merge(id);
      nextMerge = -1;
    } else {
      log.openSegment(id);
      numBlocksInSegments.set(id, 0L);
    }

    if (numSealedUserWrittenSegments == 256) {
      numSealedUserWrittenSegments = 0;
      splitAndMerge();
    }
  }

  @Override
  public void append(long lba) {
    if (lastTime.containsKey(lba)) {
      ui = Simulator.globalTimestampInUs - lastTime.get(lba) + penaltyTime.get(lba);
    } else {
      ui = 0;
    }

    lastTime.put(lba, Simulator.globalTimestampInUs); 
    penaltyTime.put(lba, 0);
  }

  @Override
  public void gcAppend(long lba) {
    ui = Simulator.globalTimestampInUs - lastTime.get(lba);
    penaltyTime.put(lba, ui);
  }

  @Override
  public int classify(boolean isGcAppend, long lba) {
    if (isGcAppend) {
      return 0;
    } else {
      // only focus on foreground I/O
      int nearestIndex = 1;
      double minDiff = Double.MAX_VALUE;

      for (int i = centersOfSegments.size() - 1; i >= 1; --i) {
        double diff = Math.abs(centersOfSegments.get(i) - ui);
        if (diff < minDiff) {
          nearestIndex = i;
          minDiff = diff;
        }
      }

      double center = centersOfSegments.get(nearestIndex);
      long numBlocks = numBlocksInSegments.get(nearestIndex);
      long numWrites = numWritesOfClusters.get(nearestIndex);

      // update center
      centersOfSegments.set(nearestIndex, ((double)center * numBlocks + ui) / (numBlocks + 1));
      numBlocksInSegments.set(nearestIndex, numBlocks + 1);
      numWritesOfClusters.set(nearestIndex, numWrites + 1);
      totNumWrites += 1;

      return nearestIndex;
    }
  }
}
