#!/bin/bash

TARGET_BUILD_FOLDER=../build

mkdir $TARGET_BUILD_FOLDER
mkdir $TARGET_BUILD_FOLDER/pen

cd $TARGET_BUILD_FOLDER/pen
cmake ../../source/pen
make
