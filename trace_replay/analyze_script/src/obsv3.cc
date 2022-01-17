#include <iostream>
#include <unordered_map>
#include <string>
#include <cstdint>
#include <map>
#include <cstdio>
#include <vector>
#include <set>
#include <algorithm>
#include "large_array.h"
#include "trace.h"
#include <cassert>

class Analyzer : Analyzer_base {
  const uint32_t max_dis = (uint32_t)(1) << 29; // 128 GiB as maximum

  LargeArray<uint64_t>* index_map_;
  LargeArray<uint64_t>* lba_2_lifespans_mb_indices_;
//  LargeArray<uint64_t>* updateDistancesMap_;
  LargeArray<uint8_t>* blk_freqs_;
  LargeArray<uint32_t>* lifespans_mb_;
  LargeArray<uint64_t>* lifespans_2_cnts_;

  uint64_t current_id_ = 1;
  uint64_t num_unique_lbas_ = 0;

  uint32_t getLifespanMb(uint64_t lba) { // Return as number of MiB 
    uint64_t distance = current_id_ - index_map_->get(lba);
    distance /= 1024 / 4;  // 1MiB as Unit
    if (distance > max_dis - 1) distance = max_dis - 1;  // Max: 128 GiB
    return (uint32_t)distance;
  }

public:

  // initialize properties
  void init(char *propertyFileName, char *volume) {
    std::string volume_id(volume);
    volume_id_ = volume_id;

    trace_.loadProperty(propertyFileName, volume);
    uint64_t max_lba = trace_.getMaxLba(volume_id);
    num_unique_lbas_ = trace_.getUniqueLba(volume_id); // Problem: should not use wss, should use uwb
    n_blocks_ = max_lba + 1;

    std::cerr << "nBlocks = " << n_blocks_ << std::endl;
    std::cerr << "WsS = " << num_unique_lbas_ << std::endl;

    index_map_ = new LargeArray<uint64_t>(n_blocks_);
    lba_2_lifespans_mb_indices_ = new LargeArray<uint64_t>(n_blocks_);

    blk_freqs_ = new LargeArray<uint8_t>(n_blocks_);
    lifespans_mb_ = new LargeArray<uint32_t>(num_unique_lbas_ * 4); // In MiB

    lifespans_2_cnts_ = new LargeArray<uint64_t>(1024 * 1024 * 64);  // Maximum: 64 TiB
  }

  void obsv3_summary() {
    uint64_t lifespans_cnts_denominator = 0;
    uint64_t lifespan_cnts = 0, lifespan_cnts_total = 0;
    double chkpts[13] = {0.2, 0.25, 0.5, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0, 4.0, 8.0, 16.0};
    const int chkpts_num = 13;
    int index = 0;

    for (uint64_t i = 0; i < lifespans_mb_->getSize(); i++) {
      uint32_t value = lifespans_mb_->get(i);
      if (value == 0) continue;
      lifespans_2_cnts_->inc(value);
      lifespans_cnts_denominator++;
    }

    for (uint64_t lba = 0; lba < blk_freqs_->getSize(); lba++) {
      uint8_t blk_freq = blk_freqs_->get(lba);
      if (blk_freq >= 5) continue;
      if (blk_freq == 0) continue;
      lifespans_2_cnts_->inc(getLifespanMb(lba));
      lifespans_cnts_denominator++;
    }

    for (int i = 0; i < (int)lifespans_2_cnts_->getSize(); i++) {  // i is the ud in MiB
      lifespan_cnts += lifespans_2_cnts_->get(i);
      lifespan_cnts_total += lifespans_2_cnts_->get(i);

      while (index < chkpts_num && (double)i >= (double)chkpts[index] * num_unique_lbas_ / 256.0) {
        std::cerr << "index " << index << " i " << i 
          << " num_unique_lbas_ / 256 " << (double)num_unique_lbas_ / 256.0 
          << " " << (double)lifespan_cnts / lifespans_cnts_denominator << std::endl;

        std::cout << volume_id_.c_str() << " p" << index + 1 << " " 
          << (double)lifespan_cnts / lifespans_cnts_denominator << std::endl;
        lifespan_cnts = 0;
        index++;
      }
      if (index == chkpts_num) {
        std::cout << volume_id_.c_str() << " p" << index + 1 << " " 
          << 1.0 - (double)lifespan_cnts_total / lifespans_cnts_denominator << std::endl;
        index++;
      }
    }

    while (index <= chkpts_num) {
      std::cout << volume_id_.c_str() << " p" << index + 1 << " 0\n";
      index++;
    }
  }

  void analyze(char *input_trace) {
    uint64_t offset, length, timestamp;
    bool is_write;
    uint64_t num_rare_blk_x4 = 0, lba;

    openTrace(input_trace);
    trace_.myTimer(true, "Lifespans in rarely updated blocks");

    while (trace_.readNextRequestFstream(*is_, timestamp, is_write, offset, length, line2_)) {
      if (!is_write) continue;

      for (uint64_t i = 0; i < length; i += 1) {
        lba = offset + i;
        uint8_t blk_freq = blk_freqs_->get(lba); // before this update, how many updates have happened
        uint64_t last_id = index_map_->get(lba);

        if (last_id != 0) { // a block appeared before
          if (blk_freq >= 4) { // has been updated four times
            continue;
          }

          if (blk_freq < 4) {
            uint32_t lifespan_mb = getLifespanMb(lba);
            uint64_t lifespan_index = lba_2_lifespans_mb_indices_->get(lba) - 4 + blk_freq;
            lifespans_mb_->put(lifespan_index, lifespan_mb + 1);
          }
        } else { // a new block
          num_rare_blk_x4 += 4;
          lba_2_lifespans_mb_indices_->put(lba, num_rare_blk_x4);
        }

        if (blk_freq == 4) { // fifth update, delete everything
          uint64_t lifespan_index = lba_2_lifespans_mb_indices_->get(lba) - 4;
          for (uint64_t j = lifespan_index; j <= lifespan_index + 3; j++) {
            lifespans_mb_->put(j, 0); // delete 4 lifespans
          } 
        }

        index_map_->put(offset + i, current_id_++);
        if (last_id > 0) blk_freqs_->put(offset + i, ((blk_freq > 4) ? 5 : blk_freq + 1));
      }

      trace_.myTimer(false, "Lifespans in rarely updated blocks");
    }

    obsv3_summary();
  }
};

int main(int argc, char *argv[]) {
  Analyzer analyzer;
  analyzer.init(argv[3], argv[1]);
  analyzer.analyze(argv[2]);
  return 0;
}
