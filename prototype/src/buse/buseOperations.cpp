/*
 * buseOperations.cpp
 *
 *  Created on: Jan 25, 2015
 *      Author: andras
 */
#include <chrono>
#include <iostream>
#include "src/buse/buseOperations.h"

using namespace std;

namespace buse {
	buseOperations::buseOperations() { this->size = 0; }
	buseOperations::buseOperations(uint64_t size) { this->size = size; }

	buseOperations::~buseOperations() {
		DEBUGPRINTLN("Destroying buse object.");
		while(!disks.empty()) { close(disks.back().getFD()); disks.pop_back(); }
	}

	int buseOperations::read(void *buf, size_t len, off64_t offset) {
		UNUSED(buf); UNUSED(len); UNUSED(offset);
		DEBUGPRINTLN("R - " << offset << ", " << len);
		return handleTX(buf,len, offset,::commonIncludesRead);
	}

	int buseOperations::write(const void *buf, size_t len, off64_t offset) {
		UNUSED(buf); UNUSED(len); UNUSED(offset);
		DEBUGPRINTLN("W - " << offset << ", " << len);
		return handleTX((void*)buf,len, offset,::commonIncludesWrite);
	}

	int buseOperations::handleTX(void *buf, size_t len, off64_t offset, ssize_t (*func)(int, void *, size_t)) {
		UNUSED(buf); UNUSED(len); UNUSED(offset); UNUSED(func);
		DEBUGPRINTLN("H - " << offset << ", " << len);
		return 0;
	}

	void buseOperations::disc() { DEBUGPRINTLN("Received a disconnect request."); }

	int buseOperations::flush() {
		DEBUGPRINTLN("Received a flush request.");
		for(uint i = 0; i < this->disks.size(); i++) ::syncfs(disks[i].getFD());
		return 0;
	}

	int buseOperations::trim(uint64_t from, uint32_t len) {
		UNUSED(from); UNUSED(len);
		DEBUGPRINTLN("T - " << from << ", " << len);
		return 0;
	}

	uint8_t buseOperations::getNumAsyncIdle() {
		uint8_t numDisks = 0;
		for(uint i = 0; i < disks.size(); i++) { numDisks += (uint8_t)(disks[i].aio_error()!=EINPROGRESS); }
		return numDisks;
	}

	uint8_t buseOperations::getFastestIdleReadDisk() {
		uint8_t cdID = 0;
		for(uint8_t i = 0; i < disks.size(); i++) { if((disks[i].aio_error()!=EINPROGRESS) && (disks[cdID].getReadSpeed() > disks[i].getReadSpeed())) cdID = i; }
		return cdID;
	}

	uint8_t buseOperations::getFastestIdleWriteDisk() {
		uint8_t cdID = 0;
		for(uint8_t i = 0; i < disks.size(); i++) { if((disks[i].aio_error()!=EINPROGRESS) && (disks[cdID].getWriteSpeed() > disks[i].getWriteSpeed())) cdID = i; }
		return cdID;
	}
}
