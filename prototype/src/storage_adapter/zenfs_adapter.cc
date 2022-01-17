#include <iostream>
#include "src/logstore/config.h"
#include "src/storage_adapter/zenfs_adapter.h"

ZenFSAdapter::ZenFSAdapter()
{
  mZbd = new ZonedBlockDevice(Config::GetInstance().zbdName.c_str(), nullptr);
  IOStatus status = mZbd->Open(false);

  if (!status.ok()) {
    std::cout << "Open zoned block device failed" << std::endl;
    delete mZbd;
  }

  Status s;
  mZenFS = new ZenFS(mZbd, FileSystem::Default(), nullptr);
  s = mZenFS->MkFS(Config::GetInstance().zenFsAuxPath.c_str(), 0);
  if (!s.ok()) {
    std::cout << "Open zenfs failed" << std::endl;
    delete mZenFS;
  }
  s = mZenFS->Mount(false);
  if (!s.ok()) {
    std::cout << "mount zenfs failed" << std::endl;
    delete mZenFS;
  }

  printf("ZenFS file system created. Free space: %lu MB\n", 
      mZbd->GetFreeSpace() / (1024 * 1024));
}

void ZenFSAdapter::CreateSegment(int id)
{
  std::cout << "Open the " << id << "-th segment." << std::endl;
  std::string fileName = std::to_string(id);  
  FileOptions fopts;
  IOOptions iopts;
  IODebugContext dbg;
  std::unique_ptr<FSSequentialFile> readHandle;
  std::unique_ptr<FSWritableFile>   writeHandle;

  Status s = mZenFS->NewWritableFile(fileName, fopts, &writeHandle, &dbg);

  if (!s.ok()) {
    std::cout << "Cannot create new file!" << std::endl;
    exit(-1);
    return;
  }
  s = mZenFS->NewSequentialFile(fileName, fopts, &readHandle, &dbg);
  if (!s.ok())
  {
    std::cout << "Cannot open new file!" << std::endl;
    exit(-1);
    return;
  }

  mWriteSegments[id] = std::move(writeHandle);
  mReadSegments[id]  = std::move(readHandle);
}

void ZenFSAdapter::Write(const void *data, int id, off64_t offset)
{
  Slice slice(static_cast<const char*>(data), 4096);
  IOOptions iopts;
  IODebugContext dbg;

  Status s = mWriteSegments[id]->Append(slice, iopts, &dbg);
  if (!s.ok()) {
    exit(-1);
  }
  // Ensure that the buffer of the segment is flushed and the zone
  // is closed when the segment is full.
  if (offset + 1 == 131072) {
    mWriteSegments[id]->Sync(iopts, &dbg);
  }
  assert(s.ok());
}

void ZenFSAdapter::Read(void *data, int id, off64_t offset)
{
  Slice slice;
  IOOptions iopts;
  IODebugContext dbg;

  Status s = mReadSegments[id]->PositionedRead(offset * 4096, 4096, iopts, &slice, static_cast<char*>(data), &dbg);
  if (!s.ok())
  {
    exit(-1);
  }
  assert(s.ok());
}

void ZenFSAdapter::ReadWholeSegment(void *data, int id)
{
  Slice slice;
  IOOptions iopts;
  IODebugContext dbg;

  Status s = mReadSegments[id]->Read(4096 * 131072, iopts, &slice, static_cast<char*>(data), &dbg);
  assert(s.ok());
}

void ZenFSAdapter::DestroySegment(int id)
{
  std::cout << "Destroy the " << id << "-th segment." << std::endl;
  IOOptions iopts;
  IODebugContext dbg;

  std::string fileName = std::to_string(id);  
  mWriteSegments.erase(id);
  mReadSegments.erase(id);

  mZenFS->DeleteFile(fileName, iopts, &dbg);
}
