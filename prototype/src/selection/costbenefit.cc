#include <cmath>
#include <algorithm>
#include <float.h>
#include <iostream>
#include "src/selection/costbenefit.h"
#include "src/logstore/manager.h"

std::vector<std::pair<double, int>> CostBenefit::Select(std::vector<Segment> segments) {
  uint64_t globalTimestamp = Manager::globalTimestamp;
  std::vector<std::pair<double, int>> vec;
  for (int i = 0; i < segments.size(); ++i) {
    Segment &o = segments[i];

    double gp = o.GetGp();
    double age = globalTimestamp - o.GetCreationTimestamp();
    double score = (gp == 1.0) ? DBL_MAX : gp / (1 - gp) * sqrt(age);

    vec.emplace_back(score, o.GetSegmentId());
  }

  std::sort(vec.begin(), vec.end(),
      [] (const std::pair<double, int> &a, 
        const std::pair<double, int> &b) 
      { 
        return (a.first > b.first); 
      });

  return vec;
}

