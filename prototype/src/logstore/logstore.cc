#include <cstdint>
#include <cstring>
#include <ctime>

#include "src/logstore/logstore.h"
#include "src/logstore/segment.h"

int LogStore::write(const void *buf, size_t len, off64_t offset) {
  // check against the invalidation
  alignas(512) char tmp[4096];

  buseOperations::write(buf, len, offset);
  if(unlikely(len + (uint64_t)offset > this->getSize())) return EFBIG;
  
  size_t beg = offset / 4096 * 4096;
  size_t end = (offset + len + 4095) / 4096 * 4096;

  //   |-----|--------|------| (4KiB)
  // beg    off    off+len   end
  //   | tmp                 |
  //         | buf    |
  if ( (beg != offset || end != offset + len) && end == beg + 4096) {
    memset(tmp, 0, sizeof(tmp));
    mManager->Read(tmp, beg);
    memcpy(tmp + offset - beg, (const char*)buf, len);
    mManager->Append(tmp, beg);
    return 0;
  }

  uint64_t i = 0;
  // deal with the beginning part
  if (beg < offset) {
    memset(tmp, 0, sizeof(tmp));
    mManager->Read(tmp, beg);
    memcpy(tmp + offset - beg, (const char*)buf, beg + 4096 - offset);
    mManager->Append(tmp, beg);

    i = beg + offset - offset;
  }

  for ( ;; i += 4096) {
    off64_t addr = offset + i;
    if (addr == end - 4096) break;
    mManager->Append(((const char *)buf) + i, addr);
  }

  if (offset + len < end) {
    // deal with the beginning part
    memset(tmp, 0, sizeof(tmp));
    mManager->Read(tmp, end - 4096);
    memcpy(tmp, (const char*)buf + (end - offset) - 4096, offset + len - (end - 4096));
    mManager->Append(tmp, end - 4096);
  } else {
    mManager->Append(((const char *)buf) + i, offset + i);
  }

  return 0;
}

int LogStore::read(void *buf, size_t len, off64_t offset) {
  alignas(512) char tmp[4096];

  buseOperations::read(buf, len, offset);
  if(unlikely(len + (uint64_t)offset > this->getSize())) return EFBIG;
  size_t beg = offset / 4096 * 4096;
  size_t end = (offset + len + 4095) / 4096 * 4096;

  if ( (beg != offset || end != offset + len) && end == beg + 4096) {
    mManager->Read(tmp, beg);
    memcpy((char*)buf, tmp + offset - beg, len);
    return 0;
  }

  if (beg < offset) {
    memset(tmp, 0, sizeof(tmp));
    // deal with the beginning part
    mManager->Read(tmp, beg);
    memcpy((char*)buf, tmp + offset - beg, beg + 4096 - offset);
  }

  uint64_t i = 0;
  if (beg < offset) i = 4096 - (offset - beg);
  else i = 0;
  for (; i < len; i += 4096) {
    off64_t addr = offset + i;
    if (addr + 4096 == end) break;
    mManager->Read(((char*)buf) + i, addr);
  }

  if (offset + len < end) {
    memset(tmp, 0, sizeof(tmp));
    mManager->Read(tmp, end - 4096);
    memcpy((char*)buf + (end - offset) - 4096, tmp, offset + len - (end - 4096));
  } else {
    mManager->Read(((char*)buf) + i, offset + i);
  }
  return 0;
}

LogStore::LogStore(uint64_t size) :
  buse::buseOperations(size)
{
  mManager = std::make_unique<Manager>(6);
  mScheduler = std::make_unique<Scheduler>(mManager.get());
}

void LogStore::Shutdown() {
  mScheduler->Shutdown();
}

