add_executable(app main.cc)
target_compile_features(app PRIVATE cxx_std_17)
target_link_libraries(app PRIVATE logstore buse rt pthread)
