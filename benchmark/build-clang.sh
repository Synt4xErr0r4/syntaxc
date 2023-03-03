#!/bin/sh

mkdir -p asm
mkdir -p bin

for opt in 0 1 2 3; do
    echo "Building Clang (-O$opt)"
    clang -ggdb3 -fpic -fpie -m32 ./*.c -o ./benchmark-clang-$opt -O$opt -I../include
    chmod a+x ./benchmark-clang-$opt
done