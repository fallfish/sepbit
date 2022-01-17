#ifndef LOGSTORE_STORAGE_ADAPTER_H
#define LOGSTORE_STORAGE_ADAPTER_H

class StorageAdapter {

public:
    virtual void Write(const void *, int, off64_t) = 0;
    virtual void CreateSegment(int) = 0;
    virtual void Read(void *, int, off64_t) = 0;
    virtual void ReadWholeSegment(void *, int) = 0;
    virtual void DestroySegment(int) = 0;
};

#endif //LOGSTORE_STORAGE_ADAPTER_H
