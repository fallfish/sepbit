#ifndef LOGSTORE_SELECTION_H
#define LOGSTORE_SELECTION_H

#include <vector>
#include <utility>
#include "src/logstore/segment.h"

class Selection {

public:
    Selection() = default;
    virtual std::vector<std::pair<double, int>> Select(std::vector<Segment> segment) = 0;
};

#endif //LOGSTORE_SELECTION_H
