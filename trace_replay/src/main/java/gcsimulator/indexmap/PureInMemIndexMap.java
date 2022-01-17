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
 */
public class PureInMemIndexMap implements IndexMap {
  private LargeArray map;
  private long size;

  public PureInMemIndexMap() {}

  public void setSize(long size) {
    this.size = size;
    map = new LargeNativeArray(size);
  }

  @Override
  public void put(long key, long value) {
    map.put(key, value);
  }

  @Override
  public long get(long key) {
    return map.get(key);
  }

  @Override
  public boolean containsKey(long key) {
    return (map.get(key) != -1L);
  }

  @Override
  public long size() {
    return 0;
  }

}
