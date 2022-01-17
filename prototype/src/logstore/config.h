#ifndef LOGSTORE_CONFIG_H
#define LOGSTORE_CONFIG_H

#include <string>

class Config {
public:
  static Config& GetInstance() {
    static Config instance;
    return instance;
  }
  std::string selection = "CostBenefit";
  std::string indexMap = "Array";
  std::string storageAdapter = "ZenFS";
  std::string placement = "SepBIT";
  int         maxNumOpenSegments = 6;
  uint64_t    numValidBlocks = 0;

  // For zenfs and zoned storage backend
  std::string zbdName = "sdd";
  std::string zenFsAuxPath = "/tmp/aux_path";

  // For local file system backend
  std::string localAdapterDir = "/tmp/local";

  // For SepBIT
  std::string fifoDir = "/tmp/fifo";
  std::string metadataDir = "/tmp/metadata";
};

#endif
