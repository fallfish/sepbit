#ifndef LOGSTORE_WARCIP_H
#define LOGSTORE_WARCIP_H

#include <vector>
#include <set>

#include "src/logstore/segment.h"
#include "src/indexmap/indexmap.h"
#include "src/indexmap/factory.h"
#include "src/placement/placement.h"


class WARCIP : public Placement {
  public:
    WARCIP();
    int  Classify(uint32_t blockAddr, bool isGcAppend) override;
    void Append(uint32_t blockAddr, uint64_t timestamp) override;
    void GcAppend(uint32_t blockAddr) override;
    void CollectSegment(Segment *segment) override;

  private:
    void closeAndReassignCenter(int classNum);
    void dynamicSplitAndMerge();  
    void split(int classNum);  
    void merge(int classNum);  


    std::vector<double>       mCenterOfClusters;
    std::vector<double>       mTotalWeightOfClusters;
    std::vector<uint64_t>     mTotalBlocksOfClusters;
    std::vector<uint64_t>     mTotalWritesOfClusters;
    std::unique_ptr<IndexMap> mLastWriteTimestamps;
    std::unique_ptr<IndexMap> mPenalty;

    std::set<int>             mNextMerge;

    uint64_t mRwi = 0;
    int      mTotalSealedUserSegments = 0;
    uint64_t mTotalWritesAllClusters = 0;
};

#endif //LOGSTORE_WARCIP_H
