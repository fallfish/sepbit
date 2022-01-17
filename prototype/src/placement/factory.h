
#ifndef LOGSTORE_PLACEMENT_FACTORY_H
#define LOGSTORE_PLACEMENT_FACTORY_H

#include "placement.h"
#include "no_placement.h"
#include "sepbit.h"
#include "sepgc.h"
#include "dac.h"
#include "warcip.h"
#include <string>
#include <iostream>

class PlacementFactory {
  public:
    static Placement *GetInstance(std::string type) {
      std::cout << "Placement algorithm: " << type << std::endl;
      if (type == "NoSep") {
        return new NoPlacement();
      } else if (type == "SepGC") {
        return new SepGC();
      } else if (type == "DAC") {
        return new DAC();
      } else if (type == "SepBIT") {
        return new SepBIT();
      } else if (type == "WARCIP") {
        return new WARCIP();
      } else {
        std::cout << "No Placement, type: " << type << std::endl;
      }
      return new NoPlacement();
    }
};

#endif
