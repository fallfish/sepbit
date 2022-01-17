#include "src/indexmap/array.h"
#include <iostream>

Array::Array(int capacity) {
  mCapacity = capacity;
  mValues = new uint64_t[mCapacity];
  for (int i = 0; i < mCapacity; ++i) {
    mValues[i] = ~0ull;
  }
}

Array::Array() {
  mValues = new uint64_t[mCapacity];
  for (int i = 0; i < mCapacity; ++i) {
    mValues[i] = ~0ull;
  }
}

void Array::Update(uint32_t blockAddr, uint64_t phyAddr) {
  mValues[blockAddr] = phyAddr;
}

uint64_t Array::Query(uint32_t blockAddr) {
  return mValues[blockAddr];
}

Array::~Array() {
  delete[] mValues;
}
