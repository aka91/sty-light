#add_library(coordinates.cpp)

cmake_minimum_required(VERSION 2.8)
project( coordinates )

set (CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake" )

# Finds and loads settings from an external project

find_package ( libpixyusb REQUIRED )
find_package ( libusb-1.0 REQUIRED )
find_package ( Boost 1.49 COMPONENTS thread system chrono REQUIRED)
find_package( OpenCV REQUIRED )

file(STRINGS "cmake/VERSION" LIBPIXY_VERSION)
add_definitions(-D__LIBPIXY_VERSION__="${LIBPIXY_VERSION}")

include_directories (src
                     include
                     ../../../include
                     ${Boost_INCLUDE_DIR}
                     ${LIBUSB_1_INCLUDE_DIRS})

add_executable( coordinates coordinates.cpp )

# libpixyusb should always come before libboost and libusb #
target_link_libraries (coordinates /usr/local/lib/libpixyusb.a)
target_link_libraries (coordinates ${Boost_LIBRARIES})
target_link_libraries (coordinates ${LIBUSB_1_LIBRARY})
target_link_libraries( coordinates ${OpenCV_LIBS} )