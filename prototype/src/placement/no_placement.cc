#include "src/placement/no_placement.h"

int NoPlacement::Classify(uint32_t blockAddr, bool isGcAppend) {
  return 0;
}

void NoPlacement::Append(uint32_t blockAddr, uint64_t timestamp) {
}

void NoPlacement::GcAppend(uint32_t blockAddr) {

}

void NoPlacement::CollectSegment(Segment *segment) {

}

