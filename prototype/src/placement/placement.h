#ifndef LOGSTORE_PLACEMENT_H
#define LOGSTORE_PLACEMENT_H

#include "src/logstore/segment.h"
class Placement {
public:
    virtual int  Classify(uint32_t, bool) = 0;
    virtual void Append(uint32_t addr, uint64_t timestamp) = 0;
    virtual void GcAppend(uint32_t addr) = 0;
    virtual void CollectSegment(Segment *segment) = 0;
};

#endif //LOGSTORE_PLACEMENT_H
