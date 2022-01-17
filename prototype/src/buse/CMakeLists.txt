add_library(buse buse.cpp buseOperations.cpp diskStats.cpp)

target_include_directories(buse PUBLIC ${PROJECT_SOURCE_DIR})
target_compile_features(buse PRIVATE cxx_std_17)
