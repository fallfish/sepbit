package gcsimulator;

import gcsimulator.indexmap.IndexMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntLongHashMap;
import org.apache.commons.math3.util.Pair;

import java.util.*;

import static gcsimulator.Configs.*;


public class Metadata {

  public static class Stat {
    long nRealTimeValidBlocks;
    long nBlocks;
    long nInvalidBlocks;

    long nBytesWriteToStorage;
    long nBytesWriteNormal;

    Stat() {
      nBlocks = 0;
      nInvalidBlocks = 0;
      nBytesWriteToStorage = 0;
      nBytesWriteNormal = 0;
    }

    @Override
    public String toString() {
      return "nBlocks: " + nBlocks + ", nInvalidBlocks: " + nInvalidBlocks;
    }

    public double getGarbageProportion() {
      return (double)nInvalidBlocks / nBlocks;
    }
  }


  public static HashMap<String, Log> logs = new HashMap<>();
  public static Log lastUpdateLog = null;
  public static Stat stat = new Stat();

  Metadata(GCScheduler GCScheduler) {
  }

  /**
   * Normal write: Append a request to the log
   * @param logId ID of log
   * @param LBA Logical Block Address
   */
  static Log write(String logId, long offset, long length, long timestamp) {
    Log log = logs.get(logId);
    // Add a new segment if it doesn't appear before
    if (log == null) {
      log = new Log(logId, timestamp);
      logs.put(logId, log);
    }

    lastUpdateLog = null;

    if (log.appendRequest(offset, length, timestamp) > 0) {
      lastUpdateLog = log;
    }

    return null;
  }
  //
  static HashMap<String, Log.Stat> getLogStats() { // all logId, num of invalid blocks, num of total blocks
    HashMap<String, Log.Stat> ret = new HashMap<>();
    for (HashMap.Entry<String, Log> segmentEntry : logs.entrySet()) {
      ret.put(segmentEntry.getKey(), segmentEntry.getValue().getStat());
    }
    return ret;
  }

  public static Collection<Log> getLogs() {
    return logs.values();
  }

  public static IndexMap getIndexMap(String logId) {
    return logs.get(logId).getIndexMap();
  }
}
