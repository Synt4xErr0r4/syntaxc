#!/bin/sh

JNI_PATH=/usr/lib/jvm/default/include/
OUT_PATH=../src/main/java/native/

gcc -m64 -I$JNI_PATH -Iunix -fPIC -rdynamic -shared -O3 -o $OUT_PATH/libx86cpuid64.so x86cpuid.c
gcc -m32 -I$JNI_PATH -Iunix -fPIC -rdynamic -shared -O3 -o $OUT_PATH/libx86cpuid32.so x86cpuid.c

x86_64-w64-mingw32-gcc -m64 -I$JNI_PATH -Iwindows -fPIC -mdll -O3 -o $OUT_PATH/x86cpuid64.dll x86cpuid.c
i686-w64-mingw32-gcc -m32 -I$JNI_PATH -Iwindows -fPIC -mdll -O3 -o $OUT_PATH/x86cpuid32.dll x86cpuid.c

# TODO: mac support

