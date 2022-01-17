#!/bin/bash
# Varying the overall GP for triggering GC

export propertyPath="./etc/ali_property.txt"
export oraclePath="./traces/oracles/"

schemes="NoSep SepGC DAC SFS MultiLog ETI MultiQueue SFR FADaC WARCIP SepBIT FK"
for scheme in $schemes
do
  for groupId in {1..30}
  do
    selection="CostBenefit"
    for gp in 0.10 0.20 0.25
    do
      options="${gp} 536870912 1 6"
      bash base.sh ${scheme} ${selection} $options \
        ./etc/ali_groups/group${groupId} \
        tmp/ \
        ./results/exp3/${groupId}/${scheme}_${gp}.result
    done
  done
done

cd ../results
for scheme in $schemes
do
  for gp in "0.10" "0.20" "0.25"
  do
    for groupId in {1..30}
    do
      mkdir exp3/${scheme}
      ag "segment WA" exp3/${groupId}/${scheme}_${gp}.result > exp3/${scheme}/${groupId}_${gp}.result
      awk '{print $2$16}' exp3/${scheme}/${groupId}_${gp}.result > tmp; mv tmp exp3/${scheme}/${groupId}_${gp}
    done
    cat exp3/${scheme}/*_${gp} > exp3/${scheme}_${gp}
  done
done

echo "Please check the results in results/exp3 for the WAs of each scheme"
