## Scripts for Trace Analysis

These C++ programs and R scripts are used for trace analysis. After you download the traces, you need to use the script `split.sh` to split the trace file (with hundreds of GiBs) into different volumes. Then you can run the scripts `run*.sh` to finish the processing. You can also plot the figures if needed (by adding the parameter "plot" to the Rscript).

After you download the Alibaba Cloud and Tencent Cloud traces, modify `etc/common.sh` to set the paths of the both traces, and run the following to split the trace into independent csv files for each volume:
```
$ vim etc/common.sh # Edit the paths
$ ./split.sh
```

Then you also need to select which set of traces you want to analyze, in `etc/common.sh`.

### About Motivation

After the previous steps are finished, you can run the scripts to get the results of Observations 1-3.
```
$ ./run_obsv1.sh
$ ./run_obsv2.sh
$ ./run_obsv3.sh
```

+ `src/obsv1.cc` will get the lifespans of user-written blocks. It provides the results for Figure 3.
+ `src/obsv2.cc` will get the CVs of the lifespans of blocks with similar (and high) frequency. It provides the results for Figure 4.
+ `src/obsv3.cc` will get the lifespans of rarely updated blocks. It provides the results for Figure 5.

### About Design

You can run the scripts to get the results of design.
```
$ ./run_design.sh
```

+ `src/design_uw.cc` will get the probability of $u<=u_0$ for the user writes conditioning on $v<=v_0$, under different $v_0$ and $u_0$ values as different fractions of the write WSS.
+ `src/design_gw.cc` will get the probability of $u<=r_0+g_0$ for the user writes conditioning on $u>=g_0$, under different $r_0$ and $g_0$ values as different times of the write WSS.

### About Evaluation

To get the result of FK, you need to run the following script to annotate the trace in advance:
```
$ ./run_annotate.sh
```

To generate the synthetic traces in Experiment 5, run the following:
```
$ ./synthetic_gen.sh
```

To calculate the percetage of traffic in 20\% of the LBAs, run the following;
```
$ ./run_exp5_hot20.sh
```
