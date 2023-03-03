/* (c) 2023, Thomas Kasper */

#ifndef _SYNTAXBENCH_H
#define _SYNTAXBENCH_H

#include <stdio.h>

#ifdef NEED_INT_TYPES
typedef signed long    int32_t;
typedef unsigned long  uint32_t;
typedef signed short   int16_t;
typedef unsigned short uint16_t;
typedef signed char    int8_t;
typedef unsigned char  uint8_t;
#endif

typedef float          float32_t;
typedef double         float64_t;

typedef struct {
    uint32_t   n, m;
    float64_t *data;
} matrix_t;

void consume_result(void *ptr);

void benchmark_matrix(FILE *input);
void benchmark_crc32 (FILE *input);
void benchmark_aes   (FILE *input);
void benchmark_qsort (FILE *input);

typedef void (*benchmark_t)(FILE *input);

typedef struct {
    benchmark_t  benchmark;
    const char  *name;
    uint32_t     iterations;
} benchmark_data_t;

#define ITERATIONS_MATRIX 100
#define ITERATIONS_CRC32  100
#define ITERATIONS_AES    100
#define ITERATIONS_QSORT  100

/* 256x256 matrices */
#define MATRIX_A 512
#define MATRIX_B 512

/* 64MiB CRC32 */
#define CRC32_BYTES 67108864

/* 8MiB AES data */
#define AES_BYTES 8388608

/* 1M Quicksort items */
#define QSORT_ITEMS 750000

#ifdef NEED_BENCHMARKS

#define XSTR(x) # x
#define STR(x) XSTR(x)

static benchmark_data_t benchmarks[] = {
    {
        benchmark_matrix,
        "Matrix multiplication (" STR(MATRIX_A) "x" STR(MATRIX_B) ")",
        ITERATIONS_MATRIX
    },
    {
        benchmark_crc32,
        "CRC32 (" STR(CRC32_BYTES) " bytes)",
        ITERATIONS_CRC32
    },
    {
        benchmark_aes,
        "AES (" STR(AES_BYTES) " bytes)",
        ITERATIONS_AES
    },
    {
        benchmark_qsort,
        "Quicksort (" STR(QSORT_ITEMS) " items)",
        ITERATIONS_QSORT
    }
};

#define NUM_BENCHMARKS (sizeof benchmarks / sizeof(benchmark_data_t))

#undef STR
#undef XSTR

#endif

#endif /* !_SYNTAXBENCH_H */