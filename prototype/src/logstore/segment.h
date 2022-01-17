#ifndef LOGSTORE_SEGMENT_H
#define LOGSTORE_SEGMENT_H


#include <memory>
#include <mutex>

class Segment {
public:
    Segment(uint64_t id, int temperature, uint64_t timestamp);
    Segment(Segment *);
    Segment(std::shared_ptr<Segment>);
    uint64_t Append(uint32_t blockAddr);
    void      Invalidate(int i);
    void      Seal();

    bool      IsFull();
    bool      IsSealed();
    char*    GetData();
    char*    GetBlockData(int i);

    off64_t  GetBlockAddr(int i);
    off64_t  GetPhyAddr(int i);

    uint64_t GetSegmentId() const;
    int      GetClassNum() const;

    uint64_t GetTotalInvalidBlocks() const;
    uint64_t GetTotalValidBlocks() const;
    uint64_t GetTotalBlocks() const;

    double   GetGp() const;
    uint64_t GetAge() const;
    uint64_t GetCreationTimestamp() const;

    void Lock();
    void Unlock();

    ~Segment() {
      if (mBlocks != nullptr) {
        delete[] mBlocks;
      }
      if (mData != nullptr) {
        free(mData);
      }
      if (mMutex != nullptr) {
        delete mMutex;
      }
    }

private:
    uint64_t mSegmentId = 0;
    bool     mSealed = false;
    uint64_t mCreationTimestamp = 0;
    int      mClassNum = 0;

    uint64_t mTotalInvalidBlocks = 0;

    uint32_t* mBlocks = nullptr; // storing the logical block addresses of the blocks
    uint64_t mNextOffset = 0; // the offset of next appended block

    char *mData = nullptr; // storing the data of the blocks, only used when GC

    std::mutex* mMutex = nullptr;
};


#endif //LOGSTORE_SEGMENT_H
