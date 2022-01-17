#!/bin/bash

source etc/common.sh

analyze_multiple_files "exp5_hot20" "exp5_hot20" "src/exp5_hot20.cc" "exp5_hot20" "Traffic in Hot 20% blocks" 
merge "exp5_hot20" "exp5_hot20.data" "log pct"
