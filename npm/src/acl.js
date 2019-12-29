const { Types } = require('./permission');

/**
 * The Acl class for managing permissions.
 * @name Acl
 * @constructor
 */
function Acl(perms, resourceReg, roleReg) {
  this.permissions = perms;
  this.resources = resourceReg;
  this.roles = roleReg;
}

/**
 * Adds a resource.
 * @memberof Acl
 */
Acl.prototype.addResource = function (resource, parent) {
  this.resources.add(getValue(resource), getValue(parent));
}

Acl.prototype.addRole = function (role, parent) {
  this.roles.add(getValue(role), getValue(parent));
};

Acl.prototype.allowAllResource = function (role) {
  var roleValue = getValue(role);

  try {
    this.roles.add(roleValue);
  } catch (e) { //duplicate entry
    //do nothing
  }
  this.permissions.allow(roleValue, '*');
};

Acl.prototype.allowAllRole = function (resource) {
  var resValue = getValue(resource);

  try {
    this.resources.add(resValue);
  } catch (e) { //duplicate entry
    //do nothing
  }
  this.permissions.allow('*', resValue);
}

Acl.prototype.allow = function (role, resource, action) {
  var resValue = getValue(resource),
      roleValue = getValue(role);

  try {
    this.roles.add(roleValue);
  } catch (e) {
    //do nothing
  }
  try {
    this.resources.add(resValue);
  } catch (e) {
    //do nothing
  }
  this.permissions.allow(roleValue, resValue, action);
};

Acl.prototype.clear = function () {
  this.permissions.clear();
  this.resources.clear();
  this.roles.clear();
};

Acl.prototype.denyAllResource = function (role) {
  var roleValue = getValue(role);

  try {
    this.roles.add(roleValue);
  } catch (e) {
    //do nothing
  }
  this.permissions.deny(roleValue, '*');
};

Acl.prototype.denyAllRole = function (resource) {
  var resValue = getValue(resource);

  try {
    this.resources.add(resValue);
  } catch (e) {
    //do nothing
  }
  this.permissions.deny('*', resValue);
};

Acl.prototype.deny = function (role, resource, action) {
  var resValue = getValue(resource),
      roleValue = getValue(role);

  try {
    this.roles.add(roleValue);
  } catch (e) {
    //do nothing
  }
  try {
    this.resources.add(resValue);
  } catch (e) {
    //do nothing
  }
  this.permissions.deny(roleValue, resValue, action);
};

Acl.prototype.exportPermissions = function () {
  return this.permissions.export();
};

Acl.prototype.exportResources = function () {
  return this.resources.export();
};

Acl.prototype.exportRoles = function () {
  return this.roles.export();
};

Acl.prototype.importPermissions = function (permissions) {
  if (this.permissions.size() !== 0) {
    throw new Error(NON_EMPTY.replace(/_reg_/, 'permissions'));
  }
  this.permissions.importMap(permissions);
};

Acl.prototype.importResources = function (resources) {
  if (this.resources.size() !== 0) {
    throw new Error(NON_EMPTY.replace(/_reg_/g, 'resources'));
  }
  this.resources.importRegistry(resources);
};

Acl.prototype.importRoles = function (roles) {
  if (this.roles.size() !== 0) {
    throw new Error(NON_EMPTY.replace(/_reg_/g, 'roles'));
  }
  this.roles.importRegistry(roles);
};

Acl.prototype.isAllowed = function (role, resource, action) {
  var aco, aro, c, grant, r,
      resValue = getValue(resource),
      roleValue = getValue(role),
      resPath = this.resources.traverseRoot(resValue),
      rolePath = this.roles.traverseRoot(roleValue);

  if (!action) {
    action = Types.ALL;
  }

  //check role-resource
  for (r in rolePath) {
    if (rolePath.hasOwnProperty(r)) {
      aro = rolePath[r];
      for (c in resPath) {
        if (resPath.hasOwnProperty(c)) {
          aco = resPath[c];
          if (action === Types.ALL) {
            grant = this.permissions.isAllowedAll(aro, aco);
          } else {
            grant = this.permissions.isAllowed(aro, aco, action);
          }

          if (grant !== null) {
            return grant;
          } //else null, continue
        }
      }
    }
  }

  return false;
};

Acl.prototype.isDenied = function (role, resource, action) {
  var aco, aro, c, grant, r,
      resValue = getValue(resource),
      roleValue = getValue(role),
      resPath = this.resources.traverseRoot(resValue),
      rolePath = this.roles.traverseRoot(roleValue);

  if (!action) {
    action = Types.ALL;
  }

  //check role-resource
  for (r in rolePath) {
    if (rolePath.hasOwnProperty(r)) {
      aro = rolePath[r];
      for (c in resPath) {
        if (resPath.hasOwnProperty(c)) {
          aco = resPath[c];
          if (action === Types.ALL) {
            grant = this.permissions.isDeniedAll(aro, aco);
          } else {
            grant = this.permissions.isDenied(aro, aco, action);
          }

          if (grant !== null) {
            return grant;
          } //else null, continue
        }
      }
    }
  }

  return false;
};

Acl.prototype.makeDefaultAllow = function () {
  this.permissions.makeDefaultAllow();
};

Acl.prototype.makeDefaultDeny = function () {
  this.permissions.makeDefaultDeny();
};

Acl.prototype.remove = function (role, resource, action) {
  this.permissions.remove(getValue(role), getValue(resource), action);
};

Acl.prototype.removeResource = function (resource, removeDescendants) {
  var i, resources,
      resId = getValue(resource);

  if (resId === null) {
    throw new Error('Cannot remove null resource');
  }
  resources = this.resources.remove(resId, removeDescendants);
  for (i = 0; i < resources.length; i++) {
    this.permissions.removeByResource(resources[i]);
  }
};

Acl.prototype.removeRole = function (role, removeDescendants) {
  var i, roles,
      roleId = getValue(role);

  if (roleId === null) {
    throw new Error('Cannot remove null role');
  }
  roles = this.roles.remove(roleId, removeDescendants);
  for (i = 0; i < roles.length; i++) {
    this.permissions.removeByRole(roles[i]);
  }
};

Acl.prototype.visualize = function () {
  var output = [];

  output.push(this.roles.toString());
  output.push('\n');
  output.push(this.resources.toString());
  output.push('\n');
  output.push(this.permissions.toString());
  output.push('\n');

  return output.join('');
};

Acl.prototype.visualizePermissions = function () {
  return this.permissions.toString();
};

Acl.prototype.visualizeResources = function (loader) {
  return this.resources.display(loader, null, null);
};

Acl.prototype.visualizeRoles = function (loader) {
  return this.roles.display(loader, null, null);
};

function getValue(val) {
  if (!val) {
    return null;
  }
  if (typeof val.getId === 'function' &&
  typeof val.getId() === 'string') {
    return val.getId();
  }
  return val;
}

module.exports = Acl;