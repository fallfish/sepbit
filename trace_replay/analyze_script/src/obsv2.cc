#include <cmath>
#include <unordered_set>
#include "large_array.h"
#include "trace.h"

#include <cassert>

struct lbaStat {
  LargeArray<uint64_t>* lifespanNums, *lifespanSums;
  LargeArray<double>* lifespanSqrSums;
  std::string volume_id_;

  void init(uint64_t length, const char* volume_id) {
    lifespanNums = new LargeArray<uint64_t>(length);
    lifespanSums = new LargeArray<uint64_t>(length);
    lifespanSqrSums = new LargeArray<double>(length);
    volume_id_ = std::string(volume_id);
  }

  void push(uint64_t index, uint64_t udInMb) { // index is an LBA index, indirectly specifies an LBA
    uint64_t t;
    double tdb;
    tdb = lifespanSqrSums->get(index);
    lifespanSqrSums->put(index, tdb + udInMb * udInMb);

    t = lifespanSums->get(index);
    lifespanSums->put(index, t + udInMb);

    t = lifespanNums->get(index);
    lifespanNums->put(index, t + 1);
  }

  struct MyStruct {
    uint64_t index, value;

    bool operator < (const MyStruct& str) const { // Sort ascendingly
      return (value > str.value);
    }
  };

  void summary() {
    uint64_t cnts[100], wss = lifespanNums->getSize(), i;
    memset(cnts, 0, sizeof(cnts));
    uint64_t cumuSum = 0;  // The summary of UD values
    uint64_t cumuN = 0;    // The number of UDs
    uint64_t cumuNlbas = 0; // The number of unique LBAs

    double cumuSqrSumDouble = 0.0;
    double avg, sd, cv;

    uint64_t lbaCnts = 0;
    int pcts[4] = {1, 5, 10, 20}, index = 0;

    std::cerr << wss << std::endl;

    struct MyStruct* sortor = new MyStruct[wss];
    for (uint64_t i = 0; i < (uint64_t)wss; i++) {
      sortor[i].index = i;
      sortor[i].value = lifespanNums->get(i);
    }

    std::sort(sortor, sortor + wss); 
    for (uint64_t outi = 0; outi < wss; outi++) { // wss == the number of LBAs
      i = sortor[outi].index;
      uint64_t freq = lifespanNums->get(i); // The frequency for this LBA

      if (freq == 0) continue;

      lbaCnts ++;

      while (index < 4 && lbaCnts >= wss * (uint64_t)pcts[index] / 100) {
        avg = (double)cumuSum / cumuN;
        sd = (cumuN == 1) ? 0.0 : sqrt(cumuSqrSumDouble / (cumuN - 1)
            - 2.0 * avg / (cumuN - 1) * cumuSum 
            + (double) cumuN / (cumuN - 1) * avg * avg);
        cv = (cumuSum == 0) ? 0.0 : sd / avg;

        if (cumuN == 0) {
          index++;
          continue;
        }
//        std::cout << std::fixed << lastFreq << " " << cumuN << " " << cumuNlbas << " " << cumuSqrSumDouble << " " << cumuSum << " " << avg << " " << sd << " " << cv << std::endl;

        std::cout << volume_id_ << " " << cv << " " << index + 1 << std::endl;
        std::cerr << "output: " << outi << " out of " << wss << " " << freq << std::endl;

        cumuN = cumuNlbas = cumuSum = 0;
        cumuSqrSumDouble = 0.0;

        index++;
      }

      if (index >= 4) break;

      cumuSum += lifespanSums->get(i);
      cumuSqrSumDouble += lifespanSqrSums->get(i);
      cumuN += freq;
      cumuNlbas ++;

      if (outi % 2000000 == 3) {
        std::cerr << std::fixed << "Sampled: " << freq << " " << cumuN << " " << cumuNlbas << " " << cumuSqrSumDouble << " " << cumuSum << " last: index = " << index << ", " << avg << " " << sd << " " << cv << std::endl;
        std::cerr << lbaCnts << " " << wss << "\n";
      }
    }
  }

} hotLbas;

class Analyzer : Analyzer_base {
  const uint32_t max_dis = (uint32_t)(1) << 29; // 128 GiB as maximum

  LargeArray<uint64_t>* index_map_;
  LargeArray<uint64_t>* lba2lbaIndex_;
  LargeArray<uint32_t>* coldUds_;

  uint64_t startTimestamp_ = 0, current_id_ = 0;
  uint64_t maxLba;
  uint64_t uniqueLba;

  uint32_t getDistance(uint64_t off) { // Return as number of MiB 
    uint64_t distance = current_id_ - index_map_->get(off);
    distance /= 4096 / 16;  // 1MiB as Unit
    if (distance > max_dis - 1) distance = max_dis - 1;  // Max: 128 GiB
    return (uint32_t)distance;
  }


public:

  // initialize properties
  void init(char *propertyFileName, char *volume) {
    std::string volume_id(volume);
    volume_id_ = volume_id;
    trace_.loadProperty(propertyFileName, volume);

    maxLba = trace_.getMaxLba(volume_id);
    uniqueLba = trace_.getUniqueLba(volume_id);
    n_blocks_ = maxLba + 1;

    std::cerr << "nBlocks = " << n_blocks_ << std::endl;
    std::cerr << "WsS = " << uniqueLba << std::endl;

    lba2lbaIndex_ = new LargeArray<uint64_t>(n_blocks_);
    index_map_ = new LargeArray<uint64_t>(n_blocks_);
    hotLbas.init(uniqueLba, volume);
  }

  void analyze(char *inputTrace) {
    uint64_t offset, length, timestamp;
    bool isWrite;

    openTrace(inputTrace);
    uint64_t maxLbaIndex = 0, lbaIndex = 0, lba;

    trace_.myTimer(true, "hotcold");

    while (trace_.readNextRequestFstream(*is_, timestamp, isWrite, offset, length, line2_)) {
      if (!isWrite) continue;

      for (uint64_t i = 0; i < length; i += 1) {
        lba = offset + i;
        uint64_t lastBlockId = index_map_->get(lba);

        if (lastBlockId != 0) { // an old LBA
          lbaIndex = lba2lbaIndex_->get(lba);
          uint32_t currDisInMiB = getDistance(lba);
          hotLbas.push(lbaIndex, currDisInMiB);
        } else {  // a new LBA, allocate index number
          lbaIndex = maxLbaIndex;
          lba2lbaIndex_->put(lba, maxLbaIndex);
          maxLbaIndex++;
        }

        index_map_->put(lba, ++current_id_);
      }

      trace_.myTimer(false, "hotcold");
    }

    std::cerr << "finished, maxLbaIndex = " << maxLbaIndex << std::endl;
    hotLbas.summary();
  }
};

int main(int argc, char *argv[]) {
  Analyzer analyzer;
  analyzer.init(argv[3], argv[1]);
  analyzer.analyze(argv[2]);
  return 0;
}
