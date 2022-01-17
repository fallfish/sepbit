#ifndef LOGSTORE_SepBIT_H
#define LOGSTORE_SepBIT_H

#include "src/logstore/segment.h"
#include "src/indexmap/indexmap.h"
#include "src/indexmap/factory.h"
#include "src/placement/placement.h"
#include "src/placement/fifo.h"
#include "src/placement/metadata.h"

class SepBIT : public Placement {
  public:
    SepBIT();
    int  Classify(uint32_t blockAddr, bool isGcAppend) override;
    void Append(uint32_t blockAddr, uint64_t timestamp) override;
    void GcAppend(uint32_t blockAddr) override;
    void CollectSegment(Segment *segment) override;

  private:
    std::unique_ptr<Metadata> mMetadata;
    std::unique_ptr<FIFO> mLba2Fifo;

    double mAvgLifespan;
    uint64_t mClassNumOfLastCollectedSegment;
};

#endif //LOGSTORE_SepBIT_H
