# Change Log

## 0.3.1 - 2016-04-11

Cleaned up the codebase a bit before changing directory.

## 0.3.0 - 2016-04-08

Achieved 100% test coverage.

### Added
- Added a new exception for import methods.
- Added new export methods.

## 0.2.0 - 2016-03-04

### Added
- Added methods for initializing the ACL and registries.
- Also added a corresponding exception for the operation.

## 0.1.0 - 2016-02-26

### Added
- Added a method to clear the ACL.

### Changed
- Corrected the isAllowed() and isDenied() methods of Acl to
prioritise an explicit permission setting over an inherited one.
- Updated the methods in Permission in tandem to achive this objective.
- Filled in some missing docblocks.
- Updated the tests correspondingly to achieve 100% code coverage.

## 0.0.1 - 2016-02-24

Initial commit

