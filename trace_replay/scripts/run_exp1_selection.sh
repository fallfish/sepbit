#!/bin/bash
# Varying selection algorithms between Greedy and Cost-Benefit

export propertyPath="./etc/ali_property.txt"
export oraclePath="./traces/oracles/"
schemes="NoSep SepGC DAC SFS MultiLog ETI MultiQueue SFR FADaC WARCIP SepBIT FK"
options="0.15 536870912 1 6"

for scheme in $schemes
do
  echo "Running ${scheme}"
  for groupId in {1..30}
  do
    for selection in "Greedy" "CostBenefit"
    do
      bash base.sh ${scheme} ${selection} $options \
        ./etc/ali_groups/group${groupId} \
        tmp/ \
        ./results/exp1/${groupId}/${scheme}_${selection}.result
    done
  done
done

cd ../results
for scheme in $schemes
do
  for selection in "Greedy" "CostBenefit"
  do
    for groupId in {1..30}
    do
      mkdir exp1/${scheme}
      ag "segment WA" exp1/${groupId}/${scheme}_${selection}.result > exp1/${scheme}/${groupId}_${selection}.result
      awk '{print $2$16}' exp1/${scheme}/${groupId}_${selection}.result > tmp; mv tmp exp1/${scheme}/${groupId}_${selection}
    done
    cat exp1/${scheme}/*_${selection} > exp1/${scheme}_${selection}
  done
done

echo "Please check the results in results/exp1 for the WAs of each scheme"
