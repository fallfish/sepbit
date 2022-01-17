#!/bin/bash

source etc/common.sh

analyze_multiple_files "obsv1" "obsv1" "src/obsv1.cc" "obsv1" "Observation 1" 
merge "obsv1" "obsv1.data" "wss pct"

cd r 
Rscript obsv1.r

############ Uncomment these lines if you want to plot the figures
#if [[ ! -d ../figure ]] ; then
#  mkdir -p ../figure
#fi
#Rscript obsv1.r plot  
