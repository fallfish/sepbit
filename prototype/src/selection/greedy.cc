#include <algorithm>
#include "src/selection/greedy.h"

std::vector<std::pair<double, int>> Greedy::Select(std::vector<Segment> segments) {
  std::vector<std::pair<double, int>> vec;
  for (int i = 0; i < segments.size(); ++i) {
    Segment &o = segments[i];

    double gp = o.GetGp();

    vec.emplace_back(gp, o.GetSegmentId());
  }

  std::sort(vec.begin(), vec.end(),
      [] (const std::pair<double, int> &a, 
        const std::pair<double, int> &b) 
      { 
        return (a.first > b.first); 
      });

  return vec;
}

