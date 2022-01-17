// SFS.fast12 

package gcsimulator.placement;

import gcsimulator.Log;
import gcsimulator.Segment;
import gcsimulator.Simulator;
import gcsimulator.Configs;
import gcsimulator.indexmap.IndexMap;
import gcsimulator.indexmap.IndexMapFactory;

import java.util.ArrayList;
import java.util.Collections;

public class SFS extends Separator {
  public IndexMap lastModifiedTime;
  public IndexMap writeFrequency;
  public double currentLbaHotness;
  public ArrayList<Double> centers;
  ArrayList<Double>[] groups;
  public int nCollectedSegments = 0;

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);

    lastModifiedTime = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    lastModifiedTime.setSize(Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024);
    writeFrequency = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    writeFrequency.setSize(Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024);

    groups = new ArrayList[numOpenSegments];
    centers = new ArrayList<>();
    for (int i = 0; i < numOpenSegments; ++i) {
      centers.add(0d);
      groups[i] = new ArrayList<>();
    }
  }

  public void segmentQuantization() {
    ArrayList<Segment> segments = new ArrayList<>();
    for (Segment segment : log.getSegments()) {
      if (segment.isSealed()) {
        segments.add(segment);
      }
    }

    if (segments.size() <= numOpenSegments) {
      for (int i = 0; i < numOpenSegments; ++i) {
        Segment segment = segments.get(i % segments.size());
        double hotness = (double) segment.meta.totalWriteCount / ((double)segment.getnValidBlocks() *
            Simulator.globalTimestampInUs - segment.meta.totalLastModifiedTime);
        centers.set(i, hotness);
      }
    } else {
      // clustering
      for (int j = 0; j < 3; ++j) {
        for (int i = 0; i < numOpenSegments; ++i) groups[i].clear();
        for (Segment segment : segments) {
          // compute hotness
          double hotness = (double) segment.meta.totalWriteCount / ((double)segment.getnValidBlocks() *
              Simulator.globalTimestampInUs - segment.meta.totalLastModifiedTime);
          //System.out.println("Hotness: " + segment.meta.totalWriteCount + " " + segment.getnValidBlocks() + " " + Simulator.globalTimestampInUs + " " + segment.meta.totalLastModifiedTime + " " + hotness);
          double minDiff = Double.MAX_VALUE;
          int destGroup = 0;
          for (int i = 0; i < numOpenSegments; i++) {
            double diff = Math.abs(hotness - centers.get(i));
            if (diff < minDiff) {
              minDiff = diff;
              destGroup = i;
            }
          }
          groups[destGroup].add(hotness);
        }

        for (int i = 0; i < numOpenSegments; ++i) {
          double tot = 0;
          for (double d : groups[i]) {
            tot += d;
          }
          if (groups[i].size() == 0) {
            centers.set(i, 0d);
          } else {
            centers.set(i, tot / groups[i].size());
          }
        }
      }
    }

    Collections.sort(centers, Collections.reverseOrder());
    nCollectedSegments += 1;
    if (nCollectedSegments == 16) {
      nCollectedSegments = 0;
      for (int i = 0; i < numOpenSegments; ++i) {
        System.out.println("Centers: " + centers.get(i));
      }
    }
  }

  @Override
  public void invalidate(Segment segment, long lba) {
    segment.meta.totalWriteCount -= writeFrequency.get(lba);
    segment.meta.totalLastModifiedTime -= lastModifiedTime.get(lba);
  }

  @Override
  public void append(long lba) {
    if (lastModifiedTime.containsKey(lba)) {
      long freq = writeFrequency.get(lba);
      long elapsedTime = Simulator.globalTimestampInUs - lastModifiedTime.get(lba);
      currentLbaHotness = (double) freq / elapsedTime;
      writeFrequency.put(lba, freq + 1);
    } else {
      currentLbaHotness = Double.MAX_VALUE;
      writeFrequency.put(lba, 1);
    }
    lastModifiedTime.put(lba, Simulator.globalTimestampInUs);
  }

  @Override
  public void collectSegment(Segment segment) {
    super.collectSegment(segment);
    segmentQuantization();
  }

  @Override
  public void gcAppend(long lba) {
    long freq = writeFrequency.get(lba);
    long elapsedTime = Simulator.globalTimestampInUs - lastModifiedTime.get(lba);
    currentLbaHotness = (double) freq / elapsedTime;
  }

  @Override
  public void postAppend(Segment segment, long lba) {
    segment.meta.totalWriteCount += writeFrequency.get(lba);
    segment.meta.totalLastModifiedTime += lastModifiedTime.get(lba);
  }

  @Override
  public int classify(boolean isGcAppend, long lba) {
    int nearestIndex = 0;
    double minDiff = Double.MAX_VALUE;

    for (int i = centers.size() - 1; i >= 0; --i) {
      double diff = Math.abs(centers.get(i) - currentLbaHotness);
      if (diff < minDiff) {
        nearestIndex = i;
        minDiff = diff;
      }
    }

    return nearestIndex;
  }
}
