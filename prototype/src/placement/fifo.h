#ifndef LOGSTORE_FIFO_H
#define LOGSTORE_FIFO_H
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <cassert>
#include <map>
#include <cstring>
#include "src/logstore/config.h"

class FIFO
{
  public:
    FIFO()
    {
      mFd = open(Config::GetInstance().fifoDir.c_str(), O_RDWR | O_CREAT, 0644);
      
      if (mFd == -1)
      {
        printf("Error open fifo, error: %s\n", strerror(errno));
        exit(-1);
      }

    
      if (ftruncate(mFd, kFileSize * 8) == -1)
      {
        printf("Truncate failed, error: %s\n", strerror(errno));
        exit(-1);
      }

      mArray = (uint32_t*)mmap(mArray, kFileSize * 8, PROT_READ | PROT_WRITE, MAP_SHARED, mFd, 0);
      if (mArray == MAP_FAILED)
      {
        printf("Mmap failed, error: %s\n", strerror(errno));
        exit(-1);
      }
    }

    void Update(uint32_t blockAddr, double threshold)
    {
      double nValidBlocks = Config::GetInstance().numValidBlocks;
      int res = 0;

      mArray[mTail] = blockAddr;
      mMap[blockAddr] = mTail;
      mTail += 1;
      if (mTail == kFileSize) mTail = 0;

      if ((mTail + kFileSize - mHead) % kFileSize > std::min(threshold, nValidBlocks))
      {
        uint32_t oldBlockAddr = mArray[mHead];
        if (mMap[oldBlockAddr] == mHead)
        {
          mMap.erase(oldBlockAddr);
        }
        mHead += 1;
        if (mHead == kFileSize) mHead = 0;

        if ((mTail + kFileSize - mHead) % kFileSize > threshold)
        {
          oldBlockAddr = mArray[mHead];

          if (mMap[oldBlockAddr] == mHead)
          {
            mMap.erase(oldBlockAddr);
          }
          mHead += 1;
          if (mHead == kFileSize) mHead = 0;
        }
      }
    }

    uint32_t Query(uint32_t blockAddr)
    {
      auto it = mMap.find(blockAddr);
      if (it == mMap.end())
      {
        return UINT32_MAX;
      }
      uint32_t position = it->second;
      uint32_t lifespan = (mTail < position) ?
        mTail + kFileSize - position : mTail - position;
      return lifespan;
    }

    uint32_t mTail = 0;
    uint32_t mHead = 0;
    std::map<uint32_t, uint32_t> mMap;
    uint32_t* mArray = NULL;
    int mFd;
    const uint32_t kFileSize = 128 * 1024 * 1024;
};
#endif // LOGSTORE_FIFO_H
