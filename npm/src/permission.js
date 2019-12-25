const PERM_NOT_FOUND = "Permission '_perm_' not found on '_role_' for '_res_'.";

const Types = {
  ALL: 'ALL',
  CREATE: 'CREATE',
  READ: 'READ',
  UPDATE: 'UPDATE',
  DELETE: 'DELETE'
};

/**
 * The map of role-resource tuple to permissions.
 * The first level key is the tuple, the second level key is the action.
 * Available actions are "ALL", "CREATE", "READ", "UPDATE", "DELETE".
 * @name Permission
 * @constructor
 */
function Permission() {
  //map of string to map of string-bool
  this.perms = {};
  this.makeDefaultDeny();
}

Permission.DEFAULT_KEY = '*::*';


/**
 * Visualization of the permissions in tuples.
 * @memberof Permission
 */
Permission.prototype.toString = function () {
  var action, entry, tuple,
      out = ['Size: '];

  out.push(this.size());
  out.push('\n-------\n');

  for (tuple in this.perms) {
    if (this.perms.hasOwnProperty(tuple)) {
      out.push('- ');
      out.push(tuple);
      out.push('\n');
      entry = this.perms[tuple];
      for (action in entry) {
        if (entry.hasOwnProperty(action)) {
          out.push('\t');
          out.push(action);
          out.push('\t');
          out.push(entry[action]);
          out.push('\n');
        }
      }
    }
  }

  return out.join('');
};

/**
 * Grants action permission on resource to role.
 * Adds the action permission to any existing actions. Overrides the
 * existing action permission if any.
 *
 * @param {string} role - The ID of the access request object.
 * @param {string} resource - The ID of the access control object.
 * @param {string} [action=ALL] - The access action.
 * @memberof Permission
 */
Permission.prototype.allow = function (role, resource, action) {
  var perm, key = this.makeKey(role, resource);

  if (!action) {
    action = Types.ALL;
  }
  if (this.has(key)) {
    perm = this.perms[key];
    perm[action] = true;
  } else {
    this.perms[key] = this.makePermission(action, true);
  }
};

/**
 * Removes all permissions.
 * @memberof Permission
 */
Permission.prototype.clear = function () {
  this.perms = {};
};

/**
 * Denies action permission on resource to role.
 * Adds the action permission to any existing actions. Overrides the
 * existing action permission if any.
 *
 * @param {string} role - The ID of the access request object.
 * @param {string} resource - The ID of the access control object.
 * @param {string} [action=ALL] - The access action.
 * @memberof Permission
 */
Permission.prototype.deny = function (role, resource, action) {
  var perm, key = this.makeKey(role, resource);

  if (!action) {
    action = Types.ALL;
  }
  if (this.has(key)) {
    perm = this.perms[key];
    perm[action] = false;
  } else {
    this.perms[key] = this.makePermission(action, false);
  }
};

/**
 * Exports a snapshot of the permissions map.
 *
 * @return {Object} A map with string keys and map values where
 * the values are maps of string to boolean entries. Typically
 * meant for persistent storage.
 * @memberof Permission
 */
Permission.prototype.export = function () {
  var i, j, clone = {};

  for (i in this.perms) {
    if (this.perms.hasOwnProperty(i)) {
      clone[i] = {};
      for (j in this.perms[i]) {
        if (this.perms[i].hasOwnProperty(j)) {
          clone[i][j] = this.perms[i][j];
        }
      }
    }
  }

  return clone;
};

/**
 * Determines if the role-resource tuple is available.
 *
 * @param {string} key - The tuple of role and resource.
 * @return {Boolean} Returns true if the permission is available for this tuple.
 * @memberof Permission
 */
Permission.prototype.has = function (key) {
  return this.perms.hasOwnProperty(key);
};

/**
 * Re-creates the permission map with a new of permissions.
 *
 * @param {Object} map - The map of string-string-boolean
 * tuples. The first-level string is a permission key
 * (<aco>::<aro>); the second-level string is the set of
 * actions; the boolean value indicates whether the permission
 * is explicitly granted/denied.
 * @memberof Permission
 */
Permission.prototype.importMap = function (map) {
  var i, j;

  this.perms = {};
  for (i in map) {
    if (map.hasOwnProperty(i)) {
      this.perms[i] = {};
      for (j in map[i]) {
        if (map[i].hasOwnProperty(j)) {
          this.perms[i][j] = map[i][j];
        }
      }
    }
  }
};

