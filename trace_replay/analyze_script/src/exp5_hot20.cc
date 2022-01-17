#include "large_array.h"
#include "trace.h"

class Analyzer : Analyzer_base {
  const uint32_t max_dis = (uint32_t)(1) << 29; // 128 GiB as maximum

  LargeArray<uint64_t>* lba_2_freq_;
  uint64_t traffic_ = 0;

  void summary() {
    std::vector<uint64_t> freqs;
    uint64_t wss = 0;
    uint64_t hot20_traffic = 0;
    
    for (uint64_t i = 0; i < lba_2_freq_->getSize(); i++) {
      uint64_t value = lba_2_freq_->get(i);
      if (value) {
        freqs.push_back(value);
        wss++;
      }
    }
    std::sort(freqs.begin(), freqs.end(), std::greater<int>());

    for (uint64_t i = 0; i < wss / 5; i++) {
      hot20_traffic += freqs[i];
    }
    std::cout << volume_id_ << " " << (double)hot20_traffic / traffic_ << std::endl;
  }

public:

  // initialize properties
  void init(char *propertyFileName, char *volume) {
    std::string volume_id(volume);
    volume_id_ = volume_id;
    trace_.loadProperty(propertyFileName, volume);

    uint64_t maxLba = trace_.getMaxLba(volume_id);
    n_blocks_ = maxLba + 1;

    std::cerr << "nBlocks = " << n_blocks_ << std::endl;

    lba_2_freq_ = new LargeArray<uint64_t>(n_blocks_);
  }

  void analyze(char *inputTrace) {
    uint64_t offset, length, timestamp;
    bool is_write;

    openTrace(inputTrace);

    trace_.myTimer(true, "hot 20% traffic");

    while (trace_.readNextRequestFstream(*is_, timestamp, is_write, offset, length, line2_)) {
      if (!is_write) continue;

      traffic_ += length;
      for (uint64_t i = 0; i < length; i += 1) {
        lba_2_freq_->inc(offset + i);
      }

      trace_.myTimer(false, "hot 20% traffic");
    }

    summary();
  }
};

int main(int argc, char *argv[]) {
  Analyzer analyzer;
  analyzer.init(argv[3], argv[1]);
  analyzer.analyze(argv[2]);
  return 0;
}
