package gcsimulator.indexmap;

import org.apache.commons.math3.util.Pair;

import gcsimulator.Configs;

import java.lang.reflect.Method;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;

/*
 * This file is a page-level cache based on a persistent index map structure.
 * By saying page-level cache, we mean that the cached items are disk-page aligned
 * size, e.g., 4KiB, which contains 512 8Bytes entries.
 */
public class IndexMapWithGlobalPageCache implements IndexMap {
  static ArrayList<Integer[]> cache;
  static ArrayList<IndexMap> disks;
  static LinkedHashMap<Long, Integer> lruList;
  static ArrayList<long[]> prealloctedPages;
  static LinkedList<Integer> availablePages;

  static long capacity = 0;
  static int pageSize = 512;
  static long numMisses = 0;

  long nElements = 0;

  // return the id
  public static int addVolume(IndexMap disk, int numPages, int numCachedPages) {
    if (cache == null) {
      cache = new ArrayList<>();
      disks = new ArrayList<>();
      lruList = new LinkedHashMap<Long, Integer>(8192, 0.5f, true);
      prealloctedPages = new ArrayList<>();
      availablePages = new LinkedList<>();
    }
    cache.add(new Integer[numPages]);
    disks.add(disk);

    for (int i = 0; i < numCachedPages; ++i) {
      long[] tmp = new long[pageSize];
      prealloctedPages.add(tmp);
      availablePages.add(prealloctedPages.size() - 1);
    }

    capacity += numCachedPages;
    System.out.println("Capacity: " + capacity);

    return disks.size() - 1;
  }

  public static Page getPageInternal(int id, int offset) {
    Page page = new Page();

    if (cache.get(id)[offset] == null || cache.get(id)[offset] == -1) {
      numMisses ++;
      if (numMisses % 1000000 == 0) {
        System.out.println("Number of misses: " + numMisses);
      }

      if (lruList.size() >= capacity) {
        Iterator<Map.Entry<Long, Integer>> lru = lruList.entrySet().iterator();
        long removedKey = lru.next().getKey();
        int removedId = Math.toIntExact(removedKey >> 32);
        int removedOffset = Math.toIntExact(removedKey & 0xffffffffL);
        int pageId = cache.get(removedId)[removedOffset];
        ((PersistentIndexMap)disks.get(removedId)).putBulk(removedOffset * pageSize, prealloctedPages.get(pageId));
        lru.remove();
        cache.get(removedId)[removedOffset] = -1;

        availablePages.add(pageId);
      }

      int newPageId = availablePages.get(0);
      availablePages.removeFirst();
      if (cache.get(id)[offset] == null) {
        for (int i = 0; i < pageSize; ++i) {
          prealloctedPages.get(newPageId)[i] = -1L;
        }
      } else {
        ((PersistentIndexMap)disks.get(id)).getBulk(
            offset * pageSize, Math.toIntExact(pageSize),
            prealloctedPages.get(newPageId)
            );
      }
      cache.get(id)[offset] = newPageId;

      lruList.put(((long)id << 32) | (offset & 0xffffffffL), newPageId);
    }

    page.entries = prealloctedPages.get(cache.get(id)[offset]);

    return page;
  }

  IndexMap disk;
  int id;

  public IndexMapWithGlobalPageCache(IndexMap m) {
    disk = m;
  }

  @Override
  public void setSize(long size) {
    size = (size + pageSize - 1) / pageSize * pageSize;
    disk.setSize(size);
    int numPages = Math.toIntExact(size / pageSize); // 118TiB 
    int numCachedPages = Math.toIntExact((size / pageSize + 32) / 16); // 118TiB / 64
    id = addVolume(disk, numPages, numCachedPages);
  }

  Page getPage(int offset) {
    return getPageInternal(id, offset);
  }

  @Override
  public void put(long key, long value) {
    int offset = Math.toIntExact(key / pageSize);
    int remainder = Math.toIntExact(key % pageSize);
    Page page = getPage(offset);

    if (page.entries[remainder] == -1L && value != -1L) {
      nElements += 1;
    } else if (page.entries[remainder] != -1L && value == -1L) {
      nElements -= 1;
    }

    page.entries[remainder] = value;
  }

  @Override
  public long get(long key) {
    int offset = Math.toIntExact(key / pageSize);
    int remainder = Math.toIntExact(key % pageSize);
    Page page = getPage(offset);

    return page.entries[remainder];
  }

  @Override
  public boolean containsKey(long key) {
    int offset = Math.toIntExact(key / pageSize);
    int remainder = Math.toIntExact(key % pageSize);
    Page page = getPage(offset);

    return page.entries[remainder] != -1L;
  }

  @Override
  public long size() {
    return nElements;
  }
}
