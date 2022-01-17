#ifndef LOGSTORE_INDEXMAP_H
#define LOGSTORE_INDEXMAP_H

#include <cstdint>

class IndexMap {

public:
    virtual uint64_t Query(uint32_t blockAddr) = 0;
    virtual void Update(uint32_t blockAddr, uint64_t phyAddr) = 0;

    IndexMap() = default;
};


#endif //LOGSTORE_INDEXMAP_H
