#!/bin/bash
# Varying the segment sizes

export propertyPath="./etc/ali_property.txt"
export oraclePath="./traces/oracles/"

schemes="NoSep SepGC DAC SFS MultiLog ETI MultiQueue SFR FADaC WARCIP SepBIT FK"
for scheme in $schemes
do
  for groupId in {1..30}
  do
    selection="CostBenefit"
    options="0.15 268435456 2 6"
    bash base.sh ${scheme} ${selection} $options \
      ./etc/ali_groups/group${groupId} \
      tmp/ \
      ./results/exp2/${groupId}/${scheme}_256m.result

    options="0.15 134217728 4 6"
    bash base.sh ${scheme} ${selection} $options \
      ./etc/ali_groups/group${groupId} \
      tmp/ \
      ./results/exp2/${groupId}/${scheme}_128m.result

    options="0.15 67108864 8 6"
    bash base.sh ${scheme} ${selection} $options \
      ./etc/ali_groups/group${groupId} \
      tmp/ \
      ./results/exp2/${groupId}/${scheme}_64m.result
  done
done

cd ../results
for scheme in $schemes
do
  for segsize in "64m" "128" "256m"
  do
    for groupId in {1..30}
    do
      mkdir exp2/${scheme}
      ag "segment WA" exp2/${groupId}/${scheme}_${segsize}.result > exp2/${scheme}/${groupId}_${segsize}.result
      awk '{print $2$16}' exp2/${scheme}/${groupId}_${segsize}.result > tmp; mv tmp exp2/${scheme}/${groupId}_${segsize}
    done
    cat exp2/${scheme}/*_${segsize} > exp2/${scheme}_${segsize}
  done
done

echo "Please check the results in results/exp2 for the WAs of each scheme"
