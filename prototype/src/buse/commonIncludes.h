#ifndef COMMONINCLUDES_H
#define COMMONINCLUDES_H

//#define _GNU_SOURCE
//#define _LARGEFILE64_SOURCE

//#define DEBUG
#include <sys/types.h>
#include <stdint.h>
#include <unistd.h>
#include <chrono>
#include <ctime>
#include <iostream>
#include <iomanip>

using namespace std;

// stole this part from: http://stackoverflow.com/questions/16692400/c11-adding-a-stream-output-operator-for-stdchronotime-point
namespace prettyPrint {
	template<typename T>
	struct printWrapper { // boost::noopy optional -- if so, use it with && as an argument
		T const &data;

		printWrapper(T const &t) : data(t) { }
	};

	template<typename T>
	printWrapper<T> format(T const &t) { return {t}; }

	template<typename Rep, typename Period>
	std::ostream &operator<<(std::ostream &stream, const printWrapper<std::chrono::duration<Rep, Period>> &&duration) {
		return stream << std::chrono::duration_cast<std::chrono::microseconds>(duration.data).count() << "us";
	}

	template<typename Clock, typename Duration>
	std::ostream &operator<<(std::ostream &stream, const printWrapper<std::chrono::time_point<Clock, Duration>> &&time_point) {
		const time_t time = Clock::to_time_t(time_point.data);
#if __GNUC__ > 4 || ((__GNUC__ == 4) && __GNUC_MINOR__ > 8 && __GNUC_REVISION__ > 1)
		// Maybe the put_time will be implemented later?
		struct tm tm;
		localtime_r(&time, &tm);
		return stream << std::put_time(&tm, "c");
#else
		char buffer[26];
		ctime_r(&time, buffer);
		buffer[24] = '\0';  // Removes the newline that is added
		return stream << buffer;
#endif
	}
}

inline ssize_t commonIncludesRead(int fd, void *buf, size_t count) { return ::read(fd,buf,count); }
inline ssize_t commonIncludesWrite(int fd, void *buf, size_t count) { return ::write(fd,buf,count); }

#define likely(x)	__builtin_expect(!!(x), 1)
#define unlikely(x)	__builtin_expect(!!(x), 0)

#ifdef DEBUG

#include <boost/thread/mutex.hpp>
#include <boost/thread/lock_guard.hpp>
#include <boost/serialization/singleton.hpp>

#define DEBUGCODE(X) do {\
	boost::lock_guard<boost::mutex> guard(boost::serialization::singleton<boost::mutex>::get_mutable_instance());\
    cerr << prettyPrint::format(std::chrono::system_clock::now()) << ':' << __FILE__ << ':' << __LINE__ << ": " << endl;\
    { X; };\
    } while(0);
#define DEBUGPRINTLN(X) do {\
	boost::lock_guard<boost::mutex> guard(boost::serialization::singleton<boost::mutex>::get_mutable_instance());\
    cerr << prettyPrint::format(std::chrono::system_clock::now()) << ':' << __FILE__ << ':' << __LINE__ << ": " << X << endl;\
    } while(0);
#else
#define DEBUGCODE(X)
#define DEBUGPRINTLN(X)
//#define DEBUGCODE(X) std::cout << X << std::endl;
//#define DEBUGPRINTLN(X) std::cout << X << std::endl;
#endif

#define UNUSED(expr) (void)(expr)

#endif
