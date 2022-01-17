#include "large_array.h"
#include "trace.h"

class Split : public Analyzer_base {
  std::vector<uint64_t> savedOffsets;
  std::vector<uint64_t> savedLengths;
  std::vector<bool> savedIsWrites;
  uint64_t lastTimestamp;
  uint64_t ptr, maxPtr;

  void output() {
    char s[200];
    if (ptr > 0) {
      double interval = (double)1000000.0 / ptr;
      for (int i = 0; i < ptr; i++) {
        // 0,R,126703661056,4096,1577808000000046
        sprintf(s, "%s,%c,%lu,%lu,%lu\n", volume_id_.c_str(), 
            (savedIsWrites[i] ? 'W' : 'R'), savedOffsets[i] * 4096, savedLengths[i] * 4096, 
            (uint64_t)std::min((double)lastTimestamp / 10.0 + interval * i, (double)lastTimestamp / 10.0 + 999999.0)); 
        std::cout << s;
//        std::cout << volume_id_ << "," 
//          << ((savedIsWrites[i]) ? 'W' : 'R') << ","
//          << savedOffsets[i] << ","
//          << savedLengths[i] << ","
//          << (uint64_t)std::min((double)lastTimestamp / 10.0 + interval * i, (double)lastTimestamp / 10.0 + 999999.0) << std::endl;
      }
    }
    ptr = 0;
  }

public:

  void analyze(char *inputTrace) {
    uint64_t offset, length, timestamp;
    bool isWrite;
    char filename[300];
    openTrace(inputTrace);
    trace_.myTimer(true, "split");

    bool first = true;
    uint64_t cnt = 0;
    ptr = maxPtr = 0;

    while (trace_.readNextRequestFstream(*is_, timestamp, isWrite, offset, length, line2_)) {
      if (!isWrite) continue;
      if (lastTimestamp != timestamp) {
        output();
      }

      if (ptr < maxPtr) {
        savedOffsets[ptr] = offset;
        savedLengths[ptr] = length;
        savedIsWrites[ptr] = isWrite;
        ptr++;
      } else {
        savedOffsets.push_back(offset);
        savedLengths.push_back(length);
        savedIsWrites.push_back(isWrite);
        maxPtr = (uint64_t)savedOffsets.size();
        ptr = maxPtr;
      }
      lastTimestamp = timestamp;

      trace_.myTimer(false, "split");
    }

    output();
  }
};

int main(int argc, char *argv[]) {
  setbuf(stderr, NULL);
  Split split;
  split.init(argv[3], argv[1]);
  split.analyze(argv[2]);
}
