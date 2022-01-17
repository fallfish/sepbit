add_library(logstore logstore/logstore.cc logstore/manager.cc logstore/segment.cc logstore/scheduler.cc)

add_subdirectory(indexmap)
add_subdirectory(placement)
add_subdirectory(selection)
add_subdirectory(storage_adapter)
add_subdirectory(buse)

target_link_libraries(logstore indexmap placement selection storage_adapter)
target_compile_features(logstore PRIVATE cxx_std_17)
