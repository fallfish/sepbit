package gcsimulator.tracereplay;

import gcsimulator.Configs;
import gcsimulator.iorequest.*;
import gcsimulator.Log;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The dispatcher.
 */
public class TraceReplay {
  class RequestQueue {
    // For mode DIR: store a chain of files that mix all requests
    // For mode DEVICE: store a chain of files that only represents the DEVICE
    //    We shall interpret the log id by its file name
    List<String> fileNames;
    String logId;
    BufferedReader reader;
    IORequest nextRequest;
    IORequest currentRequest;
    Integer nextFileId;

    IORequest getRequest() {
      IORequest ret;
      if (this.nextRequest == null) return null;

      this.currentRequest = this.nextRequest;
      ret = this.currentRequest;

      try {
        readNextRequest();
      } catch (IOException e) {
        e.printStackTrace();
      }

      return ret;
    }

    private BufferedReader openFile() throws FileNotFoundException {
      if (nextFileId == fileNames.size()) return null;
      System.out.println("openFile: " + fileNames.get(nextFileId));
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(
              new FileInputStream(fileNames.get(nextFileId))
          )
      );
      nextFileId ++;
      return reader;
    }

    void readNextRequest() throws IOException {
      if (reader == null) {
        nextRequest = null;
        return;
      }

      String str = reader.readLine();

      while (true) {
        if (str == null) {
          reader = openFile();
          if (reader == null) {
            nextRequest = null;
            return;
          }

          str = reader.readLine();
        }

        if (str == null) {
          continue;
        }

        if (Configs.requestType.equals(Configs.RequestTypeEnum.ALI)) {
          nextRequest = new AliIORequest(str);
        } 

        if (!nextRequest.isWrite()) {
          str = reader.readLine();
          continue;
        }
        break;
      }
    }

    RequestQueue(List<String> files) {
      if (Configs.requestType.equals(Configs.RequestTypeEnum.ALI)) {
        this.nextRequest = new AliIORequest();
        this.currentRequest = new AliIORequest();
      }

      this.fileNames = files;
      this.fileNames.sort(String::compareTo);
      if (this.fileNames.size() == 1) {
          int beg = fileNames.get(0).lastIndexOf('/');
          logId = fileNames.get(0).substring(beg + 1);
          logId = logId.substring(0, logId.indexOf('.'));
      }
      this.nextFileId = 0;

      try {
        reader = openFile();
        readNextRequest();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // the index represents the queue id
  class IORequestWithIndex {
    IORequest ioRequest;
    Integer index;

    IORequestWithIndex(IORequest ioRequest, Integer index) {
      this.ioRequest = ioRequest;
      this.index = index;
    }
  }

  Comparator<IORequestWithIndex> ioRequestWithIndexComparator = (s1, s2) -> {
    if (s1.ioRequest.timestampInUs < s2.ioRequest.timestampInUs) return -1;
    if (s1.ioRequest.timestampInUs == s2.ioRequest.timestampInUs) {
      if (s1.index < s2.index) return -1; 
      // Impossible to be equal: impossible for 2 request with the same index existing in the priority queue
      
      return 1;
    }
    return 1;
  };

  private List<RequestQueue> queues;
  private PriorityQueue<IORequestWithIndex> ioRequestWithIndices;

  public TraceReplay() {
    queues = new ArrayList<>();
    ioRequestWithIndices = new PriorityQueue<>(ioRequestWithIndexComparator);
  }

  public void init(String path) throws IOException {
    IORequest firstRequest;
    System.out.println("Init: " + path);
    RequestQueue queue;
    int index = 0;

    List<String> result;
    if (Configs.traceReplayMode == Configs.TraceReplayModeEnum.DEV_LIST) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
      String filename;
      filename = reader.readLine();

      // "Result" stores a single filename
      while (filename != null) {
        System.out.println(filename);
        result = new ArrayList<>();
        result.add(filename);

        queue = new RequestQueue(result);
        queues.add(queue);
        firstRequest = queue.getRequest();

        if (firstRequest != null) ioRequestWithIndices.offer(new IORequestWithIndex(firstRequest, index++));

        filename = reader.readLine();
      }
    }
  }

  public IORequest getNextRequest() {
    RequestQueue nextDevice;
    IORequest ioRequest;
    int index;
    IORequestWithIndex ioRequestWithIndex = null;
    IORequest nextRequest;

    while (!ioRequestWithIndices.isEmpty()) {
      ioRequestWithIndex = ioRequestWithIndices.poll();
      if (ioRequestWithIndex != null) break;
    }
    if (ioRequestWithIndex == null) return null;

    index = ioRequestWithIndex.index;
    ioRequest = ioRequestWithIndex.ioRequest;

    nextDevice = queues.get(index);
    nextRequest = nextDevice.getRequest();

    if (nextRequest != null)
      ioRequestWithIndices.offer(new IORequestWithIndex(nextRequest, index));

    return ioRequest;
  }
}
