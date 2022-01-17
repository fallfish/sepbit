#ifndef LOGSTORE_NO_PLACEMENT_H
#define LOGSTORE_NO_PLACEMENT_H

#include "src/logstore/segment.h"
#include "sys/types.h"
#include "src/placement/placement.h"

class NoPlacement : public Placement {
public:
   int  Classify(uint32_t blockAddr, bool isGcAppend) override;
   void Append(uint32_t blockAddr, uint64_t timestamp) override;
   void GcAppend(uint32_t blockAddr) override;
   void CollectSegment(Segment *segment) override;
};

#endif //LOGSTORE_NO_PLACEMENT_H
