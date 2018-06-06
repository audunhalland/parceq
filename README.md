# parceq
A parser for logical search query language for use with search engines, written in Java 8 using https://github.com/vavr-io/vavr.

# examples
`"foo bar" -> or("foo", "bar")`

`"foo bar -baz" -> and(or("foo", "bar"), not("baz"))`
