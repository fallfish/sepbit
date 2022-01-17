/*
 * buse-cpp - block-device userspace extensions (in C++)
 * Copyright (C) 2015 Andras Fekete
 *
 * This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
#include "src/buse/buse.h"

#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <linux/types.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <unistd.h>

#include <sys/types.h>
#include <linux/nbd.h>

// interrupt handling stuffs
#include <ctype.h>
#include <signal.h>

#include <sys/wait.h>

#include <iostream>

using namespace std;

// A function that takes the child end of the socket and handle commands coming in on the NBD device
int nbdChild = -1;

inline int doChild(const int sockChild, const uint64_t bopSize) {
	int retVal = 0;
	if (ioctl(nbdChild, NBD_SET_SIZE, bopSize) == -1) {
    exit(-1);
  }
//	assert(ioctl(nbdChild, NBD_SET_BLKSIZE, 4096) != -1);
//	assert(ioctl(nbdChild, NBD_SET_SIZE_BLOCKS, bopSize / 4096) != -1);
	if (ioctl(nbdChild, NBD_CLEAR_SOCK) == -1) {
    exit(-1);
  }

	DEBUGPRINTLN("Child process started.");
	/* The child needs to continue setting things up. */

	DEBUGPRINTLN("Before SET_SOCK");

	if (ioctl(nbdChild, NBD_SET_SOCK, sockChild) == -1) {
		cerr << "child: ioctl(nbd, NBD_SET_SOCK, sp[sockChild]) failed.[" << strerror(errno) << "]" << endl;
		retVal = errno;
	}
#if defined NBD_SET_FLAGS && defined NBD_FLAG_SEND_TRIM
	else if (ioctl(nbdChild, NBD_SET_FLAGS, NBD_FLAG_SEND_TRIM) == -1) {
		cerr << "child: ioctl(nbd, NBD_SET_FLAGS, NBD_FLAG_SEND_TRIM) failed.[" << strerror(errno) << "]" << endl;
		retVal = errno;
	}
#endif
	else {
		DEBUGPRINTLN("Before DO_IT");
		int err = ioctl(nbdChild, NBD_DO_IT);
		if (err == -1) {
			if (errno != EPIPE) { // we're expecting a broken pipe when the parent closes it
				cerr << "child: nbd device terminated with code " << errno << '(' << strerror(errno) << ')' << endl;
				retVal = errno;
			}
		}
	}

	DEBUGPRINTLN("child: Before CLEAR_QUE");
	ioctl(nbdChild, NBD_CLEAR_QUE);
	DEBUGPRINTLN("child: Before CLEAR_SOCK");
	ioctl(nbdChild, NBD_CLEAR_SOCK);
	//DEBUGPRINTLN("child: Before CMD_DISC");
	//ioctl(nbd, NBD_CMD_DISC);

	return retVal;
}

// These helper functions were taken from cliserv.h in the nbd distribution.
#ifdef WORDS_BIGENDIAN
u_int64_t ntohll(u_int64_t a) { return a; }
#else

u_int64_t ntohll(u_int64_t a) {
	u_int32_t lo = a & 0xffffffff;
	u_int32_t hi = a >> 32U;
	lo = ntohl(lo);
	hi = ntohl(hi);
	return ((u_int64_t) lo) << 32U | hi;
}

#endif
#define htonll ntohll

static void read_all(int fd, char *buf, size_t count) {
	ssize_t bytes_read;

	while (count > 0) {
		bytes_read = read(fd, buf, count);
		assert(bytes_read > 0);
		buf += bytes_read;
		count -= bytes_read;
	}
	assert(count == 0);
}

static void write_all(int fd, char *buf, size_t count) {
	ssize_t bytes_written;

	while (count > 0) {
		bytes_written = write(fd, buf, count);
		assert(bytes_written > 0);
		buf += bytes_written;
		count -= bytes_written;
	}
	assert(count == 0);
}

