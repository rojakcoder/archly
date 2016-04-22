# Change Log

## 2016-04-21

### Added
- Added methods for exporting and importing permissions.

## 2016-04-19

### Added
- Added methods removeByResource() and removeByRole() for both
JavaScript and Java implementations.
- Added a test for concurernt modifications on permissions for
removeByResource()/removeByRole()

## 2016-04-15

Added the JavaScript implementation.

## 2016-04-14

Moved the entire codebase into the `java` directory.

## 0.4.0 - 2016-04-13

### Changed
- Changed the underlying classes to use String rather than AclEntry
for the API. The conversion from AclEntry to String is performed
inside Acl.java
- Changed Registry.print() to Registry.display()

## 0.3.2 - 2016-04-12

### Changed
- Changed the instance method print() to display() for Registry.

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

