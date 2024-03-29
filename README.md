# SyntaxC ![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/Synt4xErr0r4/syntaxc/maven.yml) ![LoC](https://raw.githubusercontent.com/Synt4xErr0r4/syntaxc/image-data/badge.svg)

**This compiler is currently *not* properly operational (at least the code generation and some optimizations). I'm currently working on a rewrite for the C23 standard. Progress will be published on the `c23` branch at some point.**

An ANSI-C (C89, C90) compiler written in Java 19.  
Currently only working for x86 (32-bit, Linux/Windows).

## Motivation and Goals

This project was developed as part of my [pre-scientific thesis](https://www.ahs-vwa.at/) on how compilers operate, specifically a compiler for the (ANSI-)C language (also known as C89 and C90). The main goal of this compiler is to produce assembly code for the x86 platform on Linux (32 and 64 bits). However, this could easily be expanded (e.g., to support ARM or Windows) due to the already existing [modularized target architecture system](https://github.com/Synt4xErr0r4/syntaxc/blob/main/src/main/java/at/syntaxerror/syntaxc/generator/arch/ArchitectureRegistry.java). All features specified by the standard ([see below](#98991990-standard)) and also some newer/non-standard features ([see below](#extensions)) are supported. The command line syntax is similar or even identical to GCC's system (e.g., `-E` only generates the output of the preprocessor), and also the generated assembly code is aimed to match/be similar to GCC's output. However, this compiler only supports a handfull of the various optimizations, and the output is therefore usually only comparable to GCC when using the command line option `-O0` (generally disables all of GCC's aggressive optimizations). This compiler does *not* support assembling or linking by itself, only the assembly code is generated by this compiler and the result is then passed to GCC (unless the `-E` or `-S` option is present, in which case the compilation is stopped after the preprocessor or code generator, respectively). The `-c` option, if supplied, will be forwarded to GCC and inhibit any linking of the output binary.

The version described in the thesis is accessible [here](https://github.com/Synt4xErr0r4/syntaxc/tree/vwa).

## Dependencies

All dependencies are also listed in the [pom.xml file](https://github.com/Synt4xErr0r4/syntaxc/blob/main/pom.xml).

- [Lombok](https://projectlombok.org/) (used to reduce boiler plate code)
- [JNA](https://github.com/java-native-access/jna) (used to determine the default target architecture)
- [graphviz-java](https://github.com/nidi3/graphviz-java) (used to generate syntax trees and control flow graphs)
- [big-math](https://github.com/eobermuhlner/big-math) (used for calculating logarithms on `java.math.BigDecimal`s)

## Supported optimizations

### `-Oconst-folding`

Automatically inlines global (arithmetic) variables declared as `const`.

before:

```c
const int j = 3;

/* ... */

/* ... */ j * 4 /* ... */
```

after:

```c
const int j = 3;

/* ... */

/* ... */ 12 /* ... */
```

### `-Ogoto`

Automatically removes `goto` statements followed by the label they jump to.

before:

```c
    /* ... */
    goto label;
label:
    /* ... */
```

after:

```c
    /* ... */
label:
    /* ... */
```

### `-Ojump-to-jump`

Automatically chooses the shortest path instead of jumping several times in a row.

before:

```c
    /* ... */
    goto label1;
    /* ... */
label1:
    goto label2;
    /* ... */
label2:
    goto label3;
    /* ... */
label3:
    /* ... */
```

after:

```c
    /* ... */
    goto label3;
    /* ... */
label1:
    goto label3;
    /* ... */
label2:
    goto label3;
    /* ... */
label3:
    /* ... */
```

### x86 optimizations

There are also various x86-specific optimizations, including (but not limited to) the following examples:

```asm
mov eax, 0
cmp ebx, 0
```

These two instructions are replaced by instructions smaller in size (which do not have to encode the immediate value `0`, but perform essentially the same operation):

```asm
xor eax, eax
test ebx, ebx
```

Also (just like GCC with options `-O2` or `-O3` would), a function of the following (or a comparable) form is heavily optimized:

```c
void run(void (*runnable)(void)) {
    runnable();
}
```

Typically, this would result in the following or a similar assembly code:

```asm
push ebp
mov ebp, esp
sub esp, 8
mov eax, dword ptr [ebp+8]
call eax
add esp, 8
pop ebp
ret
```

However, this function can be expressed as a single assembly instruction:

```asm
jmp [dword ptr [esp+4]]
```

## Usage

Compile a file using:

`java -jar syntaxc.jar input_file.c`

The output file will be named `input_file.o`

For full documentation of all command line parameters,
use `java -jar syntaxc.jar --help`.  
You can also use `--doc option_here` to get extended documentation about a specific option (e.g. `--doc D` prints the documentation for the `-D` option).

*Note*: Currently, due to the slow `long double` implementation, compiling even very small files can take a few seconds. Therefore it is advised to specify `-fno-long-double` if you do not need x87 80-bit precision `long double`s.

## Extensions

The following extensions are supported:

- `#elifdef`, `#elifndef` (C23 feature)  
  else-versions of `#ifdef` and `#ifndef`

- `__FUNCTION__`, `__func__` (GNU extension, C99 feature)  
  Returns the name of the current function.
  `__FUNCTION__` is a macro that expands to `__func__`,
  a local variable implicitly declared at the start of
  each function (as `static const char __func__[] = "function-name";`).  
  Note that the original GNU extension for `__FUNCTION__` did not declare
  it as a macro but as a synonym for `__func__`.

- *binary literals* (C23 feature)  
  When a number is prefixed with `0b`, it is interpreted as a binary literal (similar to hexadecimal literals [`0x` prefix] and octal literals [`0` prefix])

All extensions can be disabled by using the appropriate command line option:

- `-fno-elifdef`: disables `#elifdef` and `#elifndef`
- `-fno-func`: disables `__func__`
- `-fno-binary-literals`: disables binary literals

## 9899:1990 Standard

Even though the C89/C90 standard has officially been withdrawn, it is still availble from some sources:

- [https://www.yodaiken.com/wp-content/uploads/2021/05/ansi-iso-9899-1990-1.pdf](https://www.yodaiken.com/wp-content/uploads/2021/05/ansi-iso-9899-1990-1.pdf)
- [https://web.archive.org/web/20190210174230/http://read.pudn.com/downloads133/doc/565041/ANSI_ISO%2B9899-1990%2B%5B1%5D.pdf](https://web.archive.org/web/20190210174230/http://read.pudn.com/downloads133/doc/565041/ANSI_ISO%2B9899-1990%2B%5B1%5D.pdf) (archived)
- [https://web.archive.org/web/20200909074736/https://www.pdf-archive.com/2014/10/02/ansi-iso-9899-1990-1/ansi-iso-9899-1990-1.pdf](https://web.archive.org/web/20200909074736/https://www.pdf-archive.com/2014/10/02/ansi-iso-9899-1990-1/ansi-iso-9899-1990-1.pdf) (archived)
