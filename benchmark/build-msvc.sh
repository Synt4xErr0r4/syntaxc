#!/bin/sh

if [ ! -f "~/msvc/bin/x86/cl" ]; then
    echo "Skipping MSVC due to missing compiler at '`realpath ~`/msvc/bin/x86/cl'"
    exit 0
fi

mkdir -p asm
mkdir -p bin

for opt in d 1 2; do
    echo "Building MSVC (/O$opt)..."
    ~/msvc/bin/x86/cl *.c /Fe./benchmark-msvc-$opt.exe /O$opt
    chmod a+x ./benchmark-msvc-$opt.exe
done