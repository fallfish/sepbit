package gcsimulator.segment;

import gcsimulator.Configs;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ClassNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;

public class SegmentMeta {
  // store lbas
  private RandomAccessFile lbasFile;
  private long[] lbas;
  private ArrayList<Long> tmpArr = new ArrayList<>();
  private int posLbas = 0;

  private RandomAccessFile invalidLbasFile;
  private long[] invalidatedOffsets;
  private int posInvalidatedOffsets = 0;
  // private ArrayList<Long> invalidatedOffsets;

  // store additional information for each LBA
  private RandomAccessFile infoFile;
  private long[] infos;
  private int posInfoArray = 0;
  private int nInfos = 0;
  private int nLongPerInfo = 2;

  // 
  public double totalWriteCount = 0;
  public double totalLastModifiedTime = 0;
  public double seqRatio = 0;


  private String fileName;

  private static Map<Long, long[]> validPairs;
  private static ByteBuffer buf;
  private static long[] globalLbas;
  private static long[] globalInvalidatedOffsets;
  private static long[] globalInfos;

  public long segmentId;
  public int temperature;
  public boolean isSealed = false;
  public int nBlocks = 0;
  public int nInvalidBlocks = 0;
  public long createdAccessId = 0;
  public long sealedAccessId = 0;
  public long firstEvitcionAccessId = 0;
  public long timestampCreatedInUs;
  public long timestampModifiedInUs;

  private boolean inMemory = false;
  private int flushThreshold = 2048;

