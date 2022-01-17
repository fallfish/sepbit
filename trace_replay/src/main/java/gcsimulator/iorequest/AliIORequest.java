package gcsimulator.iorequest;

import gcsimulator.Log;

import static gcsimulator.Configs.BLOCK_SIZE;

public class AliIORequest extends IORequest {
  private void convertToRequest(String requestString) {
    String[] strs = requestString.split(",");
    this.logId = strs[0];
    this.write = strs[1].equals("W");
    this.LBA = Long.parseLong(strs[2]);
    this.length = Long.parseLong(strs[3]);
    this.timestampInUs = Long.parseLong(strs[4]);

    long begin = this.LBA;
    long end = this.LBA + length;
    this.LBA = begin / BLOCK_SIZE;
    this.length = ((end + BLOCK_SIZE - 1) / BLOCK_SIZE - this.LBA);
  }

  public AliIORequest() {
    this.timestampInUs = 0;
  }

  public AliIORequest(String requestString) {
    this.timestampInUs = 0;
    init(requestString);
  }

  public void init(String requestString) {
    this.str = requestString;
    setFromRequestString(requestString);
  }

  public void setFromRequestString(String requestString) {
    convertToRequest(requestString);
  }
}
