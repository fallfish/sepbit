#include "large_array.h"
#include "trace.h"

class Analyzer : Analyzer_base {
  const uint64_t max_dis = 1024 * 1024 * 64; // 64 TiB as maximum

  LargeArray<uint64_t>* index_map_;
  LargeArray<uint64_t>* lifespans_mb_;

  uint64_t num_unique_lbas_ = 0;
  double write_wss_mb_ = 0.0;

  uint64_t current_id_ = 1;
  uint64_t getDistance(uint64_t current_id, uint64_t prev_id) {
    uint64_t distance = current_id - prev_id;
    distance /= 256; // 1 MiB as unit
    if (distance > max_dis - 1) distance = max_dis - 1; 
    return distance;
  }

  void design_gw_summary() {
    std::vector<double> r0_thresholds;
    std::vector<double> g0_thresholds;
    std::vector<uint64_t> denominator, numerator;

    double r0_2_write_wss_multiples[3] = {0.4, 0.8, 1.6};
    double g0_2_write_wss_multiples[4] = {0.8, 1.6, 3.2, 6.4};

    for (int i = 0; i < 3; i++) { // r0 from 2 GiB to 8 GiB, in the units of 1 MiB
      double r0_tmp = write_wss_mb_ * r0_2_write_wss_multiples[i];
      r0_thresholds.push_back(r0_tmp);
    }
    for (int i = 0; i < 4; i++) { // g0 from 1 GiB to 64 GiB, in the units of 1 MiB
      double g0_tmp = write_wss_mb_ * g0_2_write_wss_multiples[i];
      g0_thresholds.push_back(g0_tmp);
    }

    for (int i = 0; i < (int)r0_thresholds.size() * (int)g0_thresholds.size(); i++) {
      denominator.push_back(0);
      numerator.push_back(0);
    }

    for (int lifespan_mb = 0; lifespan_mb < (int)lifespans_mb_->getSize(); lifespan_mb++) {
      uint64_t lifespan_mb_cnt = lifespans_mb_->get(lifespan_mb);
      for (int i = 0; i < (int)r0_thresholds.size(); i++) {
        for (int j = 0; j < (int)g0_thresholds.size(); j++) {
          int ij = i * g0_thresholds.size() + j;
          double r0_mb = r0_thresholds[i], g0_mb = g0_thresholds[j];

          if (lifespan_mb >= g0_mb) {
            denominator[ij] += lifespan_mb_cnt;
            if (lifespan_mb <= g0_mb + r0_mb) {
              numerator[ij] += lifespan_mb_cnt;
            }
          }
        }
      }
//      if (lifespan_mb_cnt) 
//        printf("%s %d %lu\n", volume_id_.c_str(), lifespan_mb, lifespans_mb_->get(lifespan_mb));
    }

    // Summary
    for (int i = 0; i < (int)r0_thresholds.size(); i++) {
      for (int j = 0; j < (int)g0_thresholds.size(); j++) {
        int ij = i * g0_thresholds.size() + j;
        std::cout << volume_id_ << " " << 
          r0_2_write_wss_multiples[i] << " " <<
          g0_2_write_wss_multiples[j] << " " << 
          numerator[ij] << " " << 
          denominator[ij] << " " <<
          (double)numerator[ij] / ((double)denominator[ij] + 0.00001) << std::endl;
      }
    }
  }

public:

  // initialize properties
  void init(char *propertyFileName, char *volume) {
    std::string volume_id(volume);
    volume_id_ = volume_id;

    trace_.loadProperty(propertyFileName, volume);
    uint64_t max_lba = trace_.getMaxLba(volume_id);
    n_blocks_ = max_lba + 1;
    num_unique_lbas_ = trace_.getUniqueLba(volume_id); // should be uwb
    write_wss_mb_ = (double)num_unique_lbas_ / 256.0; 

    std::cerr << "nBlocks = " << n_blocks_ << std::endl;

    index_map_ = new LargeArray<uint64_t>(n_blocks_);
    lifespans_mb_ = new LargeArray<uint64_t>(max_dis); // In MiB
  }

  void analyze(char *input_trace) {
    uint64_t offset, length, timestamp;
    bool is_write;

    openTrace(input_trace);

    trace_.myTimer(true, "GW in uwb");

    while (trace_.readNextRequestFstream(*is_, timestamp, is_write, offset, length, line2_)) {
      if (!is_write) continue;

      for (uint64_t i = 0; i < length; i++) {
        uint64_t prev_id = index_map_->get(offset + i);
        if (prev_id != 0) {
          lifespans_mb_->inc(getDistance(current_id_, prev_id));
        }

        index_map_->put(offset + i, current_id_++);
      }

      trace_.myTimer(false, "GW in uwb");
    }

    design_gw_summary();
  }
};

int main(int argc, char *argv[]) {
  Analyzer analyzer;
  analyzer.init(argv[3], argv[1]);
  analyzer.analyze(argv[2]);
  return 0;
}
