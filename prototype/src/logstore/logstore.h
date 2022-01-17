#ifndef LOGSTORE_BLOCKDEVICE_H
#define LOGSTORE_BLOCKDEVICE_H


#include <memory>
#include "src/logstore/manager.h"
#include "src/logstore/scheduler.h"
#include "src/buse/buseOperations.h"

class LogStore : public buse::buseOperations {

public:
    LogStore(uint64_t );
    ~LogStore() { Shutdown(); }

    int read(void *buf, size_t len, off64_t offset);
    int write(const void *buf, size_t len, off64_t offset);

    void Shutdown();

private:
    std::unique_ptr<Manager> mManager;
    std::unique_ptr<Scheduler> mScheduler;
};

#endif //LOGSTORE_BLOCKDEVICE_H
