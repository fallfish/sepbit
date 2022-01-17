#include <string>
#include <iostream>
#include "src/storage_adapter/storage_adapter.h"
#include "src/storage_adapter/local_adapter.h"
#include "src/storage_adapter/zenfs_adapter.h"
//#include "src/storage_adapter/hdfs_adapter.h"
//#include "src/storage_adapter/zoned_adapter.h"

class StorageAdapterFactory {
  public:
    static StorageAdapter *GetInstance(std::string type) {
      if (type == "Local") {
        return new LocalAdapter();
      } else if (type == "ZenFS") {
        return new ZenFSAdapter();
      } else {
        std::cerr << "No StorageAdapter, type: " << type << std::endl;
      }
      return new LocalAdapter();
    }
};
