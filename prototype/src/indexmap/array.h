#ifndef LOGSTORE_ARRAY_H
#define LOGSTORE_ARRAY_H

#include "indexmap.h"

class Array : public IndexMap {
public:
  Array(int capacity);
  Array();
  ~Array();
  uint64_t Query(uint32_t blockAddr) override;
  void Update(uint32_t blockAddr, uint64_t phyAddr) override;

private:
  int mCapacity = 128 * 1024 * 1024; // 512 GiB
  uint64_t *mValues;
};

#endif //LOGSTORE_ARRAY_H
