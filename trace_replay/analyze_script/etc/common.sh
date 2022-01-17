#!/bin/bash

########### Modify here to set the directory of traces
ALI_DOWNLOAD_FILE_PATH=""  # The csv trace file you downloaded from the Alibaba GitHub
TENCENT_DOWNLOAD_DIR_PATH="" # The directory you downloaded from the SNIA, containing tgz files
ALI_TRACE_PATH="/sdb_data/ali_trace/" # The directory that contains the traces split by volumes
TENCENT_TRACE_PATH="/sdc_data/tencent_trace/" # The directory that contains the traces split by volumes 
SYNTHETIC_PATH=""
SELECTED="0" # 0 for analysis of Alibaba Cloud, or 1 for Tencent Cloud 
########################################################

TRACE_PATH=$ALI_TRACE_PATH
TRACE_PREFIX="ali"
TRACE_PROPERTY="etc/ali_property.txt"
TRACE_VOLUME_PATH="etc/ali_selected_186.txt"
TRACE_DISPLAY_NAME="AliCloud"

# Both volumes use the format of AliCloud

if [[ $SELECTED -eq 1 ]]; then
  TRACE_PATH=$TENCENT_TRACE_PATH
  TRACE_PREFIX="tc"
  TRACE_PROPERTY="etc/tc_property.txt"
  TRACE_VOLUME_PATH="etc/tc_selected_271.txt"
  TRACE_DISPLAY_NAME="TencentCloud"
fi

if [[ ! -d $TRACE_PATH ]]; then
  echo "TRACE_PATH not set; please set in etc/common.sh"
  exit
fi

analyze_multiple_files() {
  bin_suffix=$1;
  output_suffix=$2;
  src=$3;
  disp=$4;
  params=()

  if [[ $# -gt 4 ]]; then
    params=("${@:5}");
    echo "${params[@]}"
  fi

  bin="bin/${TRACE_PREFIX}_${bin_suffix}"
  output_dir="result/${TRACE_PREFIX}_${output_suffix}"
  if [[ ! -d $output_dir ]]; then
    mkdir -p $output_dir
  fi
  if [[ ! -d bin ]]; then
    mkdir bin
  fi

  echo "Analyzing $TRACE_DISPLAY_NAME traces on $disp ... output at directory $output_dir"

  g++ $src -o $bin -std=c++11 -O3 -DALICLOUD -Werror -Wall
  if [[ $? -ne 0 ]]; then 
    echo "Compile failed"
    exit
  fi

  total_traces=`wc -l ${trace_file_paths[$K]} | awk '{print $1;}'`
  current_traces=0

  cat $TRACE_VOLUME_PATH | while read line; do
    current_traces=$(( $current_traces + 1 ))
    echo "Processing volume ${current_traces} / ${total_traces}"

    trace_file=$TRACE_PATH/$line.csv
    output=${output_dir}/$line.data
    sz=`ls -s ${output} 2>/dev/null | awk '{print $1;}'`
    if [[ $? -ne 0 || $sz -eq 0 ]]; then  # Not exist, or empty file
      $bin $line $trace_file $TRACE_PROPERTY ${params[@]} >> $output
      if [[ $? -ne 0 ]]; then
        echo "have error on volume $line, break" >> error_msg.txt
        sleep 2
      fi
    else
      echo "Volume $line in ${TRACE_DISPLAY_NAME} is analyzed before, skip"
    fi
  done
}

merge() {
  input_suffix=$1;
  output_suffix=$2;
  header=$3;
  
  output="result/${TRACE_PREFIX}_${output_suffix}"
  rm -f $output

  if [[ "${#header}" -gt 0 ]]; then
    echo "$header" >> $output
  fi
    
  cat $TRACE_VOLUME_PATH | while read line; do
    input="result/${TRACE_PREFIX}_${input_suffix}/${line}.data"
    if [[ ! -f $input ]]; then
      echo "Error: input $input not exist"
      return 1
    fi
    cat $input >> $output
  done
}
