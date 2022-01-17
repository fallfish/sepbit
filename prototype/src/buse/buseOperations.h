/*
 * buseOperations.h
 *
 *  Created on: Jan 25, 2015
 *      Author: andras
 */

#ifndef BUSEOPERATIONS_H_
#define BUSEOPERATIONS_H_

#include "commonIncludes.h"
#include "diskStats.h"
#include <chrono>
#include <vector>
#include <inttypes.h>

namespace buse {
	class buseOperations {
		public:
			buseOperations();
			buseOperations(uint64_t size);
			virtual ~buseOperations();

			inline uint64_t getSize() { return size; }

			virtual int read(void *buf, size_t len, off64_t offset);
			virtual int write(const void *buf, size_t len, off64_t offset);

			virtual void disc();
			virtual int flush();
			virtual int trim(uint64_t from, uint32_t len);
			inline uint8_t getNumDrives() { return (uint8_t)disks.size(); }
			uint8_t getNumAsyncIdle();
			uint8_t getFastestIdleReadDisk();
			uint8_t getFastestIdleWriteDisk();

		protected:
			// helper function for read/write operations that are similar in content
			virtual int handleTX(void *buf, size_t len, off64_t offset, ssize_t (*func)(int, void *, size_t));

			std::vector<diskStats> disks;
			uint64_t size; // size of the entire array
	};
}

#endif /* BUSEOPERATIONS_H_ */
