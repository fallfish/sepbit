add_library(selection greedy.cc costbenefit.cc)

target_include_directories(selection PUBLIC ${PROJECT_SOURCE_DIR})
target_compile_features(selection PRIVATE cxx_std_17)
