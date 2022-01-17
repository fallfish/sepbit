find_package(PkgConfig REQUIRED)
pkg_check_modules(ROCKSDB REQUIRED rocksdb)

add_library(storage_adapter local_adapter.cc zenfs_adapter.cc)

target_include_directories(storage_adapter PUBLIC ${PROJECT_SOURCE_DIR})
target_compile_features(storage_adapter PRIVATE cxx_std_17)
target_link_libraries(storage_adapter rocksdb dl pthread rt snappy bz2 tcmalloc zbd z)
