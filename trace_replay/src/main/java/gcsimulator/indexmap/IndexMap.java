package gcsimulator.indexmap;

import org.apache.commons.math3.util.Pair;

import java.lang.reflect.Method;
import java.util.Collection;

public interface IndexMap {
  void setSize(long size);
  void put(long key, long value);
  long get(long key);
  boolean containsKey(long key);
  long size();
}
