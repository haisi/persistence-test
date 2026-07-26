# Contributing

Thanks for considering a contribution to `persistence-test`.

## Reporting bugs / requesting features

Open an [issue](https://github.com/haisi/persistence-test/issues) with a minimal repro (the entity/repository
shape, the query executed, and the assertion that misbehaved) for bugs, or a short description of the use case
for feature requests.

## Making changes

1. Fork the repo and create a branch off `main`.
2. Build and test:
   ```shell
   ./mvnw verify
   ```
   `plan-assertions`' integration tests need Docker (Testcontainers PostgreSQL).
3. Keep the change focused — each module has a single responsibility (see the README's module breakdown). If
   you're proposing something larger (a new public API, a new dependency, a new module), open an issue first
   to discuss the approach before writing code.
4. Formatting, Checkstyle, Error Prone/NullAway, and the JaCoCo coverage gate (85% line / 75% branch) are all
   enforced by `verify` — run `./mvnw spotless:apply` to auto-format code and every `pom.xml` before
   committing.
5. Open a pull request against `main` describing the change and why it's needed. CI (`.github/workflows/ci.yml`)
   runs the build and test suite on every PR.

## Releasing

Releases are cut by a maintainer by pushing a `vX.Y.Z` tag — see the [Releasing](README.md#releasing) section
of the README. Contributors don't need to worry about versioning or publishing.

## Code of Conduct

This project follows the [Code of Conduct](CODE_OF_CONDUCT.md).
