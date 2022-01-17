package gcsimulator;

import gcsimulator.indexmap.*;
import gcsimulator.placement.*;
import gcsimulator.tracereplay.TraceReplay;
import org.apache.commons.math3.util.Pair;
import org.apache.kerby.config.Conf;

import java.io.*;
import java.util.*;

import java.lang.NullPointerException;


/*
  gcsimulator.Log is the basic unit of Log-structure.
 */
public class Log implements BlockContainer {
  private String id;
  private int nextSegmentId = 0;

  public HashMap<Long, Segment> segments;
  public ArrayList<Segment> openSegments;

  private IndexMap indexMap;
  public Separator separator;

  public long accessId = 0;
  public long startTimestampInUs = 0;

  private Stat stat;

  @Override
  public long getnValidBlocks() {
    return getStat().nBlocks - getStat().nInvalidBlocks;
  }

  @Override
  public long getnInvalidBlocks() {
    return getStat().nInvalidBlocks;
  }

  public long getnRealTimeValidBlocks() {
    return getStat().nRealTimeValidBlocks;
  }

  @Override
  public long getnBlocks() {
    return getStat().nBlocks;
  }

  @Override
  public double getGarbageProportion() {
    return getStat().nInvalidBlocks / (double)getStat().nBlocks;
  }

  @Override
  public double getAge() {
    return Simulator.globalTimestampInUs - getStat().timestampModifiedInUs;
  }

  @Override
  public double getLastAccessedTime() {
    return getStat().timestampModifiedInUs;
  }

  public static class Stat {
    long nRealTimeValidBlocks;
    long nBlocks;
    long nInvalidBlocks;

    long nBytesWriteToStorage;
    long nBytesWriteNormal;

    long timestampCreatedInUs;
    long timestampModifiedInUs;

    Stat() {
      nBlocks = 0;
      nInvalidBlocks = 0;
      nBytesWriteToStorage = 0;
      nBytesWriteNormal = 0;
      timestampCreatedInUs = Simulator.startTimestampInUs;
      timestampModifiedInUs = Simulator.startTimestampInUs;
    }

    @Override
    public String toString() {
      return "nBlocks: " + nBlocks + ", nInvalidBlocks: " + nInvalidBlocks;
    }
  }

  public void initSegments(int numSegments) {
    for (int i = 0; i < numSegments; ++i) {
      openSegments.add(new Segment(Simulator.globalTimestampInUs, this));
      openSegments.get(i).setTemperature(i);
      segments.put(openSegments.get(i).getSegmentId(), openSegments.get(i));
    }
  }

  Log(String id, Long timestamp) {
    this.id = id;
    segments = new HashMap<>();
    openSegments = new ArrayList<>();
    startTimestampInUs = Simulator.startTimestampInUs;

    stat = new Stat();

    getStat().timestampCreatedInUs = timestamp;
    getStat().timestampModifiedInUs = timestamp;

    // init index map
    indexMap = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    long wss = Configs.volumeWSS.get(id);
    long maxLba = Configs.volumeMaxLba.get(id);
    indexMap.setSize(maxLba / 4096 + 1024);

    System.out.println("New log: " + id + " " + wss + " " + maxLba);

    // init separate method
    separator = SeparatorFactory.getInstance(Configs.separateMethod);
    separator.init(this, Configs.numOpenSegments);

    stat.nBlocks = 0;
    stat.nInvalidBlocks = 0;
  }

  int appendRequest(long offset, long length, long timestamp) {
    // request-level update for the separator metadata
    separator.addRequest(offset, length);
    for (int i = 0; i < length; i += 1) {
      append(offset + i, timestamp);
    }

    getStat().timestampModifiedInUs = timestamp;
    stat.nBlocks += length; // update gcsimulator.Statistics
    Metadata.stat.nBlocks += length; // update gcsimulator.Statistics
    stat.nBytesWriteToStorage += Configs.BLOCK_SIZE * length;
    stat.nBytesWriteNormal += Configs.BLOCK_SIZE * length;
    Metadata.stat.nBytesWriteToStorage += Configs.BLOCK_SIZE * length;
    Metadata.stat.nBytesWriteNormal += Configs.BLOCK_SIZE * length;

    return Math.toIntExact(length);
  }

  public void openSegment(int id) {
    openSegments.set(id, new Segment(Simulator.globalTimestampInUs, this));
    segments.put(openSegments.get(id).getSegmentId(), openSegments.get(id));
    openSegments.get(id).setTemperature(id);
  }

