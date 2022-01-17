#include <fstream>
#include <iostream>
#include <assert.h>
#include "src/storage_adapter/local_adapter.h"

void LocalAdapter::CreateSegment(int id) {
  mSegments.emplace(id, std::fstream{});
  mSegments[id].open(mDirPrefix + "/" + "segment_" + std::to_string(id) + ".data",
    std::ios::in | std::ios::out | std::ios::binary | std::ios::trunc);
  std::cout << "Open the " << id << "-th segment." << std::endl;
  assert(mSegments[id].is_open());
}

void LocalAdapter::Write(const void *buf, int id, off64_t offset) {
  std::fstream& fs = mSegments[id];
  fs.seekg(offset * 4096);
  fs.write(static_cast<const char *>(buf), 4096);
}

void LocalAdapter::Read(void *buf, int id, off64_t offset) {
  std::fstream& fs = mSegments[id];
  fs.seekg(offset * 4096);
  fs.read(static_cast<char *>(buf), 4096);
}

void LocalAdapter::ReadWholeSegment(void *buf, int id) {
  std::fstream& fs = mSegments[id];
  fs.seekg(0);
  fs.read(static_cast<char *>(buf), 4096 * 130172);
}

void LocalAdapter::DestroySegment(int id) {
  mSegments[id].close();
  mSegments.erase(id);
  std::remove((mDirPrefix + "/segment_" + std::to_string(id) + ".data").c_str());
}

