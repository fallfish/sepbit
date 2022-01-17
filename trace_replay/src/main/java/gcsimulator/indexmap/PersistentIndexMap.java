package gcsimulator.indexmap;

import gcsimulator.Configs;
import org.apache.commons.math3.util.Pair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/*
  This class implements an on-disk index data structure
 */
public class PersistentIndexMap implements IndexMap {
  static int globalId = 0;
  RandomAccessFile map;
  ByteBuffer buffer;
  ByteBuffer pageBuffer;
  long maxSize;

  public PersistentIndexMap() {
    try {
      map = new RandomAccessFile(Configs.outputPrefix + "indexmap/persistent_indexmap_" + globalId, "rw");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    globalId += 1;
    buffer = ByteBuffer.allocate(Long.BYTES); // Long
    pageBuffer = ByteBuffer.allocate(512 * Long.BYTES); // Long
  }

  public void setSize(long size) {
    maxSize = size;
    try {
      ByteBuffer value = ByteBuffer.allocate(1024 * 1024 * 8);
      for (int i = 0; i < 1024 * 1024; ++i) {
        value.putLong(-1L);
      }
      for (int i = 0; i < size; i += 1024 * 1024) {
        map.write(value.array(), 0, (int)1024 * 1024 * 8);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void put(long key, long value) {
    buffer.putLong(0, value);
    try {
      map.getChannel().position(key * 8);
      map.write(buffer.array(), 0, 8);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean containsKey(long key) {
    try {
      buffer.rewind();
      map.getChannel().position(key * 8);
      map.read(buffer.array(), 0, 8);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return (buffer.asLongBuffer().get(0) != -1L);
  }

  @Override
  public long get(long key) {
    try {
      map.getChannel().position(key * 8);
      map.read(buffer.array(), 0, 8);
    } catch (IOException e) {
      e.printStackTrace();
    }
    pageBuffer.rewind();
    return buffer.asLongBuffer().get(0);
  }

  @Override
  public long size() {
    return 0;
  }

  public void putBulk(long key, long[] values) {
    pageBuffer.rewind();
    pageBuffer.asLongBuffer().put(values);
    try {
      map.getChannel().position(key * Long.BYTES);
      map.write(pageBuffer.array(), 0, values.length * Long.BYTES);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public long[] getBulk(long key, int length, long[] values) {
    try {
      map.getChannel().position(key * Long.BYTES);
      map.read(pageBuffer.array(), 0, length * Long.BYTES);
    } catch (IOException e) {
      e.printStackTrace();
    }
    pageBuffer.rewind();
    pageBuffer.asLongBuffer().get(values);

    return values;
  }

}
