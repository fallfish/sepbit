#include <cassert>
#include <mutex>
#include <iostream>
#include <thread>

#include "src/logstore/config.h"
#include "src/logstore/manager.h"
#include "src/indexmap/factory.h"
#include "src/storage_adapter/factory.h"
#include "src/placement/factory.h"

uint64_t Manager::globalTimestamp = 0;
Manager::Manager(int numOpenSegments) {
  mIndexMap = std::unique_ptr<IndexMap>(IndexMapFactory::GetInstance(Config::GetInstance().indexMap));
  mPlacement = std::unique_ptr<Placement>(PlacementFactory::GetInstance(Config::GetInstance().placement));
  mStorageAdapter = std::unique_ptr<StorageAdapter>(StorageAdapterFactory::GetInstance(Config::GetInstance().storageAdapter));

  std::lock_guard<std::mutex> lck(mSegmentMutex);
  for (int i = 0; i < numOpenSegments; ++i) {
    std::shared_ptr<Segment> segment = std::make_shared<Segment>(mCurrentSegmentId, i, globalTimestamp);
    mOpenSegments.emplace_back(segment);
    mSegments[segment->GetSegmentId()] = std::move(segment);
    mStorageAdapter->CreateSegment(mCurrentSegmentId);
    mCurrentSegmentId += 1;
  }
}

Manager::~Manager() {
  std::cout << "UserWrite: " << mTotalUserWrites * 4096 / 1024 / 1024 / 1024.0 << 
    ", GCWrite: " << mTotalGcWrites * 4096 / 1024 / 1024 / 1024.0 << std::endl;
}

/*
 * the data must be length of 4KiB
 * logical addr: in units of bytes
 * physical addr: in units of 4KiB blocks
 */
void Manager::Append(const void *buf, off64_t addr) {
  using namespace std::chrono_literals;
  if (GetGp() > 0.15) {
    std::this_thread::sleep_for(0.0001s);
  }

  uint32_t blockAddr;
  int classId;
  uint64_t oldPhyAddr, newPhyAddr;

  mGlobalMutex.lock();
  blockAddr = addr / 4096;
  classId = mPlacement->Classify(blockAddr, false);

  oldPhyAddr = mIndexMap->Query(blockAddr);
  if (oldPhyAddr != ~0ull) {
    std::shared_ptr<Segment> oldSegment = findSegment(oldPhyAddr);
    oldSegment->Invalidate(oldPhyAddr % 131072);
    if (oldSegment->IsSealed()) {
      mTotalInvalidBlocks += 1;
    }
  }

  std::shared_ptr<Segment> currentSegment = mOpenSegments[classId];
  newPhyAddr = currentSegment->Append(blockAddr);
  mIndexMap->Update(blockAddr, newPhyAddr);

  mPlacement->Append(blockAddr, globalTimestamp);
  globalTimestamp += 1; // tick inced by one
  mTotalUserWrites += 1;

  if (currentSegment->IsFull()) {
    currentSegment->Seal();
    mTotalInvalidBlocks += currentSegment->GetTotalInvalidBlocks();
    mTotalBlocks += 131072;
    OpenNewSegment(classId);
  }

  Config::GetInstance().numValidBlocks = GetnValidBlocks();
  mGlobalMutex.unlock();

  currentSegment->Lock();
  mStorageAdapter->Write(buf, currentSegment->GetSegmentId(), newPhyAddr % 131072); // data, segmentId, offset
  currentSegment->Unlock();
}

bool Manager::GcAppend(const void *buf, uint32_t blockAddr, off64_t oldPhyAddr) {
  mGlobalMutex.lock();
  int classId = mPlacement->Classify(blockAddr, true);

  std::shared_ptr<Segment> oldSegment = findSegment(oldPhyAddr);
  uint64_t phyAddr = mIndexMap->Query(blockAddr);
  if (oldPhyAddr != phyAddr) {
    mGlobalMutex.unlock();
    return false;
  }

  std::shared_ptr<Segment> currentSegment = mOpenSegments[classId];
  phyAddr = currentSegment->Append(blockAddr);

  mIndexMap->Update(blockAddr, phyAddr);
  mPlacement->GcAppend(blockAddr);
  mTotalGcWrites += 1;

  if (currentSegment->IsFull()) {
    currentSegment->Seal();
    mTotalInvalidBlocks += currentSegment->GetTotalInvalidBlocks();
    mTotalBlocks += 131072;
    OpenNewSegment(classId);
  }
  mGlobalMutex.unlock();

  currentSegment->Lock();
  mStorageAdapter->Write(buf, currentSegment->GetSegmentId(), phyAddr % 131072);
  currentSegment->Unlock();

  return true;
}

