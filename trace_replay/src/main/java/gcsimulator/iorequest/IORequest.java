package gcsimulator.iorequest;

import gcsimulator.Log;

public abstract class IORequest {
    public String logId = "";
    public long timestampInUs;
    public String str = "";
    long LBA;
    long length;
    boolean write;

    IORequest() {}

    public abstract void setFromRequestString(String input);

    @Override
    public String toString() {
      return "IORequest{" +
          "logId=" + logId +
          ", LBA=" + LBA +
          ", length=" + length +
          ", time=" + timestampInUs +
          ", r/w=" + (write ? "W" : "R");
    }

    public boolean isWrite() {
        return write;
    }

    public long getLBA() {
        return LBA;
    }

    public long getLength() {
        return length;
    }

    public String getLogId() {
        return logId;
    }
}
