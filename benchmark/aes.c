/* (c) 2023, Thomas Kasper */

#define BACK_TO_TABLES

#include "aes256.h"
#undef uint8_t

#ifndef __SYNTAXC__
#include <stdint.h>
#else
#define NEED_INT_TYPES
#endif

#include "syntaxbench.h"

#include <stdlib.h>
#include <string.h>

aes256_blk_t plaintext[AES_BYTES / sizeof(aes256_blk_t)];
aes256_blk_t ciphertext[AES_BYTES / sizeof(aes256_blk_t)];

aes256_key_t aes_key = { { 
    0x60, 0x3D, 0xEB, 0x10, 0x15, 0xCA, 0x71, 0xBE,
    0x2B, 0x73, 0xAE, 0xF0, 0x85, 0x7D, 0x77, 0x81,
    0x1F, 0x35, 0x2C, 0x07, 0x3B, 0x61, 0x08, 0xD7,
    0x2D, 0x98, 0x10, 0xA3, 0x09, 0x14, 0xDF, 0xF4
} };

aes256_context_t aes_ctx = { { { 0 } }, { { 0 } }, { { 0 } } };

void benchmark_aes(FILE *input) {
    uint32_t i, j;
    uint8_t ret;
    aes256_blk_t *src, *blk;

    ret = aes256_init(&aes_ctx, &aes_key);

    if(ret != AES_SUCCESS) {
        fprintf(stderr, "AES init failed\n");
        exit(EXIT_FAILURE);
    }

    fread(plaintext, AES_BYTES, 1, input);

    for(i = 0; i < (AES_BYTES / sizeof(aes256_blk_t)); ++i) {
        src = &plaintext[i];
        blk = &ciphertext[i];

        memcpy(blk, src, sizeof(aes256_blk_t));
        
        ret = aes256_encrypt_ecb(&aes_ctx, blk);

        if(ret != AES_SUCCESS) {
            fprintf(stderr, "AES encryption failed\n");
            exit(EXIT_FAILURE);
        }

        ret = aes256_decrypt_ecb(&aes_ctx, blk);

        if(ret != AES_SUCCESS) {
            fprintf(stderr, "AES decryption failed\n");
            exit(EXIT_FAILURE);
        }

        if(memcmp(blk, src, sizeof(aes256_blk_t))) {
            fprintf(stderr, "AES failed at block %d\n", i);
            exit(EXIT_FAILURE);
        }
    }
}