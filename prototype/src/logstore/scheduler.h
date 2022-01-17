#ifndef LOGSTORE_SCHEDULER_H
#define LOGSTORE_SCHEDULER_H

#include <thread>
#include <zconf.h>
#include <iostream>
#include <sys/time.h>

#include "src/logstore/manager.h"
#include "src/selection/factory.h"
#include "src/logstore/config.h"

class Scheduler {

public:
    Scheduler(Manager *manager);
    void Shutdown() { mShutdown = true; }

private:
    void scheduling(Manager *manager);
    int  select(Manager *manager);
    void collect(Manager* manager, Segment& segment);

    std::unique_ptr<Selection> mSelection;
    std::thread mWorker;
    bool mShutdown = false;
};
#endif //LOGSTORE_SCHEDULER_H
