add_library(indexmap hashmap.cc array.cc)

target_include_directories(indexmap PUBLIC ${PROJECT_SOURCE_DIR})

target_compile_features(indexmap PRIVATE cxx_std_17)
