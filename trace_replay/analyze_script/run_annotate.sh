#!/bin/bash

source etc/common.sh

# Both volumes use the format of AliCloud
analyze_multiple_files "annotate" "annotate" "src/annotate_future_knowledge.cc" "annotate" "Annotate future knowledge" 

cd result/annotate
for line in `ls *.data`; do
  nm=`echo $line | cut -d. -f 1`
  mv $line ${nm}.oracle
done


