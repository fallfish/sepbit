#include "src/placement/sepgc.h"

int SepGC::Classify(uint32_t blockAddr, bool isGcAppend) {
  if (!isGcAppend) return 0;
  else return 1;
}

void SepGC::Append(uint32_t blockAddr, uint64_t timestamp) {

}

void SepGC::GcAppend(uint32_t blockAddr) {

}

void SepGC::CollectSegment(Segment *segment) {

}

