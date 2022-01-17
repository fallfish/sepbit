//
// Created by andras on 5/6/15.
//

#include <assert.h>
#include <fcntl.h>
#include <sys/mount.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include "src/buse/diskStats.h"

using namespace std;

diskStats::diskStats(int fd, string name) : name(name) {
	aiocb.aio_fildes = fd;
	/* Figure out the size of the underlying block device. */
	int err = ioctl(fd, BLKGETSIZE64, &(this->diskSize));
	assert(err != -1);
	testSpeed();
}

diskStats::diskStats(int fd, string name, chrono::duration<double, std::micro> writeSpeed, chrono::duration<double, std::micro> readSpeed) : name(name) {
	aiocb.aio_fildes = fd;
	/* Figure out the size of the underlying block device. */
	int err = ioctl(fd, BLKGETSIZE64, &(this->diskSize));
	assert(err != -1);
	this->writeSpeed = writeSpeed;
	this->readSpeed = readSpeed;
}

#define READBYTESSIZE (128*1024*1024)

void diskStats::testSpeed() {
	if (this->diskSize < 2 * READBYTESSIZE) {
		this->readSpeed = chrono::duration<double, std::micro>::zero();
		this->writeSpeed = chrono::duration<double, std::micro>::zero();
		return;
	}
	char *buf0 = (char *) malloc(sizeof(char) * READBYTESSIZE);
	char *buf1 = (char *) malloc(sizeof(char) * READBYTESSIZE);
	char *buf2 = (char *) malloc(sizeof(char) * READBYTESSIZE);
	char *buf3 = (char *) malloc(sizeof(char) * READBYTESSIZE);
	if(unlikely(buf0 == NULL || buf1 == NULL || buf2 == NULL || buf3 == NULL)) {
		cerr << "Unable to allocate memory for testSpeeds()" << endl;
		goto cleanup;
	}
	for (uint i = 0; i < READBYTESSIZE; i++) { buf3[i] = (char) 0xA5; }
	testSpeed(buf0, buf1, buf2, buf3, READBYTESSIZE);
cleanup:free(buf3);
	free(buf2);
	free(buf1);
	free(buf0);
}

void diskStats::testSpeed(char *buf0, char *buf1, char *buf2, const char *buf3, const uint32_t bufSiz) {
	DEBUGPRINTLN("Entered testSpeed");
	assert(buf0 != NULL); assert(buf1 != NULL); assert(buf2 != NULL); assert(buf3 != NULL);
	assert(this->diskSize > 2*bufSiz);
	DEBUGPRINTLN("Testing readSpeed");
	auto startTime = chrono::system_clock::now();
	read(buf0, bufSiz, 0);
	read(buf1, bufSiz, this->diskSize / 2);
	read(buf2, bufSiz, this->diskSize - bufSiz);
	this->readSpeed = chrono::system_clock::now() - startTime;
	DEBUGPRINTLN("Testing writeSpeed");
	startTime = chrono::system_clock::now();
	write(buf3, bufSiz, 0);
	write(buf3, bufSiz, this->diskSize / 2);
	write(buf3, bufSiz, this->diskSize - bufSiz);
	this->writeSpeed = chrono::system_clock::now() - startTime;
	DEBUGPRINTLN("Cleaning up");
	write(buf0, bufSiz, 0);
	write(buf1, bufSiz, this->diskSize / 2);
	write(buf2, bufSiz, this->diskSize - bufSiz);
}

int diskStats::aio_error() { return ::aio_error64(&aiocb); }

ssize_t diskStats::aio_return() { return ::aio_return64(&aiocb); }

int diskStats::aio_write(const void *buf, size_t len, off64_t offset) {
	if (aio_error()) {
		errno = EBUSY;
		return -1;
	}
	aiocb.aio_nbytes = len;
	aiocb.aio_offset = offset;
	aiocb.aio_buf = (void *) buf;
	return ::aio_write64(&aiocb);
}

int diskStats::aio_read(void *buf, size_t len, off64_t offset) {
	if (aio_error()) {
		errno = EBUSY;
		return -1;
	}
	aiocb.aio_nbytes = len;
	aiocb.aio_offset = offset;
	aiocb.aio_buf = buf;
	return ::aio_read64(&aiocb);
}

ssize_t diskStats::write(const void *buf, size_t len, off64_t offset) {
	DEBUGPRINTLN("write(" << offset << ',' << len << ") with diskSize=" << this->diskSize);
	if (unlikely(lseek64(getFD(), offset, SEEK_SET) != offset)) return -1;
	return ::write(getFD(), buf, len);
}

ssize_t diskStats::read(void *buf, size_t len, off64_t offset) {
	DEBUGPRINTLN("read(" << offset << ',' << len << ") with diskSize=" << this->diskSize);
	if (unlikely(lseek64(getFD(), offset, SEEK_SET) != offset)) return -1;
	return ::read(getFD(), buf, len);
}

