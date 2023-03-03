#!/bin/bash
./clean.sh
./build-gcc.sh
./build-clang.sh
./build-msvc.sh
./build-syntaxc.sh
./run.sh