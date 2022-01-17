#ifndef LOGSTORE_DAC_H
#define LOGSTORE_DAC_H

#include "src/logstore/segment.h"
#include "src/indexmap/indexmap.h"
#include "src/indexmap/factory.h"
#include "src/placement/placement.h"

class DAC : public Placement {
  public:
    DAC();
    int  Classify(uint32_t blockAddr, bool isGcAppend) override;
    void Append(uint32_t blockAddr, uint64_t timestamp) override;
    void GcAppend(uint32_t blockAddr) override;
    void CollectSegment(Segment *segment) override;

    std::unique_ptr<IndexMap> mTemperatureMap;
};

#endif //LOGSTORE_DAC_H
