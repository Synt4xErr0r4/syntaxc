#!/bin/sh

mkdir -p asm
mkdir -p bin

echo "Building SyntaxC..."

for file in ./*.c ; do
    echo "Compiling $file..."
    java -Xmx16G --enable-preview -jar syntaxc.jar \
        -S -m32 -fno-long-double -fvery-verbose \
        -Wno-all \
        "$file" \
        -o "asm/$file-syntaxc.s" \
        -Ono-goto \
        -Ono-jump-to-jump
done

for as_ld in gcc; do
    echo "Using $as_ld as assembler/linker..."

    for file in ./*.c ; do
        echo "Assembling $file..."
        $as_ld -c -m32 "asm/$file-syntaxc.s" -o "bin/$file-syntaxc-$as_ld.o"
    done

    echo "Linking..."

    $as_ld -m32 bin/*-syntaxc-$as_ld.o -o ./benchmark-syntaxc-$as_ld
    chmod a+x ./benchmark-syntaxc-$as_ld
done