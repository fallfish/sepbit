package gcsimulator.placement;

import gcsimulator.Log;
import gcsimulator.Configs;
import gcsimulator.indexmap.IndexMap;
import gcsimulator.indexmap.IndexMapFactory;

import java.util.Scanner;
import java.math.BigInteger;
import java.io.File;
import java.io.FileNotFoundException;

public class FK extends Separator {
  public IndexMap lastAccess;
  public IndexMap lifespans;
  public Scanner reader;

  public FK() {}

  @Override
  public void init(Log log, int numOpenSegments) {
    super.init(log, numOpenSegments);  

    lastAccess = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    lastAccess.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024));
    lifespans = IndexMapFactory.getInstance(Configs.indexMapType, Configs.indexMapCache);
    lifespans.setSize((Configs.volumeMaxLba.get(log.getId()) / 4096 + 1024));

    try {
      reader = new Scanner(new File(Configs.oraclePath + "/" + log.getId() + ".oracle"));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void append(long lba) {
    BigInteger tmp = reader.nextBigInteger(10);
    long lifespan = 0;
    if (tmp.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) == 1) {
      lifespan = -1;
    } else {
      lifespan = tmp.longValue();
    }
    if (lifespan == -1) lifespan = Long.MAX_VALUE - log.accessId;
    
    lastAccess.put(lba, log.accessId);
    lifespans.put(lba, lifespan);
  }

  @Override
  public int classify(boolean isGcAppend, long lba) {
    long remainingLifespan = 0;
    long level = 0;

    remainingLifespan = (lastAccess.get(lba) + lifespans.get(lba)) - log.accessId;
    level = remainingLifespan / Configs.getSegmentMaxLen();
    if (level >= numOpenSegments) level = numOpenSegments - 1;

    return (int)level;
  }
}
