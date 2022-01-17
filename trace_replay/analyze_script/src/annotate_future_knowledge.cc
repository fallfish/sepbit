#include "large_array.h"
#include "trace.h"

class Analyzer : Analyzer_base {
  LargeArray<uint64_t>* indexMap_;
  LargeArray<uint64_t>* fifo_;

  uint64_t n_blocks_ = -1ull;
  uint64_t currentId_ = 1;
  // For blocks whose lifespans are greater than 8-TiB data (due to memory constrain on a single machine),
  // we annotate it as infinity (represented as UINT64_MAX)
  // This will not affect the simulation because they are rare (the largest write traffic is 35TiB)
  // maintain a FIFO of 8-TiB data - 2048 million blocks - 16GiB memory overhead
  uint64_t size_ =  2ull * 1024 * 1024 * 1024; 
  uint64_t head = 0, tail = 0, headAccessId = 1;

public:

  // initialize properties
  void init(char *propertyFileName, char *volume) {
    std::string volume_id(volume);
    volume_id_ = volume_id;

    trace_.loadProperty(propertyFileName, volume);

    uint64_t maxLba = trace_.getMaxLba(volume_id);
    n_blocks_ = maxLba + 1;

    indexMap_ = new LargeArray<uint64_t>(n_blocks_);
    fifo_ = new LargeArray<uint64_t>(size_);
  }

  void analyze(char *inputTrace) {
      uint64_t offset, length, timestamp;
      bool isWrite;
      char line[100];

      openTrace(inputTrace);

      while (trace_.readNextRequestFstream(*is_, timestamp, isWrite, offset, length, line)) {

        if (!isWrite) continue;

        for (uint64_t i = 0; i < length; i += 1) {
          uint64_t lastAccessId = indexMap_->get(offset + i);
          if (lastAccessId != 0) {
            uint64_t lifespan = currentId_ - lastAccessId;
            if (lastAccessId >= headAccessId) {
              uint64_t posAccessId = lastAccessId - headAccessId;
              uint64_t pos = head + posAccessId;
              if (pos >= size_) {
                pos -= size_;
              }
              fifo_->put(pos, lifespan);
            }
          }

          indexMap_->put(offset + i, currentId_++);
          fifo_->put(tail++, UINT64_MAX);
          if (tail == size_) {
            tail = 0;
          }

          if (head == tail) {
            uint64_t lifespan = fifo_->get(head++);
            headAccessId += 1;
            std::cout << lifespan << std::endl;
            if (head == size_) {
              head = 0;
            }
          }
        }
      }

      while (head != tail) {
        std::cout << fifo_->get(head++) << std::endl;
        if (head == size_) {
          head = 0;
        }
      }
  }
};

int main(int argc, char *argv[]) {
  Analyzer analyzer;
  if (argc <= 3) {
    std::cerr << "Parameters not enough!\n";
    std::cerr << "Usage: " << argv[0] << " <trace filename> <volume id> <property filename>\n";
    return 1;
  }
  analyzer.init(argv[3], argv[1]);
  analyzer.analyze(argv[2]);
  return 0;
}
