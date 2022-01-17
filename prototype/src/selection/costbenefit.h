#ifndef LOGSTORE_COSTBENEFIT_H
#define LOGSTORE_COSTBENEFIT_H

#include "src/selection/selection.h"

class CostBenefit : public Selection {
public:
    CostBenefit() = default;
    std::vector<std::pair<double, int>> Select(std::vector<Segment> segment) override;
};

#endif //LOGSTORE_COSTBENEFIT_H
