<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Changelist Automated Changelog

## [Unreleased]

## [0.1.1] - 2024-03-04

### Fixed

- Plugin previously required the IDE to be restarted on update. Shouldn't going forward

## [0.1.0] - 2023-12-15

### Added

- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- `changelist` URL Scheme handler
- `/api/changelist/` REST handler
- X-Callback-URL support. Invokes `x-success` or `x-error` if provided

## [0.1.0-beta.3] - 2023-12-07

### Added

- `POST /changelist/{project}/{name}` to correctly handle rename semantics
- 400 responses where appropriate to OpenAPI doc
- X-Callback-URL support. Invokes `x-success` or `x-error` if provided

### Changed

- `PUT /changelist/{project}/{name}` no longer supports renaming a changelist

### Fixed

- Return 400 when new or renamed changelist will have same name as another existing one

## [0.1.0-beta.2] - 2023-12-02

### Added

- Project Icon
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- `changelist` URL Scheme handler
- `/api/changelist/` REST handler

### Changed

- README improvements

### Fixed

- Linter warning

## [0.1.0-beta.1] - 2023-12-01

### Added

- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- `changelist` URL Scheme handler
- `/api/changelist/` REST handler

[Unreleased]: https://github.com/sblundy/changelist-protocol/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/sblundy/changelist-protocol/compare/v0.1.0...v0.1.1
[0.1.1-beta.1]: https://github.com/sblundy/changelist-protocol/compare/v0.1.0...v0.1.1-beta.1
[0.1.0]: https://github.com/sblundy/changelist-protocol/compare/v0.1.0-beta.3...v0.1.0
[0.1.0-beta.3]: https://github.com/sblundy/changelist-protocol/compare/v0.1.0-beta.2...v0.1.0-beta.3
[0.1.0-beta.1]: https://github.com/sblundy/changelist-protocol/commits/v0.1.0-beta.1
[0.1.0-beta.2]: https://github.com/sblundy/changelist-protocol/compare/v0.1.0-beta.1...v0.1.0-beta.2
