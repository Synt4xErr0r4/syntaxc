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

#ifndef _ASSERT_H
#define _ASSERT_H

# ifdef NDEBUG

#  define assert(ignore) ((void) 0)

# else

#  ifdef __linux__

/* from glibc */
#  define assert(expr) ((expr) ? ((void) 0) : __assert_fail(#expr, __FILE__, __LINE__, __FUNCTION__))

extern void __assert_fail(const char *expr, const char *file, unsigned int line, const char *function);

#  elifdef _WIN32

/* from VC++ */
#  define assert(expr) (void)(\
        (!!(expression)) ||\
        (_wassert(_CRT_WIDE(#expression), _CRT_WIDE(__FILE__), (unsigned)(__LINE__)), 0)\
    )

extern __WCHAR_TYPE__ const*_CRT_WIDE(char const*);
extern void _wassert(__WCHAR_TYPE__ const* _Message, __WCHAR_TYPE__ const* _File, unsigned _Line);

#  endif /* __linux__ || _WIN32 */

# endif /* !NDEBUG */

#endif /* !_ASSERT_H */