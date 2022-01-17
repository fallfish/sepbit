package gcsimulator;

import gcsimulator.Metadata;
import gcsimulator.Log;
import gcsimulator.Segment;
import static gcsimulator.Configs.LOG_GARBAGE_PROPORTION;

import java.util.*;

public class GCScheduler {
  GCScheduler() {
  }

  public static void schedule() {
    if (Metadata.lastUpdateLog == null) {
      return;
    }

    if (Metadata.lastUpdateLog.getGarbageProportion() > LOG_GARBAGE_PROPORTION) {
      System.out.println("Before GC: " + Metadata.stat.getGarbageProportion());
      Statistics.getInstance().incrementnGc();

      GCWorker.doJob(Metadata.lastUpdateLog);

      System.out.println("After GC: " + Metadata.stat.getGarbageProportion());
      System.out.println();
      System.out.println();
    }
  }

  public static void summary(boolean simple) {
    if (simple) {
      long nValidBlocksRealTime = 0;
      long nSegments = 0;
      Collection<Log> logs = Metadata.getLogs();
      for (Log log : logs) {
        nValidBlocksRealTime += log.getnRealTimeValidBlocks();
        nSegments += log.getSegments().size();
      }

      System.out.println("  valid Blocks: " + nValidBlocksRealTime + ", num of segments: " + nSegments +
          " elapsed time (s): " +
          1.0 * (System.currentTimeMillis() - Statistics.getInstance().getRealStartTimestamp().getTime()) / 1000);
      return;
    }

    Collection<Log> logs = Metadata.getLogs();
    for (Log log : logs) {
      log.shutdown();
      System.out.print("  " + log.getId().toString() + ": " + log.getStat().toString());
      System.out.format(" , garbage prop = %.3f ", (log.getGarbageProportion()));
      System.out.format(" , segment WA = %.6f\n", log.getWA());

      if (Configs.printSegmentInSummary) {
        Collection<Segment> segments = log.getSegments();
        for (Segment segment : segments) {
          System.out.println("   segment gp: " + segment.getGarbageProportion() + " , " +
              " age_in_s: " + segment.getAge() / 1000000 + " , lifespan_in_s: " + segment.getLifeSpan() / 1000000);
          System.out.println(segment.toString());
        }
      }
    }
  }

  public static long[] getTotalnBlocksStat() {
    long nBlocks = 0;
    long nInvalidBlocks = 0;
    Collection<Log> logs = Metadata.getLogs();
    for (Log log : logs) {
      nBlocks += log.getStat().nBlocks;
      nInvalidBlocks += log.getStat().nInvalidBlocks;
    }

    long[] longs = new long[2];
    longs[0] = nBlocks;
    longs[1] = nInvalidBlocks;

    return longs;
  }
}
