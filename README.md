# SyntaxC

An ANSI-C compiler written in Java 18

## Usage

Compile a file using:

`java -jar syntaxc.jar input_file.c`

The output file will be named `input_file.o`

For full documentation of all command line parameters,
use `java -jar syntaxc.jar --help`.  
You can also use `--doc command_here` to get extended documentation about a specific command (e.g. `--doc D` prints the documentation for `-D` to stdout).

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

All extensions can be disabled by using the appropriate command line argument:

- `-fno-elifdef`: disables `#elifdef` and `#elifndef`
- `-fno-func`: disabled `__func__`
- `-fno-binary-literals`: disables binary literals

## Sources (incomplete)

- [ANSI/ISO 9899-1990; ANSI X3.159-1989](https://www.iso.org/standard/17782.html) (C Standard) - officially withdrawn, but still available here:
  - [https://www.yodaiken.com/wp-content/uploads/2021/05/ansi-iso-9899-1990-1.pdf](https://www.yodaiken.com/wp-content/uploads/2021/05/ansi-iso-9899-1990-1.pdf)
  - [https://web.archive.org/web/20190210174230/http://read.pudn.com/downloads133/doc/565041/ANSI_ISO%2B9899-1990%2B%5B1%5D.pdf](https://web.archive.org/web/20190210174230/http://read.pudn.com/downloads133/doc/565041/ANSI_ISO%2B9899-1990%2B%5B1%5D.pdf)
  - [https://web.archive.org/web/20200909074736/https://www.pdf-archive.com/2014/10/02/ansi-iso-9899-1990-1/ansi-iso-9899-1990-1.pdf](https://web.archive.org/web/20200909074736/https://www.pdf-archive.com/2014/10/02/ansi-iso-9899-1990-1/ansi-iso-9899-1990-1.pdf)
- [termd/Wcwidth](https://github.com/termd/termd/blob/master/src/main/java/io/termd/core/util/Wcwidth.java) (at.syntaxerror.syntaxc.logger.WCWidth)
- [Calling Conventions](https://agner.org/optimize/calling_conventions.pdf)
- [Predefined Macros](https://sourceforge.net/p/predef/wiki/)
- [ANSI Escape Sequences](https://en.wikipedia.org/wiki/ANSI_escape_code#3-bit_and_4-bit)
