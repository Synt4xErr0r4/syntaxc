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

#ifndef _SIGNAL_H
#define _SIGNAL_H

#define SIG_DFL __SIG_DFL
#define SIG_ERR __SIG_ERR
#define SIG_IGN __SIG_IGN
#define SIGABRT __SIGABRT
#define SIGFPE  __SIGFPE
#define SIGILL  __SIGILL
#define SIGINT  __SIGINT
#define SIGSEGV __SIGSEGV
#define SIGTERM __SIGTERM

typedef __SIG_ATOMIC_TYPE__ sig_atomic_t;
typedef void (*__sighandler_t)(int);

extern __sighandler_t signal(int sig, __sighandler_t func);
extern int raise(int sig);

#endif /* !_SIGNAL_H */