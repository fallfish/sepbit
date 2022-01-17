#!/bin/bash
# The number of LBAs in the FIFO for memory usage analysis
ag "FIFO" ../results/exp1/sepbit/*_CostBenefit.result > tmp
awk -F':' 'BEGIN {OFS=FS} {$1=$2=$3=""; print substr($0,5)}' tmp > tmp1

# sort by volume id
sort -t, -k1 -s -n tmp1 > sort_res

# output the results
mkdir ../results/exp8
python3 process_fifo_len.py sort_res > ../results/exp8/

# clean
rm tmp tmp1 sort_res
