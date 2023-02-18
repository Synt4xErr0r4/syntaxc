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

#ifndef _STDIO_H
#define _STDIO_H

#include <stdarg.h>

typedef __FILE_TYPE__ FILE;
typedef __FPOS_TYPE__ fpos_t;

#ifndef __HAS_SIZE_TYPE
#define __HAS_SIZE_TYPE
typedef __SIZE_TYPE__ size_t;
#endif

#ifndef __HAS_VA_LIST_TYPE
#define __HAS_VA_LIST_TYPE
typedef __builtin_va_list va_list;
#endif

#define _IOFBF ___IOFBF
#define _IOLBF ___IOLBF
#define _IONBF ___IONBF

#define BUFSIZ __BUFSIZ
#define EOF __EOF
#define FILENAME_MAX __FILENAME_MAX
#define FOPEN_MAX __FOPEN_MAX
#define L_tmpnam __L_tmpnam

#define SEEK_CUR __SEEK_CUR
#define SEEK_END __SEEK_END
#define SEEK_SET __SEEK_SET

#define TMP_MAX __TMP_MAX

#ifdef _WIN32
extern FILE* __acrt_iob_func(unsigned _Ix);

#define stderr __stderr
#define stdin __stdin
#define stdout __stdout

#else

extern FILE *stdin;
extern FILE *stdout;
extern FILE *stderr;

#endif /* !_WIN32 */

extern int remove(const char *filename);
extern int rename(const char *old, const char *new);
extern FILE *tmpfile(void);
extern char *tmpnam(char *s);
extern int fclose(FILE *stream);
extern int fflush(FILE *stream);
extern FILE *fopen(const char *filename, const char *mode);
extern FILE *freopen(const char *filename, const char *mode, FILE *stream);
extern void setbuf(FILE *stream, char *buf);
extern int setvbuf(FILE *stream, char *buf, int mode, size_t size);
extern int fprintf(FILE *stream, const char *format, ...);
extern int fscanf(FILE *stream, const char *format, ...);
extern int printf(const char *format, ...);
extern int scanf(const char *format, ...);
extern int sprintf(char *s, const char *format, ...);
extern int sscanf(const char *s, const char *format, ...);
extern int vfprintf(FILE *stream, const char *format, va_list arg);
extern int vprintf(const char *format, va_list arg);
extern int vsprintf(char *s, const char *format, va_list arg);
extern int fgetc(FILE *stream);
extern char *fgets(char *s, int n, FILE *stream);
extern int fputc(int c, FILE *stream);
extern int fputs(const char *s, FILE *stream);
extern int getc(FILE *stream);
extern int getchar(void);
extern char *gets(char *s);
extern int putc(int c, FILE *stream);
extern int putchar(int c);
extern int puts(const char *s);
extern int ungetc(int c, FILE *stream);
extern size_t fread(void *ptr, size_t size, size_t nmemb, FILE *stream);
extern size_t fwrite(const void *ptr, size_t size, size_t nmemb, FILE *stream);
extern int fgetpos(FILE *stream, fpos_t *pos);
extern int fseek(FILE *stream, long int offset, int whence);
extern int fsetpos(FILE *stream, const fpos_t *pos);
extern long int ftell(FILE *stream);
extern void rewind(FILE *stream);
extern void clearerr(FILE *stream);
extern int feof(FILE *stream);
extern int ferror(FILE *stream);
extern void perror(const char *s);

#endif /* !_STDIO_H */