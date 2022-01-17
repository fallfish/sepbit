#ifndef LOGSTORE_ZENFS_ADAPTER_H
#define LOGSTORE_ZENFS_ADAPTER_H

#include <unordered_map>
#include <string>
#include <fstream>
#include <memory>
#include <rocksdb/file_system.h>
#include <rocksdb/plugin/zenfs/fs/fs_zenfs.h>

#include "src/storage_adapter/storage_adapter.h"

using namespace rocksdb;

class ZenFSAdapter : public StorageAdapter {
public:
    ZenFSAdapter();
    void CreateSegment(int) override;
    void Write(const void *, int, off64_t) override;
    void Read(void *, int, off64_t) override;
    void ReadWholeSegment(void *, int) override;
    void DestroySegment(int) override;

private:
    std::unordered_map<int, std::unique_ptr<rocksdb::FSSequentialFile>> mReadSegments;
    std::unordered_map<int, std::unique_ptr<rocksdb::FSWritableFile>> mWriteSegments;
    ZonedBlockDevice* mZbd;
    ZenFS* mZenFS;    
};

#endif //LOGSTORE_ZENFS_ADAPTER_H
