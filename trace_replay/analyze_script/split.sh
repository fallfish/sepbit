#!/bin/bash

source etc/common.sh

if [[ ! -d bin ]]; then
  mkdir bin
fi

################# Split the Alibaba Cloud traces
g++ src/split.cc -o bin/split -std=c++11 -O3 -DALICLOUD
bin/split $ALI_DOWNLOAD_FILE_PATH $ALI_TRACE_PATH

################# Split and transform timestamps of the Tencent Cloud traces
g++ src/split.cc -o bin/split -std=c++11 -Wall -Werror -O3 -DTENCENTCLOUD
if [[ ! -d $TENCENT_TRACE_PATH ]]; then
  echo "TENCENT_TRACE_PATH not set or not exist; please set in etc/common.sh"
  exit
fi
if [[ ! -d $TENCENT_DOWNLOAD_DIR_PATH ]]; then
  echo "TENCENT_DOWNLOAD_DIR_PATH not set or not exist; please set in etc/common.sh"
  exit
fi

for tgz_file in `ls ${TENCENT_DOWNLOAD_DIR_PATH}/*.tgz`; do
  echo "Extracting $tgz_file"
  name=`echo $tgz_file | rev | cut -d'/' -f 1 | rev | cut -d'.' -f 1`
  tar xzf $tgz_file 
  INPUT="cbs_trace1/atc_2020_trace/trace_ori/${name}"
  echo "Spliting $INPUT"
  bin/split $INPUT $TENCENT_TRACE_PATH
  rm $INPUT
done

transform() {
  bin_suffix=$1;
  src=$2;

  bin="bin/tc_${bin_suffix}"
  if [[ ! -d bin ]]; then
    mkdir bin
  fi

  property_file="etc/tc_property.txt"
  echo "Transforming Tencent Cloud ... output at directory $TENCENT_TRACE_PATH"

  g++ $src -o $bin -std=c++11 -DTENCENTCLOUD
  if [[ $? -ne 0 ]]; then 
    echo "Compile failed"
    exit
  fi

  cat etc/tc_selected_271.txt | while read line; do
    trace_file=$TENCENT_TRACE_PATH/$line.csv
    if [[ ! -f $trace_file ]]; then
      echo "$trace_file not exist; did you download the traces or use split.sh to split the traces?"
      exit
    fi
    echo "transforming $trace_file in TencentCloud"
    output=${TENCENT_TRACE_PATH}/${line}_tmp.csv
    sz=`ls -s ${output} 2>/dev/null | awk '{print $1;}'`
    if [[ $? -ne 0 || $sz -eq 0 ]]; then  # Not exist, or empty file
      $bin $line $trace_file $property_file >> $output
    else
      echo "Volume $line in Tencent Cloud is transformed before, skip"
    fi
    mv $output $trace_file
  done
}

transform "transform" "src/transform_timestamp_tencentCloud.cc" 

