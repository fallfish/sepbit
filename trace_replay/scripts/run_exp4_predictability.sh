#!/bin/bash
# Extract per-segment GP out of Exp1 results for predictability analysis
# The extracted result is in the form of logid, array of [gps]; numeric analysis needs further hand-writing scripts
# Hint: Python can parse array of [gps] using ast.literal_eval

schemes="NoSep SepGC DAC WARCIP SepBIT"
cd ../results

mkdir exp4
for scheme for $schemes
do
  python3 obtain_removed_seg.py exp1/*/${scheme}_CostBenefit.result > exp4/${scheme}
done
