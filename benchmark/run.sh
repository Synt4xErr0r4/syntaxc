#!/bin/bash
OUTPUT_FILE=results.csv

run_benchmark() {
    if [ ! -f "./benchmark-$1" ]; then
        echo "Skipping $2 (missing file)!"
    else
        echo "Benchmarking $2...";

        start=`date +%s.%N`

        "./benchmark-$1" 2>&1 | tee "output-$1"

        EXIT_CODE=$PIPESTATUS

        end=`date +%s.%N`
        runtime=$( echo "$end - $start" | bc -l )

        echo >> "output-$1"

        if [ $EXIT_CODE -ne 0 ]; then
            echo "Benchmark failed after $runtime seconds with exit code $EXIT_CODE" \
                | tee -a "output-$1"
        else
            echo "Benchmark $2 done! Took $runtime seconds" | tee -a "output-$1"

            ./csv-output.py "$2" "output-$1" "$OUTPUT_FILE"
        fi
    fi
}

get_compiler_version() {
    $@ | head -n 1 | grep -Eo "([0-9]+\.)+[0-9]+"
}

output_append() {
    echo "$1: $2"
    printf "\"%q\";\"%q\";;\n" "$1:" "$2" >> "$OUTPUT_FILE"
}

output_write() {
    printf "\n%s\n" "$@" >> "$OUTPUT_FILE"
}

echo "Writing random data..."

dd if=/dev/random of=data-random bs=1M count=128

if [ -e "$OUTPUT_FILE" ]; then
    rm $OUTPUT_FILE
fi

output_append System "`uname -r`"
output_append Processor "`lscpu | sed -nr '/Modellname/ s/.*:\s*(.*) @ .*/\1/p'`"

output_append GCC "`get_compiler_version gcc --version`"
output_append Clang "`get_compiler_version clang --version`"
output_append SyntaxC "`get_compiler_version java --enable-preview -jar syntaxc.jar -fno-long-double -m32 --version`"

if [ ! -f "~/msvc/bin/x86/cl" ]; then
    output_append MSVC unknown
else
    output_append MSVC "`get_compiler_version ~/msvc/bin/x86/cl`"
fi

output_write "Matrix;Minimum;Durchschnitt;Maximum"
output_write "CRC;Minimum;Durchschnitt;Maximum"
output_write "AES;Minimum;Durchschnitt;Maximum"
output_write "Quicksort;Minimum;Durchschnitt;Maximum"
output_write

total_start=`date +%s.%N`

run_benchmark gcc-3         "GCC (-O3)"
run_benchmark gcc-2         "GCC (-O2)"
run_benchmark gcc-1         "GCC (-O1)"
run_benchmark gcc-0         "GCC (-O0)"
run_benchmark clang-3       "Clang (-O3)"
run_benchmark clang-2       "Clang (-O2)"
run_benchmark clang-1       "Clang (-O1)"
run_benchmark clang-0       "Clang (-O0)"
run_benchmark msvc-2.exe    "MSVC (/O2)"
run_benchmark msvc-1.exe    "MSVC (/O1)"
run_benchmark msvc-d.exe    "MSVC (/Od)"
run_benchmark syntaxc-gcc   "SyntaxC (GCC)"
run_benchmark syntaxc-clang "SyntaxC (Clang)"

total_end=`date +%s.%N`
total_runtime=$( echo "$total_end - $total_start" | bc -l )

echo "Done! Took $total_runtime seconds"