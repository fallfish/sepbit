#include "large_array.h"
#include "trace.h"

class Analyzer : Analyzer_base {
  const uint32_t max_dis = 1024 * 1024 * 64; // 64 TiB as maximum

  LargeArray<uint64_t>* index_map_;
  LargeArray<double>* last_lifespans_mb_;

  uint64_t current_id_ = 1;
  uint64_t uniqueLbas_ = 0;
  double write_wss_mb_ = 0;

  double getLifespanMb(uint64_t prev_id) { // Return as number of MiB 
    double distance = current_id_ - prev_id;
    distance /= 256.0;  // 1 MiB as Unit
    if (distance > max_dis - 1) distance = max_dis - 1;  
    return distance;
  }

public:

  // initialize properties
  void init(char *propertyFileName, char *volume) {
    std::string volumeId(volume);
    volume_id_ = volumeId;

    trace_.loadProperty(propertyFileName, volume);
    uint64_t maxLba = trace_.getMaxLba(volumeId);
    n_blocks_ = maxLba + 1;
    uniqueLbas_ = trace_.getUniqueLba(volumeId);
    write_wss_mb_ = (double)uniqueLbas_ / 256.0;

    std::cerr << "nBlocks = " << n_blocks_ << std::endl;

    index_map_ = new LargeArray<uint64_t>(n_blocks_);
    last_lifespans_mb_ = new LargeArray<double>(n_blocks_);
  }

  void analyze(char *input_trace) {
    uint64_t offset, length, timestamp;
    bool is_write;

    std::vector<double> u0_values_mb;
    std::vector<double> v0_values_mb;
    std::vector<uint64_t> denominators, numerators;

    double write_wss_multiples[6] = {0.025, 0.05, 0.1, 0.2, 0.4, 0.8};

    for (int i = 0; i < 6; i++) { // from 0.25 GiB to 8 GiB, in the units of 16 MiB
      u0_values_mb.push_back(write_wss_multiples[i] * write_wss_mb_);
      v0_values_mb.push_back(write_wss_multiples[i] * write_wss_mb_);
    }

    openTrace(input_trace);

    int first = 10;
    trace_.myTimer(true, "Design UW in UWB");
    std::cerr << " test 1 \n";

    while (trace_.readNextRequestFstream(*is_, timestamp, is_write, offset, length, line2_)) {
      if (!is_write) continue;

      for (uint64_t i = 0; i < length; i += 1) {
        uint64_t prev_id = index_map_->get(offset + i);
        if (prev_id != 0) {
          double curr_dis_mb = getLifespanMb(prev_id);

          if (last_lifespans_mb_->get(offset + i) != 0.0) {
            double last_dis_mb = last_lifespans_mb_->get(offset + i) - 1.0;

            for (int i = 0; i < (int)v0_values_mb.size(); i++) {
              for (int j = 0; j < (int)u0_values_mb.size(); j++) {
                int ij = i * u0_values_mb.size() + j;
                while ((int)denominators.size() <= ij) {
                  denominators.push_back(0);
                  numerators.push_back(0);
                }

                double v0_mb = v0_values_mb[i], u0_mb = u0_values_mb[j];
                if ((double)last_dis_mb <= v0_mb) {
                  denominators[ij]++;
                  if ((double)curr_dis_mb <= u0_mb) {
                    numerators[ij]++;
                  }

                }
              }
            }

            if (first && last_dis_mb <= v0_values_mb[0] && (curr_dis_mb > u0_values_mb[0])) {
              std::cerr << last_dis_mb << " " << v0_values_mb[0] << " " << curr_dis_mb << std::endl;
              first--;
            }
          }

          last_lifespans_mb_->put(offset + i, curr_dis_mb + 1.0);
        }

        index_map_->put(offset + i, current_id_++);
      }

      trace_.myTimer(false, "Design UW in UWB");
    }

    // Summary
    for (int i = 0; i < (int)v0_values_mb.size(); i++) {
      for (int j = 0; j < (int)u0_values_mb.size(); j++) {
        int ij = i * u0_values_mb.size() + j;
        std::cout << volume_id_ << " " << 
          write_wss_multiples[i] << " " << 
          write_wss_multiples[j] << " " <<
          denominators[ij] << " " <<
          numerators[ij] << " " << 
          (double)numerators[ij] / ((double)denominators[ij] + 0.00001) << std::endl;

        // debug
        if (i == 0) {
          std::cerr << volume_id_ << " " << 
            write_wss_multiples[i] << " " << 
            write_wss_multiples[j] << " " <<
            denominators[ij] << " " <<
            numerators[ij] << std::endl; 
        }
      }
    }

  }
};

int main(int argc, char *argv[]) {
  Analyzer analyzer;
  analyzer.init(argv[3], argv[1]);
  analyzer.analyze(argv[2]);
  return 0;
}
