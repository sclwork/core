# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.6.0)






#########################################################################
#########################################################################
### ucrop
#########################################################################
##设置变量LAMEMP_SRC_DIR为ucrop的所在路径
#set(UCROP_SRC_DIR src/main/cpp/ucrop)
#
##指定头文件所在，可以多次调用，指定多个路径
#include_directories(src/main/cpp/ucrop)
#
##设定一个目录
#aux_source_directory(src/main/cpp/ucrop UCROP_SRC_DIR)
#
##将前面目录下所有的文件都添加进去
#add_library( # Sets the name of the library.
#        ucrop
#
#        # Sets the library as a shared library.
#        SHARED
#
#        # Provides a relative path to your source file(s).
#        src/main/cpp/ucrop/uCrop.cpp
#        ${UCROP_SRC_DIR} )
#
#find_library( # Sets the name of the path variable.
#        log-lib
#
#        # Specifies the name of the NDK library that
#        # you want CMake to locate.
#        log )
#
#target_link_libraries( # Specifies the target library.
#        ucrop
#
#        # Links the target library to the log library
#        # included in the NDK.
#        ${log-lib}
#        android
#        z
#        png
#        jpeg )






########################################################################
########################################################################
## core
########################################################################
# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             jcore

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/core.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       jcore

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )




########################################################################
########################################################################
## lamemp3
########################################################################
#设置变量LAMEMP_SRC_DIR为lamemp3的所在路径
set(LAMEMP_SRC_DIR src/main/cpp/lamemp3)

#指定头文件所在，可以多次调用，指定多个路径
include_directories(src/main/cpp/lamemp3)

#设定一个目录
aux_source_directory(src/main/cpp/lamemp3 LAMEMP_SRC_DIR)

#将前面目录下所有的文件都添加进去
add_library( # Sets the name of the library.
             jmedia

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/media.cpp
             ${LAMEMP_SRC_DIR} )

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

