package gcsimulator;

import java.sql.Timestamp;

public class Statistics {
  private static Statistics instance = null;
  private long nIORequests;
  private long nGC;

  private long nLBAs;
  private long nTotalInvalidBlocks = 0;
  private long nTotalBlocks = 1;

  private double accumulatedGarbageProportion = 0.0;
  private long nRemovedSegments = 0;

  private Timestamp realStartTimestamp;

  public void incrementnIORequests() {
    nIORequests++;
  }

  public void incrementnGc() {
    nGC++;
  }

  public void addnLBAs(long nLBAs) {
    this.nLBAs += nLBAs;
  }

  public long getnIORequests() {
    return nIORequests;
  }

  public long getnLBAs() {
    return nLBAs;
  }

  public void setnTotalInvalidBlocks(long nTotalInvalidBlocks) {
    this.nTotalInvalidBlocks = nTotalInvalidBlocks;
  }

  public void setnTotalBlocks(long nTotalBlocks) {
    this.nTotalBlocks = nTotalBlocks;
  }

  public double getOverallGarbageProp() {
    return 1.0 * nTotalInvalidBlocks / nTotalBlocks;
  }

  public void addRemovedSegmentGP(double removedSegmentGP) {
    accumulatedGarbageProportion += removedSegmentGP;
    nRemovedSegments++;
  }

  public Timestamp getRealStartTimestamp() {
    return this.realStartTimestamp;
  }

  public void start() {
    this.realStartTimestamp = new Timestamp(System.currentTimeMillis());
  }

  public void summary() {
    System.out.println("SUMMARY: ");
    System.out.println("    Requests   : " + getnIORequests());
    System.out.println("    nGC     : " + nGC);
    System.out.println("  LBAs         : " + getnLBAs());
    System.out.format ("  bytes_to_System: %d\n" +
                       "  bytes_to_Storage: %d\n" +
                       "  ** WA **     : %f\n",
        Metadata.stat.nBytesWriteNormal,
        Metadata.stat.nBytesWriteToStorage,
        Metadata.stat.nBytesWriteToStorage * 1.0 /
            (Metadata.stat.nBytesWriteNormal == 0 ? 1 :
                Metadata.stat.nBytesWriteNormal)
    );
    System.out.println("  nBlocks:     : " + nTotalBlocks);
    System.out.println("  nInvalidBlks : " + nTotalInvalidBlocks);
    System.out.println("  garb prop    : " + getOverallGarbageProp());
    System.out.println(" removed avg gp: " + (1.0 * accumulatedGarbageProportion / nRemovedSegments));
    System.out.println(" ");

    Timestamp currentTime = new Timestamp(System.currentTimeMillis());
    System.out.println("\n" +
                       "  Run time(s)  : " + (currentTime.getTime() - realStartTimestamp.getTime()) * 1.0 / 1000);
  }

  private Statistics() {
    nIORequests = 0;
    nLBAs = 0;
  }

  public static Statistics getInstance() {
    if (instance == null) {
      instance = new Statistics();
    }
    return instance;
  }

}
