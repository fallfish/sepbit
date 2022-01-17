package gcsimulator.indexmap;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.lang.ArrayIndexOutOfBoundsException;

class LargeNativeArray implements LargeArray {
  long size;
  long[][] buffers;
  final int ARRAY_MAX_SIZE = 128 * 1024 * 1024;

  public LargeNativeArray(long size) {
    long nArrays = (size + ARRAY_MAX_SIZE - 1) / ARRAY_MAX_SIZE;
    long lastArraySize = size % ARRAY_MAX_SIZE;
    if (lastArraySize == 0) lastArraySize = ARRAY_MAX_SIZE;
    buffers = new long[Math.toIntExact(nArrays)][];

    for (int i = 0; i < nArrays; ++i) {
      if (i == nArrays - 1) buffers[i] = new long[Math.toIntExact(lastArraySize)];
      else buffers[i] = new long[ARRAY_MAX_SIZE];
      Arrays.fill(buffers[i], -1L);
    }
  }

  @Override
  public void put(long index, long value) {
    long arrayId = index / ARRAY_MAX_SIZE;
    index = index % ARRAY_MAX_SIZE;

    buffers[Math.toIntExact(arrayId)][Math.toIntExact(index)] = value;
  }

  @Override
  public long get(long index) {
    long arrayId = index / ARRAY_MAX_SIZE;
    index = index % ARRAY_MAX_SIZE;

    long value = 0;
    try {
      value = buffers[Math.toIntExact(arrayId)][Math.toIntExact(index)];
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println(buffers.length);
      System.out.println("" + index + "" + arrayId + buffers[Math.toIntExact(arrayId)].length);
      throw e;
    }
    return value;
  }
}
