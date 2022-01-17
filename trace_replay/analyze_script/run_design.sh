#!/bin/bash

source etc/common.sh

analyze_multiple_files "design_uw" "design_uw" "src/design_uw.cc" "design_uw" "Design Analysis for user writes" 
merge "design_uw" "design_uw.data" "log v0 u0 denominator numerator prob"

analyze_multiple_files "design_gw" "design_gw" "src/design_gw.cc" "design_gw" "Design Analysis for GC writes" 
merge "design_gw" "design_gw.data" "log r0 g0 numerator denominator prob"

cd r
Rscript design_calculation.r
Rscript plot_design_lines.r
Rscript plot_design_traces.r
