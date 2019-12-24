## Introduction

Archly is a project for creating a hierarchy-based access control list (ACL).

This project is inspired by [CakePHP's Access Control
Lists](http://book.cakephp.org/2.0/en/core-libraries/components/access-control-lists.html)

## Java

The Java implementation of this ACL library is under the `java` folder.

The library functions are exposed via the Acl.java class.

The ACL manages access to resources (also known as Access Control
Objects) by users and roles (aka. Access Request Objects).

Any class may be tagged as an Access Request Object (ARO) or an
Access Control Object (ACO) by implementing the `RegistryEntry` interface.
