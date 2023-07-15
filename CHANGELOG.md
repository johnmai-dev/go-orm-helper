<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Go ORM Helper Changelog

## [Unreleased]

## [1.2.0] - 2023-07-15

### Added
- Support GoFrame ORM.
- Refactor Code Completion. Better Performance!
- Refactor GoORMHelperCacheManager.

## [1.1.0] - 2023-07-04

### Added
- Assisted code completion with @Table annotation.
- Support ORM's table method.

### Fixed
- Fix SQL to Struct error.

## [1.0.2] - 2023-07-04

### Fixed
- Fix the verify SQL exception.

## [1.0.1] - 2023-07-03

### Fixed
- Fix the issue where the scanning paths in the settings cannot utilize the delete, move up, move down, and other functions.

### Optimize
- Abstract the core path of the ORM into a common class.
- Implement multi-language support, currently supporting Chinese, English, Japanese, and Korean languages.
- Separate the plugin description.

## [1.0.0] - 2023-07-02

### Added
- ORM Code Completion
- SQL to Struct

[Unreleased]: https://github.com/maiqingqiang/go-orm-helper/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/maiqingqiang/go-orm-helper/compare/v1.0.2...v1.1.0
[1.0.2]: https://github.com/maiqingqiang/go-orm-helper/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/maiqingqiang/go-orm-helper/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/maiqingqiang/go-orm-helper/tree/v1.0.0
