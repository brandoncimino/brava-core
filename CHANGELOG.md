# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2024-11-16

### Changed

- `Either.of(a, b)` to `Either.ofNullable(a, b)` to make it more easily distinguished from the more commonly used `ofA(a)` and `ofB(b)` _(especially for code-completion tools)_.

### Removed

- Public access to `CollectionHelpers` _(it was already annotated as `@ApiStatus.InternalOnly`, but it didn't need to be `public`)_.

## [1.0.0] - 2024-11-13

🎊 Initial release!

### Highlights

#### "Functional programming" [abstract data types](https://en.wikipedia.org/wiki/Abstract_data_type)

- [Either<A, B>](src/main/java/brava/core/Either.java)
- [Tuple](src/main/java/brava/core/tuples/Tuple.java), `Tuple1`, `Tuple2`, etc.
- [Lazy<T>](src/main/java/brava/core/Lazy.java)

#### Functional interfaces

- [TriFunction](src/main/java/brava/core/functional/TriFunction.java), [QuadFunction](src/main/java/brava/core/functional/QuadFunction.java), etc.
- [Unchecked](src/main/java/brava/core/Unchecked.java)`.Supplier`, `.Function`, and `.Runnable`

#### Other

- [Combinatorial](src/main/java/brava/core/collections/Combinatorial.java)
- [Immutably](src/main/java/brava/core/collections/Immutably.java)