#ifndef LOGSTORE_HASHMAP_H
#define LOGSTORE_HASHMAP_H

#include <unordered_map>
#include <memory>
#include "src/indexmap/indexmap.h"

class HashMap : public IndexMap {
public:
    HashMap();
    uint64_t Query(uint32_t blockAddr) override;
    void Update(uint32_t blockAddr, uint64_t phyAddr) override;

private:
    std::unordered_map<uint32_t, uint64_t> mMap;
};

#endif //LOGSTORE_HASHMAP_H
