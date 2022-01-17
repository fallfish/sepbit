/**
 * Observation 1: Test the update distance of all the user-written blocks
 */

#include "large_array.h"
#include "trace.h"

class Analyzer : Analyzer_base {
  char volume_cstr[100];

  LargeArray<uint64_t>* index_map_;
  LargeArray<uint64_t>* lifespans_in_mb_;
  uint64_t max_lifespan_;
  double write_wss_mb_ = 0;
  uint64_t current_id_ = 1;

  uint64_t getLifespanMb(uint64_t current_id, uint64_t prev_id) {
    uint64_t distance_in_mb = (current_id - prev_id) / 256;
    if (distance_in_mb >= max_lifespan_) distance_in_mb = max_lifespan_;
    return distance_in_mb;
  }

public:

  // initialize properties
  void init(char *propertyFileName, char *volume) {
    std::string volume_id(volume);
    volume_id_ = volume_id;

    strcpy(volume_cstr, volume);
    trace_.loadProperty(propertyFileName, volume);

    uint64_t maxLba = trace_.getMaxLba(volume_id);
    write_wss_mb_ = (double)trace_.getUniqueLba(volume_id) / 256.0;
    n_blocks_ = maxLba + 1;
    max_lifespan_ = (double)n_blocks_ * 8 / 256;

    index_map_ = new LargeArray<uint64_t>(n_blocks_);
    lifespans_in_mb_ = new LargeArray<uint64_t>(max_lifespan_ + 1); // In MiB
  }

  void obsv1_summary() {
    uint64_t levels[4], tmp_lifespan_cnts = 0, ptr = 0; 
    double chkpts[4] = {0.1, 0.2, 0.4, 0.8};
    for (int i = 0; i < 4; i++) levels[i] = 0;
    for (uint64_t i = 0; i < lifespans_in_mb_->getSize(); i++) {
      tmp_lifespan_cnts += lifespans_in_mb_->get(i);
      //printf("%s %lu %lu\n", volume_id_.c_str(), i, lifespans_in_mb_->get(i));
      while (ptr < 4 && (double)i > chkpts[ptr] * write_wss_mb_) {
        levels[ptr] = tmp_lifespan_cnts;
        ptr++;
      }
    }

    for (int i = 0; i < 4; i++) {
      // tmp_lifespan_cnts is the number of lifespans
      fprintf(stderr, "%.1f %.6lf %lu %lu\n", chkpts[i], (double)levels[i] / tmp_lifespan_cnts * 100, levels[i], tmp_lifespan_cnts);
      printf("%.1f %.6lf\n", chkpts[i], (double)levels[i] / tmp_lifespan_cnts * 100);
    }
  }

  void analyze(char *input_trace_file) {
    uint64_t offset, length, timestamp;
    bool is_write;
    openTrace(input_trace_file);

    uint64_t prev_id;
    trace_.myTimer(true, "update distance");

    while (trace_.readNextRequestFstream(*is_, timestamp, is_write, offset, length, line2_)) {
      if (!is_write) continue;

      for (uint64_t i = 0; i < length; i += 1) {
        prev_id = index_map_->get(offset + i);

        if (prev_id != 0) {
          lifespans_in_mb_->inc(getLifespanMb(current_id_, prev_id));
        }

        index_map_->put(offset + i, current_id_++);
      }

      trace_.myTimer(false, "update distance");
    }

    obsv1_summary();
  }
};

int main(int argc, char *argv[]) {
  Analyzer analyzer;
  analyzer.init(argv[3], argv[1]);
  analyzer.analyze(argv[2]);
}
