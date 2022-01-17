package gcsimulator;

import gcsimulator.selection.SelectionAlgorithm;
import gcsimulator.iorequest.IORequest;
import gcsimulator.tracereplay.TraceReplay;
import gcsimulator.selection.SelectionAlgorithmFactory;

import java.io.IOException;

import static gcsimulator.Configs.*;

public class Simulator {
  public static SelectionAlgorithm<Segment> selectionAlgorithm;

  public static long globalTimestampInUs = 0;
  public static long startTimestampInUs = 0;

  private void replayTraces(TraceReplay traceReplay) {
    IORequest ioRequest = traceReplay.getNextRequest();

    while (ioRequest != null) {
      globalTimestampInUs = ioRequest.timestampInUs;

      if (startTimestampInUs == 0) {
        startTimestampInUs = ioRequest.timestampInUs;
      }
      try {
        Metadata.write(ioRequest.getLogId(),
            ioRequest.getLBA(), ioRequest.getLength(),
            ioRequest.timestampInUs);
      } catch (OutOfMemoryError e) {
        System.out.println("Current Timestamp: " + globalTimestampInUs);
        throw e;
      }
      GCScheduler.schedule();

      Statistics.getInstance().incrementnIORequests();
      Statistics.getInstance().addnLBAs(ioRequest.getLength());

      if (Statistics.getInstance().getnIORequests() % 100000 == 0) {
        System.out.print(" " + Statistics.getInstance().getnIORequests() + " requests executed ... "
            + Statistics.getInstance().getnLBAs() + " LBAs written ... time: " + globalTimestampInUs + " ... ");
        GCScheduler.summary(true);
      }

      ioRequest = traceReplay.getNextRequest();
      if (Configs.endNIORequests > 0 &&
          Statistics.getInstance().getnIORequests() >= Configs.endNIORequests) break;
      if (Configs.endNLBAs > 0 &&
          Statistics.getInstance().getnLBAs() >= Configs.endNLBAs) break;
    }
  }

  private static void parseArguments(String[] args) {

    if (args.length >= 2) {
      int i = 0;
      int selection;
      while (i + 2 <= args.length) {
        switch (args[i]) {
          case "--requestType":
            Configs.requestType = RequestTypeEnum.values()[Integer.parseInt(args[i + 1])];
            break;
          case "--selectionAlgorithm":
            Configs.selectionAlgorithm = args[i + 1];
            break;
          case "--setSeparateMethod":
            Configs.separateMethod = args[i + 1];
            break;
          case "--setNumOpenSegments":
            Configs.numOpenSegments = Integer.parseInt(args[i + 1]);
            break;
          case "--path":
            Configs.tracePath = args[i + 1];
            break;
          case "--printSegmentInSummary":
            Configs.printSegmentInSummary = (Integer.parseInt(args[i + 1]) > 0);
            break;
          case "--printGCInfo":
            Configs.printGCInfo = (Integer.parseInt(args[i + 1]) > 0);
            break;
          case "--endNIORequests":
            Configs.endNIORequests = Integer.parseInt(args[i + 1]);
            break;
          case "--endNLBAs":
            Configs.endNLBAs = Integer.parseInt(args[i + 1]);
            break;
          case "--setBlockSize":
            BLOCK_SIZE = Long.parseLong(args[i + 1]);
            break;
          case "--setSegmentSize":
            SEGMENT_SIZE = Long.parseLong(args[i + 1]);
            break;
          case "--setPickSegAmount":
            pickSegAmountFactor = Integer.parseInt(args[i + 1]);
            break;
          case "--setSystemGarbageProportionThreshold":
            Configs.LOG_GARBAGE_PROPORTION = Double.parseDouble(args[i + 1]);
            Configs.SEGMENT_GARBAGE_PROPORTION = Double.parseDouble(args[i + 1]);
            break;
          case "--randomSeed":
            Configs.randomSeed = (Integer.parseInt(args[i + 1]));
            break;
          case "--outputPrefix":
            Configs.outputPrefix = args[i + 1];
            break;
          case "--indexMapType":
            Configs.indexMapType = args[i + 1];
            break;
          case "--indexMapCache":
            Configs.indexMapCache = args[i + 1];
            break;
          case "--oraclePath":
            Configs.oraclePath = args[i + 1];
            break;
          case "--propertyPath":
            Configs.loadProperty(args[i + 1]);
            break;
          default:
            System.out.println("WARNING: unregconized option: " + args[i]);
        }
        i += 2;
      }
    }
    selectionAlgorithm = SelectionAlgorithmFactory.getSelectionAlgorithm(Configs.selectionAlgorithm);

    System.out.println("arguments:");
    for (int i = 0; i < args.length; i+=2) {
      System.out.println(args[i] + " " + args[i + 1]);
    }
  }

  public static void main(String[] args) {

    Statistics.getInstance().start();

    parseArguments(args);
    Simulator simulator = new Simulator();

    // Read traces from file
    TraceReplay traceReplay = new TraceReplay();

    try {
      traceReplay.init(Configs.tracePath);
      simulator.replayTraces(traceReplay);
    } catch (IOException e) {
      e.printStackTrace();
    }

    long[] nBlocks = GCScheduler.getTotalnBlocksStat();
    Statistics.getInstance().setnTotalBlocks(nBlocks[0]);
    Statistics.getInstance().setnTotalInvalidBlocks(nBlocks[1]);

    Statistics.getInstance().summary();
    GCScheduler.summary(false);
    System.out.println(Metadata.getLogStats().size());
  }
}

