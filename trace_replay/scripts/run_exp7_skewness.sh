#!/bin/bash
# Run the experiments of skewness (synthetic)
# For the per-volume skewness in the Alibaba Cloud traces, please refer to **analyze_script/run_expa5_hot20.sh**

export propertyPath="./etc/synthetic_property.txt"
export oraclePath="./synthetic_traces/oracles/"

schemes="NoSep SepGC DAC SFS MultiLog ETI MultiQueue SFR FADaC WARCIP SepBIT FK"
options="0.15 536870912 1 6"
for scheme in $schemes
do
  echo "Running ${scheme}"
  for selection in "Greedy"
  do
    bash base.sh ${scheme} ${selection} $options \
      ./etc/synthetic_groups/all \
      tmp/ \
      ./results/exp7/${groupId}/${scheme}_${selection}.result
  done
done

cd ../results
for scheme in $schemes
do
  for selection in "Greedy"
  do
    mkdir exp7/${scheme}
    ag "segment WA" exp7/${groupId}/${scheme}_${selection}.result > exp7/${scheme}/${groupId}_${selection}.result
    awk '{print $2$16}' exp7/${scheme}/${groupId}_${selection}.result > tmp; mv tmp exp7/${scheme}/${groupId}_${selection}
    cat exp7/${scheme}/*_${selection} > exp7/${scheme}_${selection}
  done
done

echo "Please check the results in results/exp7 for the WAs of each scheme"
