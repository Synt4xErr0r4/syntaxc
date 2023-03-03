/* (c) 2023, Thomas Kasper */

#ifndef __SYNTAXC__
#include <stdint.h>
#else
#define NEED_INT_TYPES
#endif

#include "syntaxbench.h"

#include <stdlib.h>

float64_t qsort_list[QSORT_ITEMS];

static void swap(int a, int b) {
    float64_t c = qsort_list[a];
    qsort_list[a] = qsort_list[b];
    qsort_list[b] = c;
}

static void qsort_impl(int left, int right) {
    int i, last;
    
    if(left >= right)
        return;

    swap(left, (left + right) / 2);

    last = left;

    for(i = left + 1; i <= right; ++i)
        if(qsort_list[left] > qsort_list[i])
            swap(i, ++last);
    
    swap(left, last);
    qsort_impl(left, last - 1);
    qsort_impl(last + 1, right);
}

void benchmark_qsort(FILE *input) {
    uint32_t i;
    float64_t prev;
    
    fread(qsort_list, QSORT_ITEMS, sizeof(float64_t), input);

    qsort_impl(0, QSORT_ITEMS - 1);

    for(i = 0; i < QSORT_ITEMS; ++i) {
        if(i != 0 && prev > qsort_list[i]) {
            fprintf(stderr, "Quicksort failed at byte %d [%lf/%lf]\n", i, prev, qsort_list[i]);
            exit(EXIT_FAILURE);
        }

        prev = qsort_list[i];
    }
}