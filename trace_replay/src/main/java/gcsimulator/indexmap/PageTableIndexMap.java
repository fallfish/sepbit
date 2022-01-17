package gcsimulator.indexmap;

import org.apache.commons.math3.util.Pair;

import java.lang.reflect.Method;
import java.util.Collection;

public class PageTableIndexMap implements IndexMap {

  class PageDirectory<T> {
    T[] entries;
  }

  PageDirectory directory;
  int nLevels = 1;
  int nEntriesPerPageTable = 2048;
  long size;

  public PageTableIndexMap() {

  }

  PageDirectory createPageTable(boolean isDirectory) {
    PageDirectory pd = new PageDirectory<>();
    if (isDirectory) {
      pd.entries = new PageDirectory[nEntriesPerPageTable];
    } else {
      pd.entries = new Long[nEntriesPerPageTable];
    }
    return pd;
  }

  @Override
  public void setSize(long size) {
    this.size = size;
    while (size > nEntriesPerPageTable) {
      size /= nEntriesPerPageTable;
      this.nLevels += 1;
    }
    directory = new PageDirectory<>();
    directory.entries = new PageDirectory[(int) size];
  }

  @Override
  public void put(long key, long value) {
    int[] offsets = getOffsets(key);
    PageDirectory pd = directory;
    for (int i = 0; i < nLevels; ++i) {
      if (i != nLevels - 1) {
        if (pd.entries[offsets[i]] == null) {
          pd.entries[offsets[i]] = createPageTable(i != nLevels - 2);
        }
        pd = (PageDirectory) pd.entries[offsets[i]];
      } else {
        pd.entries[offsets[i]] = value;
      }
    }
  }

  @Override
  public long get(long key) {
    int[] offsets = getOffsets(key);
    PageDirectory pd = directory;
    Long value = null;
    for (int i = 0; i < nLevels; ++i) {
      if (i == nLevels - 1) {
        value = (Long) pd.entries[offsets[i]];
      } else {
        pd = (PageDirectory) pd.entries[offsets[i]];
      }
    }
    return value;
  }

  @Override
  public boolean containsKey(long key) {
    int[] offsets = getOffsets(key);
    PageDirectory pd = directory;
    for (int i = 0; i < nLevels; ++i) {
      if (pd.entries[offsets[i]] == null)
        return false;
      if (i != nLevels - 1)
        pd = (PageDirectory) pd.entries[offsets[i]];
    }

    return true;
  }

  @Override
  public long size() {
    return 0;
  }

  private int[] getOffsets(long key) {
    int[] offsets = new int[nLevels];
    for (int i = nLevels - 1; i >= 0; --i) {
      offsets[i] = (int) (key % nEntriesPerPageTable);
      key /= nEntriesPerPageTable;
    }

    return offsets;
  }
}
