/* MIT License
 * 
 * Copyright (c) 2022 Thomas Kasper
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/* this file is part of SyntaxC */

#ifndef _STDLIB_H
#define _STDLIB_H

#ifndef __HAS_NULL
#define __HAS_NULL
#define NULL ((void *) 0)
#endif

#ifdef _WIN32
int ___mb_cur_max_func(void);
#endif /* !_WIN32 */

#define EXIT_FAILURE __EXIT_FAILURE
#define EXIT_SUCCESS __EXIT_SUCCESS
#define MB_CUR_MAX __MB_CUR_MAX
#define RAND_MAX __RAND_MAX

typedef __DIV_TYPE__ div_t;
typedef __LDIV_TYPE__ ldiv_t;

#ifndef __HAS_SIZE_TYPE
#define __HAS_SIZE_TYPE
typedef __SIZE_TYPE__ size_t;
#endif

#ifndef __HAS_WCHAR_TYPE
#define __HAS_WCHAR_TYPE
typedef __WCHAR_TYPE__ wchar_t;
#endif

extern double atof(const char *nptr);
extern int atoi(const char *nptr);
extern long int atol(const char *nptr);
extern double strtod(const char *nptr, char **endptr);
extern long int strtol(const char *nptr, char **endptr, int base);
extern unsigned long int strtoul(const char *nptr, char **endptr, int base);
extern int rand(void);
extern void srand(unsigned int seed);
extern void *calloc(size_t nmemb, size_t size);
extern void free(void *ptr);
extern void *malloc(size_t size);
extern void *realloc(void *ptr, size_t size);
extern void abort(void);
extern int atexit(void (*func)(void));
extern void exit(int status);
extern char *getenv(const char *name);
extern int system(const char *string);
extern void *bsearch(const void *key, const void *base, size_t nmemb, size_t size, int (*compar)(const void *, const void *));
extern void qsort(void *base, size_t nmemb, size_t size, int (*compar)(const void *, const void *));
extern int abs(int j);
extern div_t div(int numer, int denom);
extern long int labs(long int j);
extern ldiv_t ldiv(long int numer, long int denom);
extern int mblen(const char *s, size_t n);
extern int mbtowc(wchar_t *pwc, const char *s, size_t n);
extern int wctomb(char *s, wchar_t wchar);
extern size_t mbstowcs(wchar_t *pwcs, const char *s, size_t n);
extern size_t wcstombs(char *s, const wchar_t *pwcs, size_t n);

#endif /* !_STDLIB_H */