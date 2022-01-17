#include <cassert>
#include <cmath>
#include <cfloat>
#include <algorithm>

#include "src/logstore/manager.h"
#include "src/logstore/config.h"
#include "src/placement/warcip.h"

/*
 * Clusters
 * (1) 0; (2) 100; (3) 1000; (4) 10000; ...
 */
WARCIP::WARCIP()
{
  int maxNumOpenSegments = Config::GetInstance().maxNumOpenSegments;
  mLastWriteTimestamps = std::unique_ptr<IndexMap>(IndexMapFactory::GetInstance("HashMap"));
  mPenalty = std::unique_ptr<IndexMap>(IndexMapFactory::GetInstance("HashMap"));
  mRwi = 0;
  mCenterOfClusters.resize(maxNumOpenSegments);
  mTotalWeightOfClusters.resize(maxNumOpenSegments);
  mTotalBlocksOfClusters.resize(maxNumOpenSegments);
  mTotalWritesOfClusters.resize(maxNumOpenSegments);
  for (int i = 0; i < maxNumOpenSegments; ++i) {
    mCenterOfClusters[i] = (i == 0) ? -DBL_MAX : 0;
    mTotalWeightOfClusters[i] = 0.0;
    mTotalBlocksOfClusters[i] = 0;
    mTotalWritesOfClusters[i] = 0;
  }
  mTotalWritesAllClusters = 0;
}

int WARCIP::Classify(uint32_t blockAddr, bool isGcAppend)
{
  if (isGcAppend) { // all Gc blocks go to the same class
    return 0;
  }
   
  int numClusters = mCenterOfClusters.size();
  uint64_t rwi = mRwi;
  
  double minDiff = DBL_MAX;
  int    targetClassNum = -1;
  for (int i = 1; i < mCenterOfClusters.size(); ++i) {
    if (mCenterOfClusters[i] == -DBL_MAX) {
      continue;
    }
    double diff = std::fabs(mCenterOfClusters[i] - rwi);
    if (diff < minDiff) {
      minDiff = diff;
      targetClassNum = i;
    }
  }

  mTotalWeightOfClusters[targetClassNum] += rwi;
  mTotalBlocksOfClusters[targetClassNum] += 1;
  mCenterOfClusters[targetClassNum] = mTotalWeightOfClusters[targetClassNum] / mTotalBlocksOfClusters[targetClassNum];

  mTotalWritesOfClusters[targetClassNum] += 1;
  mTotalWritesAllClusters += 1;

  if (mTotalBlocksOfClusters[targetClassNum] == 131072) {

    closeAndReassignCenter(targetClassNum);
    mTotalSealedUserSegments += 1;
    if (mTotalSealedUserSegments == 128) {
      dynamicSplitAndMerge();
    }
    if (mNextMerge.find(targetClassNum) != mNextMerge.end()) {
      merge(targetClassNum);
      mNextMerge.erase(targetClassNum);
    }
  }

  return targetClassNum;
}

void WARCIP::Append(uint32_t blockAddr, uint64_t timestamp)
{
  uint64_t lastWriteTimestamp = mLastWriteTimestamps->Query(blockAddr);
  if (lastWriteTimestamp == (~0ull)) {
    mRwi = 0;
  } else {
    mRwi = timestamp - lastWriteTimestamp + mPenalty->Query(blockAddr);
  }
  mLastWriteTimestamps->Update(blockAddr, timestamp);
  mPenalty->Update(blockAddr, 0);
}

void WARCIP::GcAppend(uint32_t blockAddr)
{
  // Penalty
  uint64_t lastTimestamp = mLastWriteTimestamps->Query(blockAddr);
  assert(lastTimestamp != (~0ull));
  uint64_t penalty = Manager::globalTimestamp - lastTimestamp;
  mPenalty->Update(blockAddr, penalty);
}

void WARCIP::CollectSegment(Segment *segment)
{
}

void WARCIP::closeAndReassignCenter(int classNum)
{
  std::vector<double> centers;

  for (int i = 1; i < mCenterOfClusters.size(); ++i) {
    double centerValue = mCenterOfClusters[i];
    if (centerValue != -DBL_MAX) {
      centers.push_back(centerValue);
    }
  }
  std::sort(centers.begin(), centers.end());

  int rank = -1;
  for (int i = 0; i < centers.size(); ++i) {
    if (mCenterOfClusters[classNum] == centers[i]) {
      rank = i;
    }
  }

  mTotalBlocksOfClusters[classNum] = 0;
  mTotalWeightOfClusters[classNum] = 0;
}

void WARCIP::dynamicSplitAndMerge()
{
  int numClosedClusters = 0;
  for (int i = 1; i < mCenterOfClusters.size(); ++i) {
    double centerValue = mCenterOfClusters[i];
    if (centerValue == -DBL_MAX) {
      numClosedClusters += 1;
    }
  }

  bool isSplitAllowed = (numClosedClusters > 0); // 

  int numClusters = mCenterOfClusters.size();
  mNextMerge.clear();
  for (int i = 1; i < numClusters; ++i) {
    if (mTotalWritesOfClusters[i] < 131072) {
      if (mTotalBlocksOfClusters[i] == 0) {
        merge(i);
        isSplitAllowed = true;
      } else {
        mNextMerge.insert(i);
      }
    }
  }

  if (isSplitAllowed) {
    for (int i = 1; i < numClusters; ++i) {
      if (mTotalWritesOfClusters[i] > mTotalWritesAllClusters / 2) {
        split(i);
        break;
      }
    }
  }

  mTotalSealedUserSegments = 0;
  mTotalWritesAllClusters = 0;
  for (int i = 0; i < numClusters; ++i) {
    mTotalWritesOfClusters[i] = 0;
  }
}

void WARCIP::split(int classNum)
{
  std::vector<double> centers;
  int newClassNum;

  for (int i = 1; i < mCenterOfClusters.size(); ++i) {
    double centerValue = mCenterOfClusters[i];
    if (centerValue != -DBL_MAX) {
      centers.push_back(centerValue);
    } else {
      newClassNum = i;
    }
  }
  std::sort(centers.begin(), centers.end());

  int rank = -1;
  for (int i = 0; i < centers.size(); ++i) {
    if (mCenterOfClusters[classNum] == centers[i]) {
      rank = i;
    }
  }

  double left = (rank > 0) ? centers[rank - 1] : 0; // the left one or zero
  double right = (rank < centers.size() - 1) ?
    centers[rank + 1] :
    (2 * mCenterOfClusters[rank] - left); // the right one or the maximum value

  mCenterOfClusters[newClassNum] = (mCenterOfClusters[classNum] + right) / 2.0;
}

void WARCIP::merge(int classNum)
{
  mCenterOfClusters[classNum] = -DBL_MAX;
}