  public SegmentMeta(String logId, long id, long timestamp) {
    File dir = new File(Configs.outputPrefix + "segments/" + logId);
    boolean bool = dir.mkdir();
     
    this.fileName = "segments/" + logId + "/" + id;
    this.segmentId = id;
    this.timestampCreatedInUs = timestamp;
    try {
      lbasFile = new RandomAccessFile(Configs.outputPrefix + fileName + ".lbas", "rw");
      invalidLbasFile = new RandomAccessFile(Configs.outputPrefix + fileName + ".invalidlbas", "rw");
      infoFile = new RandomAccessFile(Configs.outputPrefix + fileName + ".info", "rw");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    
    lbas = new long[(int)Configs.getSegmentMaxLen()];
    if (!inMemory) {
      invalidatedOffsets = new long[flushThreshold];
      infos = new long[flushThreshold];
    } else {
      invalidatedOffsets = new long[(int)Configs.getSegmentMaxLen()];
    }

    if (buf == null) {
      validPairs = new LinkedHashMap<Long, long[]>((int)Configs.getSegmentMaxLen(), 0.75f);
      buf = ByteBuffer.allocate(Long.BYTES * (int)Configs.getSegmentMaxLen() * 2);
      globalLbas = new long[(int)Configs.getSegmentMaxLen()];
      globalInvalidatedOffsets = new long[(int)Configs.getSegmentMaxLen()];
      globalInfos = new long[(int)Configs.getSegmentMaxLen() * 2];
    }
  }

  public long append(long lba, long timestamp) {
    long offset = segmentId * Configs.getSegmentMaxLen() + nBlocks;

    if (lbas == null) {
      System.out.println(fileName + " " + lba + " " + nBlocks);
    }
    lbas[posLbas++] = lba;
    nBlocks += 1;
    timestampModifiedInUs = timestamp;

    return offset;
  }

  public void appendInfo(long[] info) {
    nInfos += 1;
    for (int i = 0; i < info.length; ++i) {
      infos[posInfoArray++] = info[i];
    }
    if (!inMemory && (posInfoArray >= flushThreshold)) {
      writeToFile(infoFile, buf, posInfoArray, infos);
      posInfoArray = 0;
    }
  }

  public void invalidate(long offset) {
    nInvalidBlocks += 1;

    invalidatedOffsets[posInvalidatedOffsets++] = offset;
    if (!inMemory && (posInvalidatedOffsets >= flushThreshold)) {
      writeToFile(invalidLbasFile, buf, posInvalidatedOffsets, invalidatedOffsets);
      posInvalidatedOffsets = 0;
    }
  }

  public void seal() {
    isSealed = true;

    if (!inMemory) {
      writeToFile(lbasFile, buf, posLbas, lbas);
      writeToFile(infoFile, buf, posInfoArray, infos);
      {
        Arrays.sort(lbas);
        int cnt = 0;
        for (int i = 1; i < lbas.length; ++i) {
          if (lbas[i] == lbas[i - 1] + 1) {
            cnt += 1;
          }
        }
        seqRatio = (double)cnt / lbas.length;
      }

      posLbas = 0;
      posInfoArray = 0;
      lbas = null;
      infos = null;
    }

    System.out.println("seal: " + fileName + " temperature = " + temperature);
  }


  public void destroy() {
    try {
      lbasFile.close();
      invalidLbasFile.close();
      infoFile.close();

      File file = new File(Configs.outputPrefix + fileName + ".lbas");
      file.delete();
      file = new File(Configs.outputPrefix + fileName + ".invalidlbas");
      file.delete();
      file = new File(Configs.outputPrefix + fileName + ".info");
      file.delete();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void summarize() {
    if (!inMemory) {
      // read in lbas and invalidated offsets
      System.out.println(fileName);
      System.out.println("Total blocks: " + nBlocks);
      System.out.println("Total invalid blocks: " + nInvalidBlocks);

      try {
        lbasFile.seek(0);
        invalidLbasFile.seek(0);
        infoFile.seek(0);
      } catch (IOException e) {
        e.printStackTrace();
      }

      readFromFile(lbasFile, buf, nBlocks, globalLbas);
      readFromFile(invalidLbasFile, buf, nInvalidBlocks - posInvalidatedOffsets, globalInvalidatedOffsets);

      for (int i = 0; i < posInvalidatedOffsets; ++i) {
        globalInvalidatedOffsets[Math.toIntExact(nInvalidBlocks - posInvalidatedOffsets + i)] = invalidatedOffsets[i];
      }

      if (nInfos != 0) {
        readFromFile(infoFile, buf, nInfos * nLongPerInfo, globalInfos);
      }
    } else {
      globalLbas = lbas;
      globalInvalidatedOffsets = invalidatedOffsets;
      globalInfos = infos;
    }

    for (int i = 0; i < nInvalidBlocks; ++i) {
      long globalInvalidatedOffset = globalInvalidatedOffsets[i];
      globalLbas[Math.toIntExact(globalInvalidatedOffset)] = -1L;
    }

    validPairs.clear();
    for (int i = 0; i < nBlocks; ++i) {
      if (globalLbas[i] != -1L) {
        long[] tmp = null;
        if (globalInfos != null) {
          tmp = new long[nLongPerInfo];
          for (int j = 0; j < nLongPerInfo; ++j) {
            tmp[j] = globalInfos[i * nLongPerInfo + j];
          }
        }
        validPairs.put(globalLbas[i], tmp);
      }
    }

    System.out.println("validPairs size: " + validPairs.size());
  }


  // When doing GC, we want to know the valid blocks in the recycled segment so that we can rewrite them
  public Collection<Long> getValidLbas() {
    return validPairs.keySet();
  }

  public Map<Long, long[]> getValidPairs() {
    return validPairs;
  }

  public String toString() {
    return "nBlocks: " + nBlocks + ", nInvalidBlocks: " + nInvalidBlocks;
  }

  private void writeToFile(RandomAccessFile file, ByteBuffer buffer, int num, long[] arr) {
    buffer.rewind();
    buffer.asLongBuffer().put(arr, 0, num);
    try {
      file.write(buffer.array(), 0, num * Long.BYTES);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void readFromFile(RandomAccessFile file, ByteBuffer buffer, int num, long[] arr) {
    try {
      buffer.rewind();
      file.read(buffer.array(), 0, num * Long.BYTES);
      buffer.asLongBuffer().get(arr, 0, num);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
