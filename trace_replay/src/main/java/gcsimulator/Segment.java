package gcsimulator;

import gcsimulator.segment.SegmentMeta;
import org.apache.commons.math3.util.Pair;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;

import static gcsimulator.Configs.*;

/*
  gcsimulator.Segment is the basic unit of garbage collection
 */
public class Segment implements BlockContainer {
  private boolean sealed;
  private Log log;
  public SegmentMeta meta;

  // Only available when "printGCInfo_WithBlockLastAccess" is enabled.
  // Update distance = next access id - invalidated access id
  private long totalUpdateDistanceOfInvalidBlocks = 0;

  public void summarize() {
    meta.summarize();
  }

  public Map<Long, long[]> getValidPairs() {
    return meta.getValidPairs();
  }

  public Collection<Long> getValidLbas() {
    return meta.getValidLbas();
  }

  public long getCreatedAccessId() {
    return meta.createdAccessId;
  }

  public long getAgeFromFirstInvalidate() {
    if (getnInvalidBlocks() == 0) {
      return 0;
    } else {
      return log.accessId - meta.firstEvitcionAccessId;
    }
  }


  @Override
  public long getnValidBlocks() {
    return meta.nBlocks - meta.nInvalidBlocks;
  }

  @Override
  public long getnInvalidBlocks() {
    return meta.nInvalidBlocks;
  }

  @Override
  public long getnBlocks() {
    return meta.nBlocks;
  }

  @Override
  public double getGarbageProportion() {
    return (double)(meta.nInvalidBlocks) / meta.nBlocks;
  }

  @Override
  public double getAge() {
    return Simulator.globalTimestampInUs - meta.timestampModifiedInUs;
  }

  @Override
  public double getLastAccessedTime() {
    return meta.timestampModifiedInUs;
  }

  double getLifeSpan() {
    return meta.timestampModifiedInUs - meta.timestampCreatedInUs;
  }

  void addUpdateDistanceOfInvalidBlocks(long updateDistance) {
    totalUpdateDistanceOfInvalidBlocks += updateDistance;
  }

  double getAvgUpdateDistanceOfInvalidBlocks() {
    return (getnInvalidBlocks() == 0) ? 0.00 : 1.0 * totalUpdateDistanceOfInvalidBlocks / getnInvalidBlocks();
  }

  public Segment(long timestamp, Log log) {
    this.log = log;
    meta = new SegmentMeta(log.getId().toString(), log.getNewSegmentId(), timestamp);
    meta.createdAccessId = log.accessId;
  }

  public void setTemperature(int t) {
    meta.temperature = t;
  }

  public long getSegmentId() {
    return meta.segmentId;
  }

  public boolean isSealed() {
    return meta.isSealed;
  }

  public void seal() {
    if (!sealed) {
      sealed = true;
      meta.seal();
      meta.sealedAccessId = log.accessId;
    }
  }
  public Log getLog() {
    return this.log;
  }

  long append(long LBA, long timestamp) {
    long offset = meta.append(LBA, timestamp);
    return offset;
  }

  public void appendInfo(long[] o) {
    meta.appendInfo(o);
  }

  void invalidate(long offset) {
    if (getnInvalidBlocks() == 128) {
      meta.firstEvitcionAccessId = log.accessId;
    }
    meta.invalidate(offset);
  }

  void destroy() {
    meta.destroy();
  }


  @Override
  public String toString() {
    return meta.toString() + ", " + (isSealed() ? "sealed" : "not sealed");
  }

}
