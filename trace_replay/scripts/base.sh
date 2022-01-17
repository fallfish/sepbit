export MAVEN_OPTS="-XX:ParallelGCThreads=3 -Xmx10g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./dumps"

# data placement scheme
separateMethod=$1
# varying selection algorithm
selectionAlgorithm=$2
# varying GP
garbageProportionThreshold=$3
# varying segment sizes
segmentSize=$4
numPickSegs=$5
# varying number of classes
numClasses=$6

# input and output
inputTrace=$7
temporaryDir=$8
rawOutputFile=$9


# enter working directory
cd ../

# create directory
mkdir -p $rawOutputFile
rm -r $rawOutputFile

run() {
  rm -r $temporaryDir
  mkdir -p $temporaryDir/segments
  mkdir -p $temporaryDir/indexmap
  mkdir -p $temporaryDir/ondiskfifo

  mvn exec:java -Dexec.mainClass="gcsimulator.Simulator" \
    -Dexec.args="--path ${inputTrace} --outputPrefix ${temporaryDir} ${options} ${hardcodedOptions}" \
    | tee $rawOutputFile
}

hardcodedOptions="--propertyPath ${propertyPath} --oraclePath ${oraclePath}"

options=""
options="${options} --setSeparateMethod ${separateMethod}"
options="${options} --selectionAlgorithm ${selectionAlgorithm}"
options="${options} --setSystemGarbageProportionThreshold ${garbageProportionThreshold}"
options="${options} --setSegmentSize ${segmentSize} --setPickSegAmount ${numPickSegs}"
options="${options} --setNumOpenSegments ${numClasses}"
echo $options

run