// Function that distributes calls to the underlying storage structure(s)
int doParent(const int sockParent, buseOperations *bop) {
	ssize_t bytes_read;
	struct nbd_request request;
	struct nbd_reply reply;
	uint32_t len;
	uint64_t from;
	void *chunk;

	reply.magic = htonl(NBD_REPLY_MAGIC);
	reply.error = htonl(0);

	DEBUGPRINTLN("Parent process is about to loop.");

	DEBUGPRINTLN("Starting while loop.");
	while (true) {
		if ((bytes_read = read(sockParent, &request, sizeof(request))) == 0) continue; // interrupt happened
		if(bytes_read == -1) { cerr << "Parent caught error: " << strerror(errno) << endl; return -1; }
		assert(bytes_read == sizeof(request));
		memcpy(reply.handle, request.handle, sizeof(reply.handle));

		len = ntohl(request.len);
		from = ntohll(request.from);
		assert(request.magic == htonl(NBD_REQUEST_MAGIC));

		switch (ntohl(request.type)) {
			/* I may at some point need to deal with the the fact that the
			 * official nbd server has a maximum buffer size, and divides up
			 * oversized requests into multiple pieces. This applies to reads
			 * and writes.
			 */
			case NBD_CMD_READ:
				DEBUGPRINTLN("Request for read of size " << len << " at " << from);
				chunk = aligned_alloc(512, len);
				reply.error = bop->read(chunk, len, from);
				write_all(sockParent, (char *) &reply, sizeof(struct nbd_reply));
				if (reply.error == 0) write_all(sockParent, (char *) chunk, len);
				free(chunk);
				break;
			case NBD_CMD_WRITE:
				DEBUGPRINTLN("Request for write of size " << len << " at " << from);
				chunk = aligned_alloc(512, len);
				read_all(sockParent, (char *) chunk, len);
				reply.error = bop->write(chunk, len, from);
				free(chunk);
				write_all(sockParent, (char *) &reply, sizeof(struct nbd_reply));
				break;
			case NBD_CMD_DISC:
				DEBUGPRINTLN("Request for disconnect.");
				bop->disc();
				return 0;
#ifdef NBD_FLAG_SEND_FLUSH
			case NBD_CMD_FLUSH:
				DEBUGPRINTLN("Request for flush.");
				reply.error = bop->flush();
				write_all(sockParent, (char *) &reply, sizeof(struct nbd_reply));
				break;
#endif
#ifdef NBD_FLAG_SEND_TRIM
			case NBD_CMD_TRIM:
				DEBUGPRINTLN("Request for trim.");
				reply.error = bop->trim(from, len);
				write_all(sockParent, (char *) &reply, sizeof(struct nbd_reply));
				break;
#endif
			default:
				cerr << "Request unknown: " << ntohl(request.type) << endl;
				assert(0);
		}
	}
	cerr << "Snuck out of while(true)" << endl;
	assert(0);
	return -1;
}

void childSIGUSR1Handler(int s) {
	UNUSED(s);
	DEBUGPRINTLN("child: Before DISCONNECT");
	ioctl(nbdChild, NBD_DISCONNECT);
}

void parentSIGUSR1Handler(int s) {
	UNUSED(s);
	// do nothing
}

// Function that catches SIGINT in the child
void childSIGINTHandler(int s) {
	static bool childSIGINTcaught = false;
	UNUSED(s);
	DEBUGPRINTLN("child: Caught signal " << s);
	if (childSIGINTcaught) {
		cout << "Child forcefully shutting down..." << endl;
		exit(-1);
	} else {
		cout << "Please send a USR1 interrupt to gracefully shut down" << endl;
		childSIGINTcaught = true;
	}
}

// Function that catches SIGINT in the parent which stops listening on the socket
void parentSIGINTHandler(int s) {
	static bool parentSIGINTcaught = false;
	UNUSED(s);
	DEBUGPRINTLN("parent: Caught signal " << s);
	if (parentSIGINTcaught) {
		cout << "Parent forcefully shutting down..." << endl;
		exit(-1);
	} else {
		cout << "Please send a USR1 interrupt to gracefully shut down" << endl;
		parentSIGINTcaught = true;
	}
}

int buse_main(const char *dev_file, buseOperations *bop) {
	int sp[2];
	static const int sockParent = 0;
	static const int sockChild = 1;
	int retVal = 0;
	struct sigaction sa;
	sigemptyset(&sa.sa_mask);
	sa.sa_flags = 0;

	//assert(!socketpair(AF_UNIX, SOCK_STREAM, 0, sp)); // create local loopback socket pair for each thread
  if (socketpair(AF_UNIX, SOCK_STREAM, 0, sp) != 0) {
    exit(-1);
  }

	{
		/* Due to a race, the kernel NBD driver cannot call for a reread of the partition table
		 * in the handling of the NBD_DO_IT ioctl(). Therefore, this is done in the first open()
		 * of the device. We therefore make sure that the device is opened at least once after the
		 * connection was made. This has to be done in a separate process, since the NBD_DO_IT ioctl()
		 * does not return until the NBD device has disconnected. */
		int tmp_fd = open(dev_file, O_RDONLY);
		assert(tmp_fd != -1);
		close(tmp_fd);
	}

	// fork() creates an identical set of processes with all the same variables
	int childPID = fork();
	if (!childPID) { // fork() always returns '0' on the child process
		close(sp[sockParent]);
		nbdChild = open(dev_file, O_RDWR | O_LARGEFILE | O_DIRECT | O_SYNC);
		assert(nbdChild != -1);

		sa.sa_handler = childSIGINTHandler;
		sigaction(SIGINT, &sa, NULL);
		sa.sa_handler = childSIGUSR1Handler;
		sigaction(SIGUSR1, &sa, NULL);

		retVal = doChild(sp[sockChild], bop->getSize());
		close(sp[sockChild]);
		DEBUGPRINTLN("Child process finished.");
	}
	else {
		close(sp[sockChild]);
		DEBUGPRINTLN("Parent PID=" << getpid() << ", child PID=" << childPID);

		sa.sa_handler = parentSIGINTHandler;
		sigaction(SIGINT, &sa, NULL);
		sa.sa_handler = SIG_IGN; // parentSIGUSR1Handler;
		sigaction(SIGUSR1, &sa, NULL);

		retVal = doParent(sp[sockParent], bop);
		close(sp[sockParent]); // we are no longer listening to commands on the socket

		delete bop; // Make sure to cleanly close up shop
		DEBUGPRINTLN("Parent process has exited.");
	}

	return retVal;
}
