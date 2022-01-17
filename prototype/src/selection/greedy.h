#ifndef LOGSTORE_GREEDY_H
#define LOGSTORE_GREEDY_H

#include "src/selection/selection.h"

class Greedy : public Selection {
public:
    Greedy() = default;
    std::vector<std::pair<double, int>> Select(std::vector<Segment> segment) override;
};

#endif //LOGSTORE_GREEDY_H
