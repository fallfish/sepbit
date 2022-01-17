#include <iostream>
#include <unistd.h>
#include <ctype.h>
#include <stdlib.h>
#include <signal.h>
#include <string.h>

#include "src/buse/buse.h"
#include "src/logstore/logstore.h"
#include "src/logstore/config.h"

using namespace buse;

int main(int argc, char *argv[]) {
//  Config::getInstance().selection = std::string(argv[1]);
  Config::GetInstance().placement = std::string(argv[1]);

  int opt;
  buseOperations *bop = NULL; // Initialize to something 
  bop = new LogStore(300 * 1024 * 1024 * 1024ull);

  alignas(512) char buffer[4096 * 512];
  for (int i = 0; i < 4096 * 512; ++i)
  {
    buffer[i] = 0;
  }
  
  char input[100];
  memset(input, 0, 100);
  sprintf(input, "%s/%s.csv", argv[2], argv[3]);
  FILE *trace = fopen(input, "r");
  char line[100];

  {
    struct timeval current_time;
    gettimeofday(&current_time, NULL);
    printf("seconds: %ld micro seconds: %ld\n",
        current_time.tv_sec, current_time.tv_usec);

  }

  while (fscanf(trace, "%s", line) != EOF)
  {
    std::string str(line);
    std::vector<string> result;
    {
      stringstream ss(str); //create string stream from the string
      while (ss.good()) {
        string substr;
        getline(ss, substr, ','); //get first string delimited by comma
        result.push_back(substr);
      }    
    }
      
    if (result[1] != "W") continue;
    
    uint64_t start = atoll(result[2].c_str());
    uint32_t length = atoi(result[3].c_str());
    uint64_t end = start + length;
    start = start & (~4095ull);
    length = ((end + 4095) & (~4095ull)) - start;
    bop->write(buffer, length, start);
  }

  {
    struct timeval current_time;
    gettimeofday(&current_time, NULL);
    printf("seconds: %ld micro seconds: %ld\n",
        current_time.tv_sec, current_time.tv_usec);

  }

  delete bop;
}
