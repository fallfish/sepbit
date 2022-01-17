add_library(placement no_placement.cc sepbit.cc sepgc.cc dac.cc warcip.cc)

target_include_directories(placement PUBLIC ${PROJECT_SOURCE_DIR})
target_compile_features(placement PRIVATE cxx_std_17)
