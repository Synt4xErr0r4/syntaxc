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

#ifndef _TIME_H
#define _TIME_H

#ifndef __HAS_NULL
#define __HAS_NULL
#define NULL ((void) 0)
#endif

#define CLOCKS_PER_SEC __CLOCKS_PER_SEC

typedef __CLOCK_TYPE__ clock_t;
typedef __TIME_TYPE__ time_t;

struct tm {
    int tm_sec;
    int tm_min;
    int tm_hour;
    int tm_mday;
    int tm_mon;
    int tm_year;
    int tm_wday;
    int tm_yday;
    int tm_isdst;
};

extern clock_t clock(void);
extern double difftime(time_t time1, time_t time0);
extern time_t mktime(struct tm *timeptr);
extern time_t time(time_t *timer);
extern char *asctime(const struct tm *timeptr);
extern char *ctime(const time_t *timer);
extern struct tm *gmtime(const time_t *timer);
extern struct tm *localtime(const time_t *timer);
extern size_t strftime(char *s, size_t maxsize, const char *format, const struct tm *timeptr);

#endif /* !_TIME_H */