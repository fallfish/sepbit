#include "src/selection/selection.h"
#include "src/selection/greedy.h"
#include "src/selection/costbenefit.h"
#include <iostream>

class SelectionFactory {
  public:
    static Selection *GetInstance(std::string type) {
      std::cout << "Selection algorithm: " << type << std::endl;
      if (type == "Greedy") {
        return new Greedy();
      } else if (type == "CostBenefit") {
        return new CostBenefit();
      } else {
        std::cerr << "No Selection, type: " << type << std::endl;
      }
      return new Greedy();
    }
};
