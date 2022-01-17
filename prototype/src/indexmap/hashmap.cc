#include <iostream>
#include "src/indexmap/hashmap.h"

HashMap::HashMap() {
  mMap.clear();
}

void HashMap::Update(uint32_t blockAddr, uint64_t phyAddr) {
  mMap[blockAddr] = phyAddr;
}

uint64_t HashMap::Query(uint32_t blockAddr) {
  if (mMap.find(blockAddr) == mMap.end()) {
    return ~0ull;
  } else {
    return mMap[blockAddr];
  }
}

