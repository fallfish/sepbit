#!/bin/bash
# Note: set the traceDir variable to the one you put the trace in
# Run this script under your build directory
traceDir="/mnt/data/alibaba_trace"
mkdir results/
for volume in `cat ./volume_list.txt`
do
	echo $volume
	for placement in "SepBIT" "DAC" "SepGC" "WARCIP" "NoSep"
	do
		sudo ../build/app/app $placement $traceDir $volume > results/${volume}_${placement}.result
	done
done
