### SepBIT prototype
This project include a real prototype of a log-structured storage system.

### Dependencies
* Snappy, bz2, tcmalloc, zlib (installed via apt-get)
* RocksDB with ZenFS compiled (see the [link](https://github.com/westerndigitalcorporation/zenfs))

### Build and Run
* The project is managed using CMake (version >= 3.15)
		* Step 1: mkdir build
		* Step 2: cd build && cmake ..
		* Step 3: make

### Run
* Build the emulated zoned storage environment
		* Use tcmu-runner to emulate an emulated SMR disks, [tutorial](https://zonedstorage.io/getting-started/smr-emulation/)
* Configure the parameters, e.g., the devices and the folders for temporary on-disk data structures, in logstore/config.h and rebuild
* Attention: root priviledge is required to operate on raw zoned storage device.
