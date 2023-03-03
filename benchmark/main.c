/* (c) 2023, Thomas Kasper */

#ifndef __SYNTAXC__
#include <stdint.h>
#else
#define NEED_INT_TYPES
#endif

#define NEED_BENCHMARKS

#include "syntaxbench.h"

#include <time.h>
#include <stdio.h>
#include <float.h>
#include <stdlib.h>

typedef struct {
    clock_t           time_total;
    clock_t           time_min;
    clock_t           time_max;
} benchmark_result_t;

benchmark_result_t results[NUM_BENCHMARKS];

int main() {
    uint32_t i, j;
    FILE *input;

    input = fopen("data-random", "r");

    srand(0);

    if(!input) {
        fprintf(stderr, "Failed to open random data file. %p\n", input);
        return 1;
    }

    printf("Running %u benchmarks...\n", NUM_BENCHMARKS);

    for(i = 0; i < NUM_BENCHMARKS; ++i) {
        clock_t time_start, time_end, time_diff;
        benchmark_data_t *benchmark = &benchmarks[i];
        benchmark_result_t *result = &results[i];

        result->time_total = 0;
        result->time_min = 2147483647;
        result->time_max = -2147483648;

        printf("\r                                    ");
        printf("\rRunning benchmark %s...\n", benchmark->name);

        for(j = 0; j < benchmark->iterations; ++j) {
            fseek(input, rand() % 134217728, SEEK_SET);

            printf("\r                                    ");
            printf("\r%d/%d...", j, benchmark->iterations);
            fflush(stdout);

            time_start = clock();
            benchmark->benchmark(input);
            time_end = clock();

            time_diff = time_end - time_start;

            if(CLOCKS_PER_SEC != 1000000)
                time_diff *= (1000000 / CLOCKS_PER_SEC);

            result->time_total += time_diff;

            if(time_diff < result->time_min)
                result->time_min = time_diff;

            if(time_diff > result->time_max)
                result->time_max = time_diff;
        }

    }

    fclose(input);

    printf("\r                                    ");
    printf("\r\nBenchmark results:\n");

    for(i = 0; i < NUM_BENCHMARKS; ++i) {
        benchmark_data_t *benchmark = &benchmarks[i];
        benchmark_result_t *result = &results[i];

        printf(
            "\n%s (%u iterations):\n",
            benchmark->name,
            benchmark->iterations
        );

        printf("Total time:   %ld µs\n", result->time_total);
        printf("Min. time:    %ld µs\n", result->time_min);
        printf("Max. time:    %ld µs\n", result->time_max);
        printf("Avg. time:    %.2lf µs\n",
            result->time_total / (float64_t) benchmark->iterations
        );
    }

    return 0;
}

void consume_result(void *ptr) { }