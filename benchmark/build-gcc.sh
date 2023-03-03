#!/bin/sh

mkdir -p asm
mkdir -p bin

for opt in 0 1 2 3; do
    echo "Building GCC (-O$opt)..."
    gcc -fpic -fpie -m32 ./*.c -o ./benchmark-gcc-$opt -O$opt -I../include
    chmod a+x ./benchmark-gcc-$opt
done