/**
 * Determines if the role has access on the resource.
 *
 * @param {string} role - The ID of the access request object.
 * @param {string} resource - The ID of the access control object.
 * @return {Boolean} Returns true only if the role has been
 * explicitly given access to all actions on the resource.
 * Returns false if the role has been explicitly denied access.
 * Returns null otherwise.
 * @memberof Permission
 */
Permission.prototype.isAllowedAll = function (role, resource) {
  var k, perm,
      allSet = 0,
      key = this.makeKey(role, resource);

  if (!this.has(key)) {
    return null;
  }

  perm = this.perms[key];
  for (k in perm) {
    if (perm.hasOwnProperty(k)) {
      if (perm[k] === false) {
        return false;
      }
      if (k !== Types.ALL) {
        allSet++;
      }
    }
  }
  if (perm.hasOwnProperty(Types.ALL)) {
    return true; //true because ALL=false would be caught in the loop
  }

  if (allSet === 4) {
    return true;
  }

  return null;
};

/**
 * Determines if the role has access on the resource for the specific action.
 * The permission on the specific action is evaluated to see if it has been
 * specified. If not specified, the permission on the <code>ALL</code>
 * permission is evaluated. If both are not specified, <code>null</code> is
 * returned.
 *
 * @param {string} role - The ID of the access request object.
 * @param {string} resource - The ID of the access control object.
 * @param {string} action - The access action.
 * @return {Boolean} Returns true if the role has access to the specified
 * action on the resource. Returns false if the role is denied
 * access. Returns null if no permission is specified.
 * @memberof Permission
 */
Permission.prototype.isAllowed = function (role, resource, action) {
  var perm, key = this.makeKey(role, resource);

  if (!this.has(key)) {
    return null;
  }

  perm = this.perms[key];
  if (perm[action] === undefined) {
    //if specific action is not present, check for ALL
    if (perm[Types.ALL] === undefined) {
      return null;
    }

    return perm[Types.ALL];
  } //else specific action is present

  return perm[action];
};

/**
 * Determines if the role is denied access on the resource.
 *
 * @param {string} role - The ID of the access request object.
 * @param {string} resource - The ID of the access control object.
 * @return {Boolean} Returns true only if the role has been
 * explicitly denied access to all actions on the resource.
 * Returns false if the role has been explicitly granted access.
 * Returns null otherwise.
 * @memberof Permission
 */
Permission.prototype.isDeniedAll = function (role, resource) {
  var k, perm,
      allSet = 0,
      key = this.makeKey(role, resource);

  if (!this.has(key)) {
    return null;
  }

  perm = this.perms[key];
  for (k in perm) {
    if (perm.hasOwnProperty(k)) {
      //if any entry is true, resource is NOT denied
      if (perm[k]) {
        return false;
      }
      if (k !== Types.ALL) {
        allSet++;
      }
    }
  }
  if (perm.hasOwnProperty(Types.ALL)) {
    return true; //true because ALL=true would be caught in the loop
  }

  if (allSet === 4) {
    return true;
  }

  return null;
};

/**
 * Determines if the role is denied access on the resource for the specific
 * action.
 * The permission on the specific action is evaluated to see if it has been
 * specified. If not specified, the permission on the <code>ALL</code>
 * permission is evaluated. If both are not specified, <code>null</code> is
 * returned.
 *
 * @param {string} role - The ID of the access request object.
 * @param {string} resource - The ID of the access control object.
 * @param {string} action - The access action.
 * @return {Boolean} Returns true if the role is denied access
 * to the specified action on the resource. Returns false if the
 * role has access. Returns null if no permission is specified.
 * @memberof Permission
 */
Permission.prototype.isDenied = function (role, resource, action) {
  var perm, key = this.makeKey(role, resource);

  if (!this.has(key)) {
    return null;
  }

  perm = this.perms[key];
  if (perm[action] === undefined) {
    //if specific action is not present, check for ALL
    if (perm[Types.ALL] === undefined) {
      return null;
    }

    return !perm[Types.ALL];
  } //else specific action is present

  return !perm[action];
};

/**
 * Makes the default permission allow.
 * @memberof Permission
 */
Permission.prototype.makeDefaultAllow = function () {
  this.perms[Permission.DEFAULT_KEY] = this.makePermission(Types.ALL, true);
};

/**
 * Makes the default permission deny.
 * @memberof Permission
 */
Permission.prototype.makeDefaultDeny = function () {
  this.perms[Permission.DEFAULT_KEY] = this.makePermission(Types.ALL, false);
};

