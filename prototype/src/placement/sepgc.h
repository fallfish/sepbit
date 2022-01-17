#include "src/logstore/segment.h"
#include "sys/types.h"
#include "src/placement/placement.h"

class SepGC : public Placement {
public:
   int  Classify(uint32_t addr, bool isGcAppend) override;
   void Append(uint32_t addr, uint64_t timestamp) override;
   void GcAppend(uint32_t addr) override;
   void CollectSegment(Segment *segment) override;
};

