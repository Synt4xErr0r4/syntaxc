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

#ifndef _MATH_H
#define _MATH_H

#define HUGE_VAL __HUGE_VAL__

extern double acos(double x);
extern double asin(double x);
extern double atan(double x);
extern double atan2(double y, double x);
extern double cos(double x);
extern double sin(double x);
extern double tan(double x);
extern double cosh(double x);
extern double sinh(double x);
extern double tanh(double x);
extern double exp(double x);
extern double frexp(double value, int *exp);
extern double ldexp(double x, int exp);
extern double log(double x);
extern double log10(double x);
extern double modf(double value, double *iptr);
extern double pow(double x, double y);
extern double sqrt(double x);
extern double ceil(double x);
extern double fabs(double x);
extern double floor(double x);
extern double fmod(double x, double y);

#endif /* !_MATH_H */