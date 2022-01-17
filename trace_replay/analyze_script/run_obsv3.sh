#!/bin/bash

source etc/common.sh

analyze_multiple_files "obsv3" "obsv3" "src/obsv3.cc" "obsv3" "Observation 3" 
merge "obsv3" "obsv3.data" "log type pct"

cd r 
Rscript obsv3.r

############ Uncomment these lines if you want to plot the figures
#if [[ ! -d ../figure ]] ; then
#  mkdir -p ../figure
#fi
#Rscript obsv3.r plot  
