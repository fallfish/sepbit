package gcsimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import gcsimulator.Configs;
import gcsimulator.Simulator;
import gcsimulator.Log;
import gcsimulator.Segment;

public class GCWorker {
  GCWorker() {}

  public static void doJob(Log log) {
    List<Segment> pickedSegments =
        pickSegments(new ArrayList<>(log.getSegments()));

    if (pickedSegments.size() == 0) {
      return;
    }

    collectSegments(log, pickedSegments);

    if (Configs.printGCInfo && pickedSegments.size() > 0)
        System.out.print("rm_segs: " + pickedSegments.size() + "\n");
  }

  public static List<Segment> pickSegments(List<Segment> input) {
    Simulator.selectionAlgorithm.filterEmptySegments(input)
        .filterNonSealedSegments(input)
        .filterSmallerThanThreshold(input, Configs.SEGMENT_GARBAGE_PROPORTION)
        .pick(input);

    Simulator.selectionAlgorithm.pickFirstK(input, (int) (Configs.getPickSegmentAmount() / Configs.SEGMENT_SIZE));

    return input;
  }

  public static void collectSegments(Log log, List<Segment> input) {
    for (Segment segment : input) {
      segment.summarize();
      log.separator.collectSegment(segment);

      Collection<Long> entries = segment.getValidLbas();

      for (Long lba : entries) {
        log.gcAppend(lba);
      }
    }

    if (Configs.printGCInfo) {
      System.out.println("Previous: " + log.getStat().toString());
    }

    log.removeSegments(input);
  }

}
