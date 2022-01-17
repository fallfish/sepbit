#ifndef LOGSTORE_LOCAL_CONNECTOR_H
#define LOGSTORE_LOCAL_CONNECTOR_H

#include <unordered_map>
#include <string>
#include <fstream>
#include "src/logstore/config.h"
#include "src/storage_adapter/storage_adapter.h"

class LocalAdapter : public StorageAdapter {
    void CreateSegment(int) override;
    void Write(const void *, int, off64_t) override;
    void Read(void *, int, off64_t) override;
    void ReadWholeSegment(void *, int) override;
    void DestroySegment(int) override;

private:
    std::unordered_map<int, std::fstream> mSegments;
    const std::string mDirPrefix = Config::GetInstance().localAdapterDir.c_str();
};

#endif //LOGSTORE_LOCAL_CONNECTOR_H
