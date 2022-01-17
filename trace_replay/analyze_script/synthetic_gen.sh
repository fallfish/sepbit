#!/bin/bash

source etc/common.sh

if [[ ! -d $SYNTHETIC_PATH ]]; then
  echo "SYNTHETIC_PATH not set or not exist; please set in etc/common.sh"
  exit
fi

alphas=("0" "0.2" "0.4" "0.6" "0.8" "1")

for ((i=0; i<${#alphas[@]}; i++)); do
  alpha=${alphas[$i]}
  Rscript r/synthetic.r ${alpha} $SYNTHETIC_PATH
  input_path="$SYNTHETIC_PATH/alpha_${alpha}.csv"

  vol_num="$i"
  output="$SYNTHETIC_PATH/${vol_num}.csv"

  if [[ ! -f $output ]]; then
    awk 'BEGIN {s=1;} {print "'"$vol_num"',W," $1*4096 ",4096," s; s=s+1;}' $input_path > $output
    head $output
  else
    echo "exist"
  fi

  rm $input_path
done
