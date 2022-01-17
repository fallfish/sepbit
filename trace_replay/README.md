# SepBIT

This project implements a simulator for log-structured storage systems; it includes multiple state-of-the-art data placement algorithms and a novel data placement algorithm named SepBIT.

### Preface

This repository contains the following folders:

* **src**: The source code of our simulator.
* **etc**: property files we use during simulation
* **analysis_script**: The scripts for analysis on the traces, the calculation of probabilities (including both mathematical distribution and real-world traces), and the plotting scripts on these results.
* **scripts**: The scripts used in experiments, including the command to run the experiments and parse the results.

### Features
* Support in total 12 data placement algorithms (including SepBIT itself)
* Support six selection algorithms 
* Support efficient on-disk indexmap (a flat on-disk array with page-aligned units cached in memory)
    * Enable the simulation for large-scale dataset or global management of multiple (tens or hundreds) disk volumes
* Extensibility to plug in more data placement algorithms and selection algorithms

### Run the simulator and experiments
#### Preprocess traces
* Download traces from [Alibaba Cloud block traces](https://github.com/alibaba/block-traces), [Tencent Cloud block storage traces](http://iotta.snia.org/traces/27917), or generate synthetic traces using analysis_scripts/synthetic_gen.sh
* Run the script in analyze_scripts/split.sh and store the per-volume traces under ali_traces/, tencent_traces/, or synthetic_traces/
* For FK, run analyze_scripts/run_annotate.sh for each volume and store the output under oracles/ in the traces folders
* Refer to the analyze_scripts/README.md for details of preprocessing the traces

#### Compilation
```
mvn package
```
#### Running
```
mvn exec:java -Dexec:mainClass="gcsimulator.Simulator" -Dexec.args="${parameters}"
```

* The must added parameters include:
```
--path: specify the path of trace file lists (.txt) containing a set of traces to run (for samples see etc/groups).
--propertyPath: a file stores (volume_id, write WSS, maximum LBA). IndexMapCache can allocate memory accordingly (for samples see etc/).
--selectionAlgorithm: specify the selection algorithm
--setSeparateMethod: specifiy the data placement algorithm 
--setNumOpenSegments: specify the number of separated classes 
--outputPrefix: specify the temporary directory to store on-disk indexmap, segment metadata, and fifo queue; must be created in advance
```

* Other parameters:
```
--setSystemGarbageProportionThreshold: specify the GP threshold for triggering
--setSegmentSize: specify the segment sizes
--setPickSegAmount: specify the number of segments to collect during each GC operation
--indexMapType: specify the type of index map (by default Persistent) (see indexmap/IndexMapFactory.java)
--indexMapCache: specify how index map stores in memory for memory-efficient simulation (by default GlobalPageCache) (see indexmap/IndexMapFactory.java)
// Oracles must be generated in advanace based on the traces
--oraclePath: specify a directory that stores the future knowledge of lifespan for each written block
```

* Shell command example:
```
mvn exec:java -Dexec.mainClass=gcsimulator.Simulator -Dexec.args=\
  --path ./etc/groups/group1 --outputPrefix ./tmp --propertyPath ./etc/ali_property.txt \
  --selectionAlgorithm CostBenefit --setSeparateMethod SepBIT --setNumOpenSegments 6 \
```
It outputs the runtime GC information for each GC, and a summary of detailed write amplification information, including the overall WA, segment volume WA, and the blocks inside each segments given a set of traces.

#### Evaluation
For all experiments, we have prepared running scripts under scripts/. Please refer to each individual scripts for details. THe scripts include how to run and how to parse the results. There are hard-coded paths for storing results; feel free to modify those paths.