/**
 * Removes the specified permission on resource from role.
 *
 * @param {string} role - The ID of the access request object.
 * @param {string} resource - The ID of the access control object.
 * @param {string} [action=ALL] - The access action.
 * @throws Will throw an error if the permission is not available.
 * @memberof Permission
 */
Permission.prototype.remove = function (role, resource, action) {
  var orig, perm, type,
      key = this.makeKey(role, resource),
      resId = resource ? resource : '*',
      roleId = role ? role : '*';

  if (!action) {
    action = Types.ALL;
  }
  if (!this.has(key)) {
    throw new Error(PERM_NOT_FOUND.replace(/_perm_/g, key)
      .replace(/_res_/g, resId)
      .replace(/_role_/g, roleId));
  }
  perm = this.perms[key];
  if (perm[action] !== undefined) { //i.e. if action is defined
    if (action === Types.ALL) {
      delete this.perms[key];

      return;
    } else { //remove specific action
      delete perm[action];
    }
  } else if (perm[Types.ALL] !== undefined) {
    //has ALL - remove and put in the others
    orig = perm[Types.ALL];
    delete perm[Types.ALL];

    for (type in Types) {
      if (Types.hasOwnProperty(type)) {
        if (type !== action && type !== Types.ALL) {
          perm[type] = orig;
        }
      }
    }
  } else if (action === Types.ALL) {
      delete this.perms[key];

      return;
  } else { //i.e. action is not defined
      throw new Error(PERM_NOT_FOUND.replace(/_perm_/g, action)
              .replace(/_res_/g, resId)
              .replace(/_role_/g, roleId));
  }

  if (Object.keys(perm).length === 0) {
      delete this.perms[key];
  } else {
      this.perms[key] = perm;
  }
};

/**
 * Removes all permissions related to the resource.
 *
 * @param {string} resourceId - The ID of the resource to remove.
 * @return {Number} The number of removed permissions.
 * @memberof Permission
 */
Permission.prototype.removeByResource = function (resourceId) {
  var key,
      toRemove = [],
      resId = '::' + resourceId;

  for (key in this.perms) {
    if (this.perms.hasOwnProperty(key)) {
      if (key.endsWith(resId)) {
        toRemove.push(key);
      }
    }
  }

  return remove(this.perms, toRemove);
};

/**
 * Removes all permissions related to the role.
 *
 * @param {string} roleId - The ID of the role to remove.
 * @return {Number} The number of removed permissions.
 * @memberof Permission
 */
Permission.prototype.removeByRole = function (roleId) {
  var key,
      toRemove = [],
      rolId = roleId + '::';

  for (key in this.perms) {
    if (this.perms.hasOwnProperty(key)) {
      if (key.startsWith(rolId)) {
        toRemove.push(key);
      }
    }
  }

  return remove(this.perms, toRemove);
};

/**
 * The number of specified permissions.
 *
 * @return {Number} The number of permissions in the registry.
 * @memberof Permission
 */
Permission.prototype.size = function () {
  return Object.keys(this.perms).length;
};

/**
 * Creates the key in the form "<aro>::<aco>" where "<aro>" is
 * the ID of the role, and "<aco>" is the ID of the resource.
 *
 * @param {string} role The ID of the role.
 * @param {string} resource The ID of the resource.
 * @return {string} The key of the permission.
 * @memberof Permission
 */
Permission.prototype.makeKey = function (role, resource) {
  var aco = resource ? resource : '*',
      aro = role ? role : '*';

  return aro + '::' + aco;
};

/**
 * Creates a permissions map.
 *
 * @param {string} action The action to set. Accepted values are
 * "ALL", "CREATE", "READ", "UPDATE", "DELETE".
 * @param {Boolean} allow Either true or false to grant or deny access.
 * @return {Object} The map of string-boolean values.
 * @memberof Permission
 */
Permission.prototype.makePermission = function (action, allow) {
  var perm = {};

  perm[action] = allow;

  return perm;
};

/**
 * Helper function called by remove functions to remove permissions.
 * @param {Object} perms - The map of permissions - Permission.perms.
 * @param {string[]} keys - The array of keys to remove from the permission.
 * @return {Number} Returns the number of removed permissions.
 */
function remove(perms, keys) {
  var i,
      removed = 0;

  for (i = 0; i < keys.length; i++) {
    if (perms.hasOwnProperty(keys[i])) {
      delete perms[keys[i]];
      removed++;
    }
  }

  return removed;
}

module.exports = {
  Permission,
  Types,
};