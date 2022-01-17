package gcsimulator.placement;

import gcsimulator.Simulator;
import gcsimulator.Configs;
import gcsimulator.Log;
import gcsimulator.Segment;

public abstract class Separator {
  Log log;
  public int numOpenSegments = 6;
  public long[] nValidBlocks;
  public long[] nTotalBlocks;
  public long[] nBlocksWritten;
  public long[] nBlocksCollected;
  public long[] nInvalidBlocksCollected;
  public long[] nSegmentsCollected;
  public long[] totalLifespan;
  public double nTotalSegmentsCollected = 0;
  public double nTotalInvalidBlocksCollected = 0;
  public double nTotalBlocksCollcted = 0;
  public double avgValidBlocksPerGC = 0;

  public void init(Log log, int numOpenSegments) {
    this.log = log;
    this.numOpenSegments = numOpenSegments;
    log.initSegments(numOpenSegments);

    nBlocksWritten = new long[numOpenSegments];
    nValidBlocks = new long[numOpenSegments];
    nTotalBlocks = new long[numOpenSegments];
    nBlocksWritten = new long[numOpenSegments];
    nBlocksCollected = new long[numOpenSegments];
    nInvalidBlocksCollected = new long[numOpenSegments];
    nSegmentsCollected = new long[numOpenSegments];
    totalLifespan = new long[numOpenSegments];
  }

  public void addRequest(long lba, long length) {
    return;
  }

  public void collectSegment(Segment segment) {
    nValidBlocks[segment.meta.temperature] -= segment.getnValidBlocks();
    nTotalBlocks[segment.meta.temperature] -= segment.getnBlocks();

    nTotalBlocksCollcted += segment.getnBlocks();
    nTotalInvalidBlocksCollected += segment.getnInvalidBlocks();
    nTotalSegmentsCollected += 1;
    avgValidBlocksPerGC = (nTotalBlocksCollcted - nTotalInvalidBlocksCollected) / nTotalSegmentsCollected;

    nBlocksCollected[segment.meta.temperature] += segment.getnBlocks();
    nInvalidBlocksCollected[segment.meta.temperature] += segment.getnInvalidBlocks();
    nSegmentsCollected[segment.meta.temperature] += 1;
    totalLifespan[segment.meta.temperature] += log.accessId - segment.meta.createdAccessId;

    return;
  }

  public void invalidate(Segment segment, long lba) {
    nValidBlocks[segment.meta.temperature] -= 1;
    return;
  }

  public void append(long lba) {
    return;
  }

  public void gcAppend(long lba) {
    return;
  }

  public void postAppend(Segment segment, long lba) {
    return;
  }

  public void sealSegment(int id) {
    log.openSegment(id);
  }

  public int classify(boolean isGcAppend, long lba) {
    return 0;
  }

  public void shutdown() {
    for (int i = 0; i < numOpenSegments; ++i) {
      double averageGp = 0;
      if (nBlocksCollected[i] != 0) {
        averageGp = (double)nInvalidBlocksCollected[i] / nBlocksCollected[i];
      }

      System.out.println("Class " + i + ": numCollectedBlocks: " + nBlocksCollected[i] +
          ", numCollectedInvalidBlocks: " + nInvalidBlocksCollected[i] +
          ", averageGp: " + averageGp +
          ", numBlocksWritten: " + nBlocksWritten[i] + ", op: " + getOp(i) +
          ", percentage of writes: " + getWritePercentage(i) +
          ", percentage of valid blocks: " + getValidBlockPercentage(i) +
          ", avgLifespan: " + (double)totalLifespan[i] / nSegmentsCollected[i]);
    }
    return;
  }

  void addBlocksWritten(int level) {
    nBlocksWritten[level] += 1;
  }

  public double getOp(int level) {
    //System.out.println("getOp: nTotalBlocks: " + nTotalBlocks[level] + ", nValidBlocks: " + nValidBlocks[level]);
    return ((double)nTotalBlocks[level] - nValidBlocks[level]) / nValidBlocks[level];
  }

  public double getWritePercentage(int level) {
    return (double)nBlocksWritten[level] / log.accessId;
  }

  public double getValidBlockPercentage(int level) {
    return (double)nValidBlocks[level] / log.getnValidBlocks();
  }
}
