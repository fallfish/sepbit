package gcsimulator;

public interface BlockContainer {
  long getnValidBlocks();
  long getnInvalidBlocks();
  long getnBlocks();
  double getGarbageProportion();
  double getAge();
  double getLastAccessedTime();
}