void Manager::Read(void *buf, off64_t addr) {
  uint32_t blockAddr = addr / 4096;
  off64_t phyAddr = mIndexMap->Query(blockAddr);
  if (phyAddr == ~0ull) {
    return;
  }

  std::shared_ptr<Segment> segment = findSegment(phyAddr);
  // read data from segmentId with offset (phyAddr % 131072) and store it in buf
  mStorageAdapter->Read(buf, segment->GetSegmentId(), phyAddr % 131072);
}

Segment Manager::ReadSegment(int id) {
  Segment segment = Segment(mSegments[id]);
  if (segment.GetTotalValidBlocks() != 0) {
    for (int i = 0; i < 131072; ++i) {
      off64_t blockAddr = segment.GetBlockAddr(i);
      if (blockAddr == ~0ull) continue;
      off64_t phyAddr = segment.GetPhyAddr(i);
      char* data = segment.GetBlockData(i);
      mStorageAdapter->Read(data, id, phyAddr % 131072);
    }
  }
  return segment;
}

void Manager::OpenNewSegment(int id) {
  std::lock_guard<std::mutex> lck(mSegmentMutex);

  std::shared_ptr<Segment> segment = std::make_shared<Segment>(mCurrentSegmentId, id, globalTimestamp);
  mOpenSegments[id] = segment;
  mSegments[segment->GetSegmentId()] = std::move(segment);
  mStorageAdapter->CreateSegment(mCurrentSegmentId);
  mCurrentSegmentId += 1;
}

void Manager::RemoveSegment(int id, uint64_t nInvalidBlocks) {
  {
    std::lock_guard<std::mutex> lck(mSegmentMutex);
    mTotalInvalidBlocks -= mSegments[id]->GetTotalInvalidBlocks();
    mTotalBlocks -= 131072;
    mSegments.erase(id);
  }

  mStorageAdapter->DestroySegment(id);
  PrintRealStats();
}

void Manager::CollectSegment(int id) {
  std::lock_guard<std::mutex> lck(mSegmentMutex);
  mPlacement->CollectSegment(mSegments[id].get());
}

double Manager::GetGp() const {
  return (mTotalBlocks == 0) ? 0 : 1.0 * mTotalInvalidBlocks / mTotalBlocks;
}

void Manager::GetSegments(std::vector<Segment> &segs) {
  std::lock_guard<std::mutex> lck(mSegmentMutex);
  segs.reserve(mSegments.size());
  for (auto pr : mSegments) {
    Segment *segment = pr.second.get();
    Segment tmp(segment);
    if (segment->IsSealed()) {
      segs.emplace_back(tmp);
    }
  }
}

uint64_t Manager::GetnValidBlocks() const {
  return mTotalBlocks - mTotalInvalidBlocks;
}

uint64_t Manager::GetnBlocks() const {
  return mTotalBlocks;
}

uint64_t Manager::GetnInvalidBlocks() const {
  return mTotalInvalidBlocks;
}

uint64_t Manager::PrintRealStats() {

  uint64_t nInvalidBlocks = 0, nBlocks = 0;
  for (auto pr : mSegments) {
    nInvalidBlocks += pr.second->GetTotalInvalidBlocks();
    nBlocks += pr.second->GetTotalBlocks();
  }
  std::cout << "Stat: " << nInvalidBlocks << " " << nBlocks - nInvalidBlocks << std::endl;
  std::cout << "UserWrite: " << mTotalUserWrites * 4096 / 1024 / 1024 / 1024.0 << 
    ", GCWrite: " << mTotalGcWrites * 4096 / 1024 / 1024 / 1024.0 << std::endl;
  return 0;
}

std::shared_ptr<Segment> Manager::findSegment(off64_t phyAddr) {
  // the first 32bit is used as the segment id while the last 32bit is used as the offset inside the segment
  uint64_t segmentId = phyAddr / 131072;
  std::shared_ptr<Segment> result = mSegments[segmentId];
  assert(result->GetSegmentId() == segmentId);
  return result;
}
