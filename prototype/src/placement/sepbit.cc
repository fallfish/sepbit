#include <iostream>
#include <cfloat>
#include "src/placement/sepbit.h"
#include "src/logstore/segment.h"
#include "src/logstore/manager.h"

SepBIT::SepBIT() {
  mMetadata.reset(new Metadata());
  mLba2Fifo.reset(new FIFO());
  mAvgLifespan = DBL_MAX;
}

int SepBIT::Classify(uint32_t blockAddr, bool isGcAppend) {
  if (!isGcAppend) {
    uint64_t lifespan = mLba2Fifo->Query(blockAddr);
    if (lifespan != UINT32_MAX && lifespan < mAvgLifespan) {
      return 0;
    } else {
      return 1;
    }
  } else {
    if (mClassNumOfLastCollectedSegment == 0) {
      return 2;
    } else {
      uint64_t age = Manager::globalTimestamp - mMetadata->Query(blockAddr);
      if (age < 4 * mAvgLifespan) {
        return 3;
      } else if (age < 16 * mAvgLifespan) {
        return 4;
      } else {
        return 5;
      }
    }
  }
}

void SepBIT::CollectSegment(Segment *segment) {
  static int totLifespan = 0;
  static int nCollects = 0;
  if (segment->GetClassNum() == 0) {
    totLifespan += segment->GetAge();
    nCollects += 1;
  }
  if (nCollects == 16) {
    mAvgLifespan = 1.0 * totLifespan / nCollects;
    nCollects = 0;
    totLifespan = 0;
    std::cout << "AvgLifespan: " << mAvgLifespan << std::endl;
  }

  mClassNumOfLastCollectedSegment = segment->GetClassNum();
}

void SepBIT::Append(uint32_t blockAddr, uint64_t timestamp) {
  mLba2Fifo->Update(blockAddr, mAvgLifespan);
  mMetadata->Update(blockAddr, Manager::globalTimestamp);
}

void SepBIT::GcAppend(uint32_t blockAddr) {
}
