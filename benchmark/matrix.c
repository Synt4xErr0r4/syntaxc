/* (c) 2023, Thomas Kasper */

#define NEED_INT_TYPES
#include "syntaxbench.h"

static float64_t A[MATRIX_A * MATRIX_B]; /* A = NxM */
static float64_t B[MATRIX_B * MATRIX_A]; /* B = MxN */
static float64_t C[MATRIX_A * MATRIX_A]; /* AB = NxN */

void benchmark_matrix(FILE *input) {
    float64_t result;
    uint32_t i, j, k;

    fread(A, sizeof A / sizeof(float64_t), sizeof(float64_t), input);
    fread(B, sizeof B / sizeof(float64_t), sizeof(float64_t), input);

    for(i = 0; i < MATRIX_A; ++i)
        for(j = 0; j < MATRIX_A; ++j) {
            result = 0;

            for(k = 0; k < MATRIX_B; ++k)
                result += A[k + i * MATRIX_B] * B[j + k * MATRIX_A];

            C[j + i * MATRIX_A] = result;
        }
    
    consume_result(&C);
}