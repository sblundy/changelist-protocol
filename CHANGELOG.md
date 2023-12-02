<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Changelist Automated Changelog

## [Unreleased]

### Added

- `POST /changelist/{project}/{name}` to correctly handle rename semantics
- 400 responses where appropriate to OpenAPI doc

### Changed

- `PUT /changelist/{project}/{name}` no longer supports renaming a changelist

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

[Unreleased]: https://github.com/sblundy/changelist-protocol/compare/v0.1.0-beta.2...HEAD
[0.1.0-beta.1]: https://github.com/sblundy/changelist-protocol/commits/v0.1.0-beta.1
[0.1.0-beta.2]: https://github.com/sblundy/changelist-protocol/compare/v0.1.0-beta.1...v0.1.0-beta.2
