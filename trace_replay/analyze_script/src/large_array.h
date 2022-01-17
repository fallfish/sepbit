#include <cstring>
#include <cinttypes>
#include <iostream>

template<class T>
class LargeArray {
  private:
    T **arrays_;
    uint32_t unitSize_ = 128 * 1024 * 1024;
    uint64_t size_;
    uint32_t lastArraySize_, nArrays_;

  public:
    LargeArray(uint64_t size) {
      nArrays_ = (size + unitSize_ - 1) / unitSize_;
      lastArraySize_ = (size - 1) % unitSize_ + 1;
      size_ = size;
      std::cerr << "Create array: size " << size << ", nArrays_ " << nArrays_ << ", lastArraySize_ " 
        << lastArraySize_ << std::endl;
      
      arrays_ = (T**)malloc(sizeof(T*) * nArrays_);
      for (int i = 0; i < (int)nArrays_; ++i) {
        if (i == (int)nArrays_ - 1) {
          arrays_[i] = (T*)malloc(sizeof(T) * lastArraySize_);
          memset(arrays_[i], 0, sizeof(T) * lastArraySize_);
        } else {
          arrays_[i] = (T*)malloc(sizeof(T) * unitSize_);
          memset(arrays_[i], 0, sizeof(T) * unitSize_);
        }
      }
    }

    void put(uint64_t key, T value) {
      uint32_t arrayId = key / unitSize_;
      uint32_t id = key % unitSize_;

      arrays_[arrayId][id] = value;
    }

    T get(uint64_t key) {
      if (key >= getSize()) {
        std::cerr << "Too large: key = " << key << " >= size = " << getSize() << std::endl;
        exit(1);
      }
      uint32_t arrayId = key / unitSize_;
      uint32_t id = key % unitSize_;
      if (!arrays_[arrayId]) {
        std::cerr << "Error 1\n";
        exit(1);
      }
      if (arrayId >= nArrays_) {
        std::cerr << "Error 2\n";
        exit(1);
      }
      if (arrayId == nArrays_ - 1 && id >= lastArraySize_) {
        std::cerr << "Error 3: " << arrayId << " " << nArrays_ << " " << 
          key << " " << getSize() << " " <<
          id << " " << lastArraySize_ << std::endl;
        exit(1);
      }
//      std::cerr << arrayId << " - " << id << std::endl;
//      std::cerr << (uint64_t)(arrays_[arrayId]) << std::endl;
//      std::cerr << (uint64_t)(arrays_[arrayId][0]) << std::endl;
//      std::cerr << (uint64_t)(arrays_[arrayId][id / 2]) << std::endl;
//      std::cerr << (uint64_t)(arrays_[arrayId][id / 4 * 3]) << std::endl;
      return arrays_[arrayId][id];
    }

    void inc(uint64_t key) {
      if (key >= getSize()) {
        std::cerr << "Too large: key = " << key << " >= size = " << getSize() << std::endl;
        exit(1);
      }
      put(key, get(key) + 1);
    }

    void incValue(uint64_t key, T value) {
      put(key, get(key) + value);
    }

    void output() {
      std::cout << size_ << std::endl;
      for (uint64_t i = 0; i < size_; ++i) {
        std::cout << i << " " << get(i) << std::endl;
      }
    }

    void outputNonZero() {
      uint64_t num = 0;
      for (uint64_t i = 0; i < size_; ++i) {
        if (get(i)) ++num;
      }

      std::cout << num << std::endl;
      for (uint64_t i = 0; i < size_; ++i) {
        if (get(i)) {
          std::cout << i << " " << get(i) << std::endl;
        }
      }
    }

    void outputNonZeroToFile(FILE* fout, bool specify_rows = true) {
      uint64_t num = 0;
      for (uint64_t i = 0; i < size_; ++i) {
        if (get(i)) ++num;
      }

      if (specify_rows) fprintf(fout, "%lu\n", num);
      for (uint64_t i = 0; i < size_; ++i) {
        if (get(i)) {
          fprintf(fout, "%lu %lu\n", i, get(i));
        }
      }
    }

    uint64_t getSize() {
      return size_;
    }

    T getSum() {
      T sumValue = 0;
      for (uint64_t i = 0; i < size_; ++i) {
        sumValue += get(i);
      }
      return sumValue;
    }
};
