// Extent-based temperature identification hotstorage 16
package gcsimulator.placement;

import gcsimulator.Log;
import gcsimulator.Configs;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.TreeMap;
import java.util.Map;

public class ETI extends Separator {
  TreeMap<Long, Pair<Long, Long>> extents;
  long numRequests = 0;
  long maxNumExtents = 1024;

  public ETI() {}

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);
    extents = new TreeMap<Long, Pair<Long, Long>>();
    extents.put(0L, new ImmutablePair(Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024, 0L));
  }

  public void decay() {
    TreeMap<Long, Pair<Long, Long>> newExtents = new TreeMap<Long, Pair<Long, Long>>();
    long off = 0;
    long len = 0;
    long lastCount = -1;
    for (Map.Entry<Long, Pair<Long, Long>> extent : extents.entrySet()) {
      long extOff = extent.getKey();
      long extLen = extent.getValue().getKey();
      long extCount = extent.getValue().getValue() / 2;
      if (lastCount == -1) {
        off = extOff;
        len = extLen;
        lastCount = extCount;
        continue;
      }

      if ( (lastCount >= 4 && lastCount == extCount) || (lastCount < 4 && extCount < 4) ) {
        len += extLen;
      } else {
        newExtents.put(off, new ImmutablePair(len, lastCount));

        off = extOff;
        len = extLen;
        lastCount = extCount;
      }
    }
    newExtents.put(off, new ImmutablePair(len, lastCount));
    extents = newExtents;
  }

  @Override
  public void addRequest(long lba, long length) {
    numRequests += 1;
    if (numRequests >= 4096) {
      decay();
      numRequests = 0;
    }

    // match
    long off = lba;
    while (off < lba + length) {
      Map.Entry<Long, Pair<Long, Long>> extent = extents.floorEntry(off);
      //System.out.println(off + " " + lba + " " + length);

      long extOff = extent.getKey();
      long extLen = extent.getValue().getKey();
      long extCount = extent.getValue().getValue();
      long len = lba + length - off;
      if (extCount == 15) {
        off = extOff + extLen;
        continue;
      }
      //System.out.println(extOff + " " + extLen + " " + extCount);

      if (extOff == off && len >= extLen) {
        // case 1: match
        extents.put(extOff, new ImmutablePair(extLen, extCount + 1));
      } else if (extOff == off && len < extLen) {
        // case 2: match the left half part
        // | match | --------------- |
        // extOff                    extOff+extLen
        if (extents.size() < maxNumExtents) {
          extents.put(extOff, new ImmutablePair(len, extCount + 1));
          extents.put(extOff + len, new ImmutablePair(extLen - len, extCount));
        }
      } else if (extOff < off && off + len >= extOff + extLen) {
        // case 2: match the right half part
        // | ------- | match |
        // extOff   off     extOff+extLen
        if (extents.size() < maxNumExtents) {
          extents.put(extOff, new ImmutablePair(off - extOff, extCount));
          extents.put(off, new ImmutablePair(extOff + extLen - off, extCount + 1));
        }
      } else if (extOff < off && off + len < extOff + extLen) {
        // case 2: match the right half part
        // | ------- | match | ------------ |
        // extOff   off     off+len       extOff+extLen
        if (extents.size() < maxNumExtents) {
          extents.put(extOff, new ImmutablePair(off - extOff, extCount));
          extents.put(off, new ImmutablePair(len, extCount + 1));
          extents.put(off + len, new ImmutablePair(extOff + extLen - (off + len), extCount));
        }
      }
      off = extOff + extLen;
    }
  }


  @Override
  public int classify(boolean isGcAppend, long lba) {
    if (isGcAppend) { // gcAppend to an individual class
      return 2;
    } else if (extents.floorEntry(lba).getValue().getValue() >= 4) { // normal append classify
      return 0;
    } else {
      return 1;
    }
  }
}
