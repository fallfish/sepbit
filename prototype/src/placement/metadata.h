#ifndef LOGSTORE_METADATA_H
#define LOGSTORE_METADATA_H
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <cassert>
#include <cstring>

class Metadata
{
  public:
    Metadata()
    {
      mFd = open(Config::GetInstance().metadataDir.c_str(), O_RDWR | O_CREAT, 0644);

      if (mFd == -1)
      {
        printf("Error open metadata, error: %s\n", strerror(errno));
        exit(-1);
      }
      if (ftruncate(mFd, kSize * 8) == -1)
      {
        printf("Truncate failed, error: %s\n", strerror(errno));
        exit(-1);
      }
      mArray = (uint64_t*)mmap(mArray, kSize * 8, PROT_READ | PROT_WRITE, MAP_SHARED, mFd, 0);
      if (mArray == MAP_FAILED)
      {
        printf("Mmap failed, error: %s\n", strerror(errno));
        exit(-1);
      }
    }

    void Update(uint64_t offset, uint64_t meta)
    {
      offset /= 4096;
      mArray[offset] = meta;
    }

    uint64_t Query(uint64_t offset)
    {
      uint64_t meta;
      offset /= 4096;
      meta = mArray[offset];
      return meta;
    }

    int mFd;
    uint64_t* mArray;
    const uint64_t kSize = 512ull * 1024 * 1024 * 1024 / 4096;
};
#endif // LOGSTORE_METADATA_H
