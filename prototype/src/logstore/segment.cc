#include <cstring>
#include <iostream>
#include "src/logstore/manager.h"

Segment::Segment(uint64_t id, int temperature, uint64_t timestamp) {
  mSegmentId = id;
  mCreationTimestamp = timestamp;
  mClassNum = temperature;

  mMutex = new std::mutex();
  mBlocks = new uint32_t[131072];
}

uint64_t Segment::Append(uint32_t blockAddr) {
  uint64_t phyAddr = mSegmentId * 131072ull + mNextOffset;

  mBlocks[mNextOffset] = blockAddr;
  mNextOffset += 1;

  return phyAddr;
}

void Segment::Invalidate(int i) {
  mTotalInvalidBlocks += 1;
  mBlocks[i] = UINT32_MAX;
}

uint64_t Segment::GetSegmentId() const {
  return mSegmentId;
}

bool Segment::IsFull() {
  return mNextOffset == 131072;
}

void Segment::Seal() {
  mSealed = true;
}

bool Segment::IsSealed() {
  return mSealed;
}

double Segment::GetGp() const {
  return 1.0 * mTotalInvalidBlocks / mNextOffset;
}

off64_t Segment::GetBlockAddr(int i) {
  return this->mBlocks[i];
}

char* Segment::GetBlockData(int i) {
  return mData + i * 4096;
}

// only obtain the information of the given segment
// used for selection
Segment::Segment(Segment *o) {
  this->mSegmentId = o->mSegmentId;
  this->mTotalInvalidBlocks = o->mTotalInvalidBlocks;
  this->mNextOffset = o->mNextOffset;
  this->mCreationTimestamp = o->mCreationTimestamp;
  this->mBlocks = nullptr;
  this->mClassNum = o->mClassNum;
}

// obtain the metadata of the block information
Segment::Segment(std::shared_ptr<Segment> o) {
  this->mSegmentId = o->mSegmentId;
  this->mTotalInvalidBlocks = o->mTotalInvalidBlocks;
  this->mNextOffset = o->mNextOffset;
  this->mCreationTimestamp = o->mCreationTimestamp;
  this->mBlocks = new uint32_t[131072];
  memcpy(this->mBlocks, o->mBlocks, sizeof(uint32_t) * 131072);
  if (this->mTotalInvalidBlocks == 131072) {
    this->mData = nullptr;
  } else {
    this->mData = (char*)aligned_alloc(512, 131072 * 4096);
  }
  this->mClassNum = o->mClassNum;
}

off64_t Segment::GetPhyAddr(int i) {
  return mSegmentId * 131072 + i;
}

char *Segment::GetData() {
  return mData;
}

uint64_t Segment::GetTotalValidBlocks() const {
  return mNextOffset - mTotalInvalidBlocks;
}

uint64_t Segment::GetTotalInvalidBlocks() const {
  return mTotalInvalidBlocks;
}

uint64_t Segment::GetTotalBlocks() const {
  return mNextOffset;
}

int Segment::GetClassNum() const {
  return mClassNum;
}

uint64_t Segment::GetAge() const {
  return Manager::globalTimestamp - mCreationTimestamp;
}

uint64_t Segment::GetCreationTimestamp() const {
  return mCreationTimestamp;
}

void Segment::Lock() {
  mMutex->lock();
}

void Segment::Unlock() {
  mMutex->unlock();
}
