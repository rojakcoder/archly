# Archly

Archly is a project for creating a hierarchy-based access control list (ACL).

### Things to Note

The library requires that the roles and resources added to it must either be a string or have a method `getId`. If not, when adding the entity, an error will be thrown.

Roles and resources are stored in separate registries.

A role/resource may only have one entry in the registry. This means that a role/resource can only be placed under one group.

### Getting Started

Get an instance of the ACL:

```
var acl = Archly.newAcl();
```

This is a self-contained instance. So you can create multiple instances to represent different rules.

Next is to decide whether to make the default permission access or deny. Without changing anything, the default is deny.

```
var acl = Archly.newAcl();
acl.makeDefaultDeny(); // Default deny - this is redundant.
acl.makeDefaultAllow(); // Default allow.
```

**Example**

For example, you can have a set of rules that grants access by default, and another set that denies access by default in the same app:

```
var aclDeny = Archly.newAcl();
var aclGrant = Archly.newAcl();
aclGrant.makeDefaultAllow();
```

The use case for most cases is to deny by default, so that is the *default* default (pardon the pun).

### Add a role entry

A root entry is created without specifying a parent.

```
acl.addRole('role1');
```

Adding the same entry will throw an error.

```
acl.addRole('role1');
try {
  acl.addRole('role1');
} catch (e) {
  console.log('entry already exists');
}
```

#### Add a child role entry

The parent role should be specified as the second parameter:

```
acl.addRole('role1a', 'role1');
```

If the parent entry does not exist in the registry, referencing it will throw an error.

```
try {
  acl.addRole('role2', 'non-existing');
} catch (e) {
  console.log('parent does not exist');
}
```

Adding the same entry will throw an error. Remember that an entry can only be placed under one group.

```
try {
  acl.addRole('role1a', 'role1');
} catch (e) {
  console.log('entry already exists');
}
try {
  acl.addRole('role1a', 'a-different-role');
} catch (e) {
  console.log('considered a duplicate even if under a different parent');
}
```

**Example**

A very common example is to have a department represented at the root level:

```
acl.addRole('it-department');
```

Then add the divisions in it.

```
acl.addRole('developers', 'it-department');
acl.addRole('operations', 'it-department');
acl.addRole('support', 'it-department');
```

While the manager of the department presumably has a set of elevated privileges different from the rest of the department, she can still be added to the department as her privileges can be defined explictly for her, thereby overriding any restrictions the department may have.

    acl.addRole('manager', 'it-department');

Let's also add sub-divisions to the developers group.

```
acl.addRole('mobile', 'developers')
acl.addRole('ios', 'mobile');
acl.addRole('android', 'mobile');
acl.addRole('web', 'developers');
acl.addRole('vue', 'web');
```

(Example to be continued below.)

### Add a resource entry

This has the same behaviour as adding a role. Simply swap `addRole` with `addResource`.

### Remove a role entry

When removing a role, there is a consideration of whether to preserve the descendant roles of that role. To remove all descendant roles, specify `true` in the second parameter of the function call:

    acl.removeRole('role1', true);

To preserve the descendant roles, specify `false`:

    acl.removeRole('role1', false);

By not removing the descendant roles, they will be "moved up" in the hierarchy. This means that the parent role for these roles will be the parent role of the removed one.

If the entry does not exist in the registry, trying to remove it will throw an error.

```
try {
  acl.removeRole('role1');
} catch (e) {
  console.log('entry does not exist');
}
```
**Example**

Continuing from the earlier example, say we want to remove the whole mobile developer division:

    acl.removeRole('mobile', true);

This will also remove the "ios" and "android" sub-divisions.

Let us also remove the "web" division but keep the "vue" division.

    acl.removeRole('web', false);

This will make "developers" the parent of "vue".

### Granting Permission

To grant permission on a resource to a role:

    acl.allow('role', 'resource');

Without specifying a third parameter, it implies the permission type is "ALL".

If specified, the permission is explicitly limited to that permission type:

    acl.allow('role', 'resource', 'CREATE');

Note that this function call will add the role and resource into the registry if they are not already present. This addition into the registry will only add the entity at the root.

If a hierarchy of the role/resource is needed, they should first be set up by calling `addRole`/`addResource`.

**Example**

Following up on the example, say that a "computers" resource is granted to all of the "it-department".

    acl.allow('it-department', 'computers');

Let's also give "operations" access to "smartphones".

    acl.allow('operations', 'smartphones');

### Checking Permissions

After granting permissions, check access permissions like so:

    acl.isAllowed('role', 'res');

**Example**

To check if the "operations" group has access to "computers":

    acl.isAllowed('operations', 'computers'); // true

This will return true because "operations" is under "it-department" which is granted access to "computers". Since "operations" itself has no specific assignment to "computers", it inherits the assignment from "it-department".

To check if "operations" has access to "smartphones":

    acl.isAllowed('operations', 'smartphones'); // true

This will return true because there is a specific assignment to "operations".

To check if "it-department" has access to "smartphones":

    acl.isAllowed('it-department', 'smartphones'); // false

This will return false because there is no assignment for this permission.

### Overriding Permissions

There is no explicit mechanism to override permissions. Rather, since permissions can be added at any level, the overriding is implicit.

**Example**

To remove access to "computers" by the "operations" department:

    acl.deny('operations', 'computers');
    acl.isAllowed('operations', 'computers'); // false

### Catchall Permissions

If either the resources or the roles are a flat hierarchy (i.e. all entities have equal priority), there are methods that can assign permissions in a much simpler way:

    acl.allowAllRole('res1');

This method above gives all roles permission to access "res1";

    acl.allowAllResource('role1');

This method above gives the role permission to access all resources.

**Note**: This blanket permission can be overridden by more specific permissions like any other permissions.

**Example**

To give every department access to "computers":

    acl.allowAllRole('computers');

In an earlier example, "computers" was denied to "operations". Now that the "computers" has been given access to "all" roles, should "operations" have access to "computers" or not?

    acl.isAllowed('operations', 'computers'); // What should this be?

What value should this expression return?

Answer is false. This is because the deny of "computers" to "operations" is more explicit than the general granting of "computers" to "all".

### More Advanced Scenarios


### Visualizations


Call `toString()` on the permission object to get a visualization of the permissions map.

#### Export/Import

The permissions, resource registry, and role registry can be exported as JSON objects and saved in persistent storage:

```
acl.exportPermissions();
acl.exportResources();
acl.exportRoles();
```

The JSON objects can then be restored by importing them:

```
var a = Archly.newAcl();
a.importRoles(acl.exportRoles());
a.importResources(acl.exportResources());
a.importPermissions(acl.exportPermissions());
```

If the registry and permissions map is not empty during the import, an error will be thrown.
