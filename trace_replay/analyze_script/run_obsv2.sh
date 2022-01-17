#!/bin/bash

source etc/common.sh

analyze_multiple_files "obsv2" "obsv2" "src/obsv2.cc" "obsv2" "Observation 2" 
merge "obsv2" "obsv2.data" "log value type"

cd r
Rscript obsv2.r

############ Uncomment these lines if you want to plot the figures
#if [[ ! -d ../figure ]] ; then
#  mkdir -p ../figure
#fi
#Rscript obsv2.r plot  
