/*
 * AutoStream.systor17: MultiQueue;
 * Only work on user writes rather than gc writes.
 * 1. 
 */
package gcsimulator.placement;

import gcsimulator.indexmap.*;
import gcsimulator.Log;
import gcsimulator.Segment;
import gcsimulator.Configs;
import gcsimulator.Simulator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Iterator;

public class MultiQueue extends Separator {
  public long devLifetime = 0L;
  public long devMaxWrites = 0L;
  public long devTotalWrites = 0L; // per request
  public long chunkSize = 256L; // 1MiB
  public long hottestChunkId = -1L;
  public IndexMap chunkLastAccess;
  public IndexMap chunkExpireTime;
  public IndexMap chunkWrites;
  public IndexMap chunkQueue;
  public ArrayList<LinkedHashSet<Long>> queues;
  public int currentQueueId;

  public MultiQueue() {}

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);

    chunkLastAccess = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    chunkLastAccess.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024) / chunkSize);
    chunkExpireTime = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    chunkExpireTime.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024) / chunkSize);
    chunkWrites = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    chunkWrites.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024) / chunkSize);
    chunkQueue = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    chunkQueue.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024) / chunkSize);

    queues = new ArrayList<>();
    for (int i = 0; i < numOpenSegments; ++i) {
      queues.add(new LinkedHashSet<Long>());
    }
  }

  @Override
  public void collectSegment(Segment segment) {
    System.out.println("devLifetime: " + devLifetime + ", devMaxWrites: " + devMaxWrites + ", devTotalWrites: " + devTotalWrites);
    super.collectSegment(segment);
  }

  public void promote(long chunkId) {
    devTotalWrites += 1;
    long numWrites, lastAccess;
    int prevQueueId, queueId;

    if (chunkWrites.containsKey(chunkId)) {
      numWrites = chunkWrites.get(chunkId) + 1;
      lastAccess = chunkLastAccess.get(chunkId);
      prevQueueId = Math.toIntExact(chunkQueue.get(chunkId));
      queueId = (int)Math.log((double)numWrites);
      if (queueId >= numOpenSegments - 1) queueId = numOpenSegments - 2;
    } else {
      numWrites = 1;
      lastAccess = 0;
      prevQueueId = -1;
      queueId = 0;
    }

    if (numWrites > devMaxWrites) {
      hottestChunkId = chunkId;
      devLifetime = devTotalWrites - lastAccess;
      devMaxWrites = numWrites;
    }

    if (prevQueueId != -1) {
      queues.get(prevQueueId).remove(chunkId);
    }
    queues.get(queueId).add(chunkId);

    chunkWrites.put(chunkId, numWrites);
    chunkLastAccess.put(chunkId, devTotalWrites);
    chunkExpireTime.put(chunkId, devTotalWrites + devLifetime);
    chunkQueue.put(chunkId, queueId);
  }

  public void demote() {
    for (int i = 0; i < queues.size(); ++i) {
      if (queues.get(i).size() == 0) continue;

      LinkedHashSet<Long> queue = queues.get(i);
      Iterator<Long> lru = queue.iterator();
      long chunkId = lru.next();
      long expireTime = chunkExpireTime.get(chunkId);
      if (expireTime < devTotalWrites) {
        chunkExpireTime.put(chunkId, devTotalWrites + devLifetime);
        lru.remove();

        if (chunkId == hottestChunkId && lru.hasNext()) {
          long tmpChunkId = lru.next();
          devMaxWrites = chunkWrites.get(tmpChunkId);
          hottestChunkId = tmpChunkId;
        }

        if (i == 0) {
          queues.get(i).add(chunkId);
        } else {
          queues.get(i - 1).add(chunkId);
        }
      }
    }
  }

  @Override
  public void addRequest(long lba, long length) {
    long chunkId = lba / chunkSize;
    for ( ; chunkId * chunkSize < lba + length; chunkId += 1) {
      promote(chunkId);
      demote();
    }
  }

  @Override
  public int classify(boolean isGcAppend, long lba) {
    if (isGcAppend) {
      return numOpenSegments - 1;
    } else {
      return Math.toIntExact(chunkQueue.get(lba / chunkSize));
    }
  }

  @Override
  public void shutdown() {
    System.out.println("devLifetime: " + devLifetime + ", devMaxWrites: " + devMaxWrites + ", devTotalWrites: " + devTotalWrites);
    super.shutdown();
  }
}
