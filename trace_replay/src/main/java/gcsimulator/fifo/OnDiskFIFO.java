package gcsimulator.fifo;

import gcsimulator.Configs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import java.util.Arrays;

public class OnDiskFIFO {
  static int globalId = 0;
  RandomAccessFile circularBuffer;
  ByteBuffer tailArray;
  ByteBuffer headArray;
  long capacity;
  int bufferSize = 4096 * 4; // 16K * 8B = 128KB buffer
  long currentLoad = -1L;
  long head = 0;
  long tail = 0;

  public OnDiskFIFO() {
    try {
      circularBuffer = new RandomAccessFile(Configs.outputPrefix + "ondiskfifo/fifo_" + globalId, "rw");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    globalId += 1;
    headArray = ByteBuffer.allocate(bufferSize * Long.BYTES); // Long
    tailArray = ByteBuffer.allocate(bufferSize * Long.BYTES); // Long
  }

  public void setSize(long size) {
    capacity = (size + bufferSize) / bufferSize * bufferSize; // align with buffer size
  }

  public void add(long key) {
    tailArray.putLong(key);
    tail += 1;
    if (tail % bufferSize == 0) {
      // if current tail buffer is filled, flush it.
      try {
        circularBuffer.getChannel().position((tail - bufferSize) * 8);
        circularBuffer.write(tailArray.array(), 0, bufferSize * 8);
        tailArray.clear();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if (tail == capacity) {
      tail = 0;
    }
  }

  public long removeFirst() {
    long headBlockId = head / bufferSize;
    long tailBlockId = tail / bufferSize;
    long value = 0;
    if (headBlockId == tailBlockId) {
      int pos = tailArray.position();
      // rewind because the content of asLongBuffer will follow the current position
      tailArray.rewind(); 
      value = tailArray.asLongBuffer().get(Math.toIntExact(head % bufferSize));
      tailArray.position(pos);
    } else {
      // If the values are not in mem, fetch it.
      if (currentLoad != headBlockId) {
        try {
          circularBuffer.getChannel().position(head / bufferSize * bufferSize * 8);
          circularBuffer.read(headArray.array(), 0, bufferSize * 8);
          headArray.rewind();
          currentLoad = headBlockId;
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      value = headArray.asLongBuffer().get(Math.toIntExact(head % bufferSize));
    }
    head += 1;
    if (head == capacity) {
      head = 0;
    }
    return value;
  }

  public long size() {
    return (tail + capacity - head) % capacity;
  }
}
