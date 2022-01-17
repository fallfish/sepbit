#include <cassert>
#include "src/placement/dac.h"

DAC::DAC()
{
  mTemperatureMap = std::unique_ptr<IndexMap>(IndexMapFactory::GetInstance("HashMap"));
}

int DAC::Classify(uint32_t blockAddr, bool isGcAppend)
{
  uint64_t classNum = mTemperatureMap->Query(blockAddr);
  if (classNum == (~0ull)) {
    classNum = 0;
  }
  return classNum;
}


void DAC::CollectSegment(Segment *segment) {
}

void DAC::Append(uint32_t blockAddr, uint64_t timestamp) {
  uint64_t classNum = mTemperatureMap->Query(blockAddr);
  if (classNum == (~0ull)) {
    classNum = 0;
    mTemperatureMap->Update(blockAddr, 0);
  } else {
    if (classNum < 5) {
      mTemperatureMap->Update(blockAddr, classNum + 1);
    }
  }
}

void DAC::GcAppend(uint32_t blockAddr) {
  uint64_t classNum = mTemperatureMap->Query(blockAddr);
  assert(classNum != (~0ull));

  if (classNum > 0) {
    mTemperatureMap->Update(blockAddr, classNum - 1);
  }
}