  public void sealSegment(int id) {
    Segment segment = openSegments.get(id);
    segment.seal();
    stat.nInvalidBlocks += segment.getnInvalidBlocks();
    Metadata.stat.nInvalidBlocks += segment.getnInvalidBlocks();
  }

  /**
   * @param Lba the Logical Block Address in the Disk
   * @return the number of new blocks added
   */
  int append(long lba, long timestamp) {
    accessId += 1;

    // Check if LBA exists. Find an existing segment
    if (indexMap.containsKey(lba)) {
      long offset = indexMap.get(lba);

      Segment prevSegment = segments.get(offset / Configs.getSegmentMaxLen());

      try {
        prevSegment.invalidate(offset % Configs.getSegmentMaxLen());
        separator.invalidate(prevSegment, lba);
      } catch (NullPointerException e) {
        System.out.println(id);
        System.out.println(lba);
        System.out.println(offset);
        System.out.println(Configs.getSegmentMaxLen());
        e.printStackTrace();
        throw e;
      }

      if (!openSegments.contains(prevSegment)) {
        stat.nInvalidBlocks += 1;  // update gcsimulator.Statistics
        Metadata.stat.nInvalidBlocks += 1;
      }
    } else {
      stat.nRealTimeValidBlocks += 1;
    }

    {
      separator.append(lba);
    }

    // The block is a new block
    // Get the latest segment
    int openId = separator.classify(false, lba);
    Segment segment = openSegments.get(openId);
    long fileLocation = segment.append(lba, timestamp);
    indexMap.put(lba, fileLocation);

    {
      separator.postAppend(segment, lba);
    }

    if (segment.getnBlocks() >= Configs.getSegmentMaxLen()) {
      sealSegment(openId);
      {
        separator.sealSegment(openId);
      }
    }

    return 1;
  }

  public void gcAppend(long lba) {
    separator.gcAppend(lba);

    int openId = separator.classify(true, lba);
    Segment segment = openSegments.get(openId);

    long fileLocation = segment.append(lba, Simulator.globalTimestampInUs);
    indexMap.put(lba, fileLocation);
    {
      separator.postAppend(segment, lba);
    }

    if (segment.getnBlocks() >= Configs.getSegmentMaxLen()) {
      sealSegment(openId);
      {
        separator.sealSegment(openId);
      }
    }

    stat.nBlocks += 1; // update gcsimulator.Statistics
    Metadata.stat.nBlocks += 1; // update gcsimulator.Statistics
    stat.nBytesWriteToStorage += Configs.BLOCK_SIZE;
    Metadata.stat.nBytesWriteToStorage += Configs.BLOCK_SIZE;
  }

  /**
   * After GC reports, we need to remove those stale segments and delete them.
   */
  public void removeSegments(List<Segment> oldSegments) {
    if (oldSegments.isEmpty()) return;

    if (Configs.printGCInfo) {
      System.out.print("removed_seg_info: gp age(s) lifespan(s) log blocks[n,valid,invalid]");
      System.out.println();
    }

    for (Segment segment : oldSegments) {
      if (Configs.printGCInfo) {
        System.out.print("rmed_seg: " + segment.getGarbageProportion() + " " + segment.getAge() / 1000000 + " " + segment.getLifeSpan() / 1000000 + " "
            + id + " " + segment.getnBlocks() + " " + segment.getnValidBlocks() + " " + segment.getnInvalidBlocks());
      }
      System.out.println();

      Statistics.getInstance().addRemovedSegmentGP(segment.getGarbageProportion());
      segment.destroy();
    }

    for (Segment segment : oldSegments) {
      this.segments.remove(segment.getSegmentId());

      stat.nBlocks -= segment.getnBlocks();
      stat.nInvalidBlocks -= segment.getnInvalidBlocks();
      Metadata.stat.nBlocks -= segment.getnBlocks();
      Metadata.stat.nInvalidBlocks -= segment.getnInvalidBlocks();
    }
    if (Configs.printGCInfo)
      System.out.println("Afterwards: " + getStat().toString());
  }

  public Stat getStat() {
    return stat;
  }

  double getWA() {
    return 1.0 * stat.nBytesWriteToStorage / stat.nBytesWriteNormal;
  }

  public Collection<Segment> getSegments() {
    return segments.values();
  }

  public String getId() {
    return id;
  }

  IndexMap getIndexMap() {
    return indexMap;
  }

  long getNewSegmentId() {
    return nextSegmentId++;
  }

  void shutdown() {
    separator.shutdown();
  }

}
