#!/bin/bash
# Run Tencent Cloud traces

export propertyPath="./etc/tencent_property.txt"
export oraclePath="./tencent_traces/oracles/"

schemes="NoSep SepGC DAC SFS MultiLog ETI MultiQueue SFR FADaC WARCIP SepBIT FK"
options="0.15 536870912 1 6"

for scheme in $schemes
do
  echo "Running ${scheme}"
  for groupId in {1..20}
  do
    for selection in "CostBenefit"
    do
      bash base.sh ${scheme} ${selection} $options \
        ./etc/tencent_groups/group${groupId} \
        tmp/ \
        ./results/exp6/${groupId}/${scheme}_${selection}.result
    done
  done
done

cd ../results
for scheme in $schemes
do
  for selection in "CostBenefit"
  do
    for groupId in {1..10}
    do
      mkdir exp6/${scheme}
      ag "segment WA" exp6/${groupId}/${scheme}_${selection}.result > exp6/${scheme}/${groupId}_${selection}.result
      awk '{print $2$16}' exp6/${scheme}/${groupId}_${selection}.result > tmp; mv tmp exp6/${scheme}/${groupId}_${selection}
    done
    cat exp6/${scheme}/*_${selection} > exp6/${scheme}_${selection}
  done
done

echo "Please check the results in results/exp6 for the WAs of each scheme"
