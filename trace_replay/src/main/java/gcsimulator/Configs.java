package gcsimulator;

import java.io.*;
import java.util.Scanner;
import java.util.HashMap;

public class Configs {
  public static HashMap<String, Long> volumeWSS = new HashMap<>();
  public static HashMap<String, Long> volumeMaxLba = new HashMap<>();

  public static String outputPrefix = "./";

  // Simulator related
  public static long          BLOCK_SIZE = 4096;
  public static long SEGMENT_SIZE = 512L * 1024 * 1024;

  // Garbage Collection Related
  public static double LOG_GARBAGE_PROPORTION = 0.15;
  public static double SEGMENT_GARBAGE_PROPORTION = 0.15;

  public enum RequestTypeEnum {
    ALI
  }

  public enum TraceReplayModeEnum {
    DEV_LIST
  }

  // Global configurations
  public static int numOpenSegments = 2;
  public static String tracePath = "";
  public static String oraclePath = "";
  public static TraceReplayModeEnum traceReplayMode = TraceReplayModeEnum.DEV_LIST;
  public static RequestTypeEnum requestType = RequestTypeEnum.ALI;
  public static boolean printGCInfo = true;
  public static boolean printSegmentInSummary = true;

  public static String selectionAlgorithm = "Greedy";
  public static String separateMethod = "Null";

  public static long endNIORequests = 0;
  public static long endNLBAs = 0;

  public static long randomSeed = 0;

  public static String indexMapType = "Persistent";
  public static String indexMapCache = "GlobalPageCache";

  // pickSegMode indicates how many data amount should be collected;
  public static int pickSegMode = 3;
  public static int pickSegAmountFactor = 1;

  public static long getPickSegmentAmount() {
    return SEGMENT_SIZE * pickSegAmountFactor;
  }

  public static long getSegmentMaxLen() {
    return SEGMENT_SIZE / BLOCK_SIZE;
  }

//  public static String propertyPath = "";
  public static void loadProperty(String propertyPath) {
    File f = new File(propertyPath);
    try {
      Scanner input = new Scanner(f);
      while (input.hasNextLine()) {
        String s = input.nextLine();
        String[] splitData = s.split("\\s+");
        if (splitData.length < 3) {
          break;
        }
        String deviceId = splitData[0];
        Long uniqueLbas = Long.parseLong(splitData[1]);
        Long maxLba = Long.parseLong(splitData[2]);
        volumeWSS.put(deviceId, uniqueLbas);
        volumeMaxLba.put(deviceId, maxLba);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
