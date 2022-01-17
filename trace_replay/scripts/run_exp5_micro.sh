#!/bin/bash
# Microbenchmark that examies UW and GW

schemes="UW GW"
options="0.15 536870912 1 6"
for scheme in $schemes
do
  for groupId in {1..30}
  do
    selection="CostBenefit"
    bash base.sh ${scheme} ${selection} $options \
      ./etc/ali_groups/group${groupId} \
      tmp/ \
      exp5/${groupId}/${scheme}_${selection}.result
  done
done

cd ../results
for scheme in $schemes
do
  do
    for groupId in {1..30}
    do
      mkdir exp5/${scheme}
      ag "segment WA" exp5/${groupId}/${scheme}_${CostBenefit}.result > exp5/${scheme}/${groupId}_${CostBenefit}.result
      awk '{print $2$16}' exp5/${scheme}/${groupId}_${CostBenefit}.result > tmp; mv tmp exp5/${scheme}/${groupId}_${CostBenefit}
    done
    cat exp5/${scheme}/*_${CostBenefit} > exp5/${scheme}_${CostBenefit}
  done
done

echo "Please check the results in results/exp5 for the WAs of each scheme"