target_link_libraries( # Specifies the target library.
                       jmedia

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
########################################################################
########################################################################
## lamemp3
########################################################################





#########################################################################
#########################################################################
### WebRTC支持如下CPU架构['arm64-v8a']
#########################################################################
#if (CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
#
#add_definitions( -DWEBRTC_ANDROID
#                 -DWEBRTC_POSIX
#                 -DWEBRTC_NS_FIXED )
#
## 设置编译类型-Debug or Release
#SET(CMAKE_BUILD_TYPE "Release")
#
## 设置编译参数
#if( CMAKE_BUILD_TYPE STREQUAL "Debug" )
#    SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -Wall -Wno-unused-variable -pthread")
#elseif( CMAKE_BUILD_TYPE STREQUAL "Release" )
#    SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -pthread -fopenmp")
#endif()
##message("Build Type:"${CMAKE_BUILD_TYPE} ${CMAKE_CXX_FLAGS})
#
#SET(CMAKE_CXX_FLAGS_DEBUG "$ENV{CXXFLAGS} -O0 -Wall -g -ggdb ")
#SET(CMAKE_CXX_FLAGS_RELEASE "$ENV{CXXFLAGS} -O3 -Wall")
#
## Creates and names a library, sets it as either STATIC
## or SHARED, and provides the relative paths to its source code.
## You can define multiple libraries, and CMake builds them for you.
## Gradle automatically packages shared libraries with your APK.
#
##指定头文件所在，可以多次调用，指定多个路径
#include_directories(src/main/cpp
#                    src/main/cpp/webrtc/common_audio/signal_processing/include
#                    src/main/cpp/webrtc/modules/audio_coding/codecs/isac/main/include )
##指定头文件所在，可以多次调用，指定多个路径
#include_directories(src/main/cpp/speex/include
#                    src/main/cpp/speex/include/speex)
#
##添加自自定义的so库时，有两种方式，一种添加一个目录，一种一个个文件添加
#
##将前面目录下所有的文件都添加进去
#add_library( # Sets the name of the library.
#             jmedia2
#
#             # Sets the library as a shared library.
#             SHARED
#
#             # Provides a relative path to your source file(s).
#             src/main/cpp/media2.cpp )
#
## 添加webrtc子目录, 将会调用子目录中的CMakeLists.txt
#ADD_SUBDIRECTORY(src/main/cpp/webrtc)
## 添加speex子目录, 将会调用子目录中的CMakeLists.txt
#ADD_SUBDIRECTORY(src/main/cpp/speex)
#
## Searches for a specified prebuilt library and stores the path as a
## variable. Because CMake includes system libraries in the search path by
## default, you only need to specify the name of the public NDK library
## you want to add. CMake verifies that the library exists before
## completing its build.
#
#find_library( # Sets the name of the path variable.
#              log-lib
#
#              # Specifies the name of the NDK library that
#              # you want CMake to locate.
#              log )
#
## Specifies libraries CMake should link to your target library. You
## can link multiple libraries, such as libraries you define in this
## build script, prebuilt third-party libraries, or system libraries.
#
#target_link_libraries( # Specifies the target library.
#                       jmedia2
#
#                       # Links the target library to the log library
#                       # included in the NDK.
#                       ${log-lib}
#                       webrtc_aec
#                       webrtc_aecm
#                       webrtc_agc
#                       webrtc_ns
#                       webrtc_utility
#                       webrtc_vad
#                       webrtc_apm
#                       webrtc_intelligibility
#                       webrtc_transient
#                       webrtc_beamformer
#                       webrtc_common_audio_vad
#                       webrtc_spl
#                       webrtc_common_audio
#                       webrtc_common_audio_resampler
#                       webrtc_codecs_isac
#                       webrtc_codecs
#                       webrtc_system_wrapper
#                       webrtc_base
#                       speex
#                       android )
#endif()




#########################################################################
#########################################################################
### WebRTC支持如下CPU架构['arm64-v8a']
#########################################################################
#if (CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
#
#add_definitions( -DWEBRTC_ANDROID
#                 -DWEBRTC_POSIX
#                 -DWEBRTC_NS_FIXED )
#
## 设置编译类型-Debug or Release
#SET(CMAKE_BUILD_TYPE "Release")
#
## 设置编译参数
#if( CMAKE_BUILD_TYPE STREQUAL "Debug" )
#    SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -Wall -Wno-unused-variable -pthread")
#elseif( CMAKE_BUILD_TYPE STREQUAL "Release" )
#    SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -pthread -fopenmp")
#endif()
##message("Build Type:"${CMAKE_BUILD_TYPE} ${CMAKE_CXX_FLAGS})
#
#SET(CMAKE_CXX_FLAGS_DEBUG "$ENV{CXXFLAGS} -O0 -Wall -g -ggdb ")
#SET(CMAKE_CXX_FLAGS_RELEASE "$ENV{CXXFLAGS} -O3 -Wall")
#
## Creates and names a library, sets it as either STATIC
## or SHARED, and provides the relative paths to its source code.
## You can define multiple libraries, and CMake builds them for you.
## Gradle automatically packages shared libraries with your APK.
#
##指定头文件所在，可以多次调用，指定多个路径
#include_directories(src/main/cpp
#        src/main/cpp/webrtc/common_audio/signal_processing/include
#        src/main/cpp/webrtc/modules/audio_coding/codecs/isac/main/include )
#
##添加自自定义的so库时，有两种方式，一种添加一个目录，一种一个个文件添加
#
##将前面目录下所有的文件都添加进去
#add_library( # Sets the name of the library.
#            jmedia3
#
#            # Sets the library as a shared library.
#            SHARED
#
#            # Provides a relative path to your source file(s).
#            src/main/cpp/media3.cpp )
#
## 添加webrtc子目录, 将会调用子目录中的CMakeLists.txt
#ADD_SUBDIRECTORY(src/main/cpp/webrtc)
#
## Searches for a specified prebuilt library and stores the path as a
## variable. Because CMake includes system libraries in the search path by
## default, you only need to specify the name of the public NDK library
## you want to add. CMake verifies that the library exists before
## completing its build.
#
#find_library( # Sets the name of the path variable.
#            log-lib
#
#            # Specifies the name of the NDK library that
#            # you want CMake to locate.
#            log )
#
## Specifies libraries CMake should link to your target library. You
## can link multiple libraries, such as libraries you define in this
## build script, prebuilt third-party libraries, or system libraries.
#
#target_link_libraries( # Specifies the target library.
#            jmedia3
#
#            # Links the target library to the log library
#            # included in the NDK.
#            ${log-lib}
#            webrtc_aec
#            webrtc_aecm
#            webrtc_agc
#            webrtc_ns
#            webrtc_utility
#            webrtc_vad
#            webrtc_apm
#            webrtc_intelligibility
#            webrtc_transient
#            webrtc_beamformer
#            webrtc_common_audio_vad
#            webrtc_spl
#            webrtc_common_audio
#            webrtc_common_audio_resampler
#            webrtc_codecs_isac
#            webrtc_codecs
#            webrtc_system_wrapper
#            webrtc_base
#            android )
#endif()




########################################################################
########################################################################
## WebRTC支持如下CPU架构['arm64-v8a']
########################################################################
add_definitions( -DWEBRTC_ANDROID
                 -DWEBRTC_POSIX
                 -DWEBRTC_NS_FIXED )

# 设置编译类型-Debug or Release
SET(CMAKE_BUILD_TYPE "Release")

# 设置编译参数
if( CMAKE_BUILD_TYPE STREQUAL "Debug" )
    SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -Wall -Wno-unused-variable -pthread")
elseif( CMAKE_BUILD_TYPE STREQUAL "Release" )
    SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -pthread")
endif()
#message("Build Type:"${CMAKE_BUILD_TYPE} ${CMAKE_CXX_FLAGS})

SET(CMAKE_CXX_FLAGS_DEBUG "$ENV{CXXFLAGS} -O0 -Wall -g -ggdb ")
SET(CMAKE_CXX_FLAGS_RELEASE "$ENV{CXXFLAGS} -O3 -Wall")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#指定头文件所在，可以多次调用，指定多个路径
include_directories(src/main/cpp
                    src/main/cpp/ns
                    src/main/cpp/lamemp3 )

#添加自自定义的so库时，有两种方式，一种添加一个目录，一种一个个文件添加

#将前面目录下所有的文件都添加进去
add_library( # Sets the name of the library.
        jmedia4

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/media4.cpp
        src/main/cpp/ns/noise_suppression.c
        ${LAMEMP_SRC_DIR} )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        jmedia4

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        android )
