(function (win) {
    'strict';

    var DUPLICATE_CHILD = "Entry '_child_' is already a child of '_entry_'",
        DUPLICATE_ENTRIES = "Entry '_entry_' is already in the registry - cannot add duplicate.",
        ENTRY_NOT_FOUND = "Entry '_entry_' not in registry",
        NON_EMPTY = "_reg registry is not empty";
        PERM_NOT_FOUND = "Permission '_perm_' not found on '_role_' for '_res_'";

    /**
     * The map of role-resource tupe to permissions.
     * The first level key is the tupe, the second level key is the action.
     * Available actions are "ALL", "CREATE", "READ", "UPDATE", "DELETE".
     * @constructor
     */
    function Permission() {
        //map of string to map of string-bool
        this.perms = {};
        this.makeDefaultDeny();
    }

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

    Permission.DEFAULT_KEY = '*::*';

    Permission.Types = {
        ALL: 'ALL',
        CREATE: 'CREATE',
        READ: 'READ',
        UPDATE: 'UPDATE',
        DELETE: 'DELETE'
    };

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

    Permission.prototype.allow = function (role, resource, action) {
        var perm, key = this.makeKey(role, resource);

        if (!action) {
            action = Permission.Types.ALL;
        }
        if (this.has(key)) {
            perm = this.perms[key];
            perm[action] = true;
        } else {
            this.perms[key] = this.makePermission(action, true);
        }
    };

    Permission.prototype.clear = function () {
        this.perms = {};
    };

    Permission.prototype.deny = function (role, resource, action) {
        var perm, key = this.makeKey(role, resource);

        if (!action) {
            action = Permission.Types.ALL;
        }
        if (this.has(key)) {
            perm = this.perms[key];
            perm[action] = false;
        } else {
            this.perms[key] = this.makePermission(action, false);
        }
    };

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

    Permission.prototype.has = function (key) {
        return !!this.perms[key];
    };

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
                if (k !== Permission.Types.ALL) {
                    allSet++;
                }
            }
        }
        if (perm.hasOwnProperty(Permission.Types.ALL)) {
            return true; //true because ALL=false would be caught in the loop
        }

        if (allSet === 4) {
            return true;
        }

        return null;
    };

    Permission.prototype.isAllowed = function (role, resource, action) {
        var perm, key = this.makeKey(role, resource);

        if (!this.has(key)) {
            return null;
        }

        perm = this.perms[key];
        if (perm[action] === undefined) {
            //if specific action is not present, check for ALL
            if (perm[Permission.Types.ALL] === undefined) {
                return null;
            }

            return perm[Permission.Types.ALL];
        } //else specific action is present

        return perm[action];
    };

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
                if (k !== Permission.Types.ALL) {
                    allSet++;
                }
            }
        }
        if (perm.hasOwnProperty(Permission.Types.ALL)) {
            return true; //true because ALL=true would be caught in the loop
        }

        if (allSet === 4) {
            return true;
        }

        return null;
    };

    Permission.prototype.isDenied = function (role, resource, action) {
        var perm, key = this.makeKey(role, resource);

        if (!this.has(key)) {
            return null;
        }

        perm = this.perms[key];
        if (perm[action] === undefined) {
            //if specific action is not present, check for ALL
            if (perm[Permission.Types.ALL] === undefined) {
                return null;
            }

            return !perm[Permission.Types.ALL];
        } //else specific action is present

        return !perm[action];
    };

    Permission.prototype.makeDefaultAllow = function () {
        this.perms[Permission.DEFAULT_KEY] = this.makePermission(Permission.Types.ALL, true);
    };

    Permission.prototype.makeDefaultDeny = function () {
        this.perms[Permission.DEFAULT_KEY] = this.makePermission(Permission.Types.ALL, false);
    };

    Permission.prototype.remove = function (role, resource, action) {
        var orig, perm,
            key = this.makeKey(role, resource),
            resId = resource ? resource : '*',
            roleId = role ? role : '*';

        if (!action) {
            action = Permission.Types.ALL;
        }
        if (!this.has(key)) {
            throw new Error(PERM_NOT_FOUND.replace(/_perm_/g, key)
                    .replace(/_res_/g, resId)
                    .replace(/_role_/g, roleId));
        }
        perm = this.perms[key];
        if (perm[action] !== undefined) { //i.e. if action is defined
            if (action === Permission.Types.ALL) {
                delete this.perms[key];

                return;
            } else { //remove specific action
                delete perm[action];
            }
        } else if (perm[Permission.Types.ALL] !== undefined) {
            //has ALL - remove and put in the others
            orig = perm[Permission.Types.ALL];
            delete perm[Permission.Types.ALL];

            for (type in Permission.Types) {
                if (Permission.Types.hasOwnProperty(type)) {
                    if (type !== action && type !== Permission.Types.ALL) {
                        perm[type] = orig;
                    }
                }
            }
        } else if (action === Permission.Types.ALL) {
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

    Permission.prototype.size = function () {
        return Object.keys(this.perms).length;
    };

    Permission.prototype.makeKey = function (role, resource) {
        var aco = resource ? resource : '*',
            aro = role ? role : '*';

//        if (!role) {
//            aro = '*';
//        } else {
//            aro = role.getId();
//        }
//        if (!resource) {
//            aco = '*';
//        } else {
//            aco = resource.getId();
//        }
//
        return aro + '::' + aco;
    };

    Permission.prototype.makePermission = function (action, allow) {
        var perm = {};

        perm[action] = allow;

        return perm;
    };

    /**
     * The registry for resources and roles.
     * @constructor
     */
    function Registry() {
        this.registry = {};
    }

    /**
     * Prints the traversal path from the entry to the root.
     *
     * @param {array} path - The list representing the path.
     */
    Registry.display = function (path) {
        var i, out = '-';

        for (i in path) {
            if (path.hasOwnProperty(i)) {
                out += ' -> ';
                out += path[i];
            }
        }
        out += ' <';

        return out;
    }

    /**
     * Adds an entry to the registry.
     *
     * @param {AclEntry} entry - The entry to add.
     * @param {AclEntry} [parent] - The parent entry under which to
     * place the child entry.
     * @throws Will throw an error if the entry is already in the registry.
     */
    Registry.prototype.add = function (entry, parent) {
        if (this.has(entry)) {
            throw new Error(DUPLICATE_ENTRIES.replace(/_entry_/g, entry));
        }
        if (parent) {
            this.registry[entry] = parent;
        } else {
            this.registry[entry] = '';
        }
    };

    /**
     * Empties the registry.
     */
    Registry.prototype.clear = function () {
        this.registry = {};
    };

    /**
     * Clones the registry to export.
     *
     * @return {object} A clone of the registry.
     */
    Registry.prototype.export = function () {
        var i, clone = {};

        for (i in this.registry) {
            if (this.registry.hasOwnProperty(i)) {
                clone[i] = this.registry[i];
            }
        }

        return clone;
    };

    /**
     * Checks if the entry is stored in the registry.
     *
     * @param {AclEntry} entry - The entry to check.
     * @return {boolean} True if the ID of the entry is present in
     * the registry.
     */
    Registry.prototype.has = function (entry) {
        return this.registry[entry] !== undefined;
    }

    /**
     * Checks if there are children IDs under the specified ID.
     *
     * @param {string} parentId - The ID of the parent to check for.
     * @return {boolean} True if there is at least one child ID.
     */
    Registry.prototype.hasChild = function (parentId) {
        for (var i in this.registry) {
            if (this.registry.hasOwnProperty(i)) {
                if (this.registry[i] === parentId) {
                    return true;
                }
            }
        }

        return false;
    };

    /**
     * Re-creates the registry with a new hierarchy.
     *
     * @param {object} map - The map containing the new hierarchy.
     */
    Registry.prototype.importRegistry = function (map) {
        var i;

        this.registry = {};
        for (i in map) {
            if (map.hasOwnProperty(i)) {
                this.registry[i] = map[i];
            }
        }
    };

    /**
     * Creates a traversal path from the entry to the root.
     *
     * @param {AclEntry} entry - The ID of the entry to start
     * traversing from.
     * @return {array} A list of entry IDs starting from the entry and
     * ending with the root.
     */
    Registry.prototype.traverseRoot = function (entry) {
        var eId, path = [];

        if (entry == null) {
            path.push('*');

            return path;
        }

        eId = entry;

        while (this.registry[eId] !== undefined) {
            path.push(eId);
            eId = this.registry[eId];
        }
        path.push('*');

        return path;
    };

    /**
     * Prints a cascading list of entries in this registry.
     *
     * @param {object} loader - An object to retrieve other entries.
     * @param {string} leading - The leading space for indented entries.
     * @param {string} entryId - The ID of the entry to start traversing from.
     * @return {string} The string representing the parent-child
     * relationships between the entries.
     */
    Registry.prototype.display = function (loader, leading, entryId) {
        var tis = this,
            childIds, output = [];

        if (!leading) {
            leading = '';
        }
        if (!entryId) {
            entryId = '';
        }

        childIds = findChildren(this.registry, entryId);
        childIds.forEach(function (childId) {
            entry = loader.retrieveEntry(childId);
            output.push(leading);
            output.push('- ');
            output.push(entry.getEntryDescription());
            output.push('\n');
            output.push(tis.display(loader, ' ' + leading, childId));
        });

        return output.join('');
    };

    /**
     * Removes an entry from the registry.
     *
     * @param {AclEntry} entry - The entry to remove from the registry.
     * @param {boolean} removeDescendants - If true, all child
     * entries and descendants are removed as well.
     * @throws Will throw an error if the entry or any of the
     * descendants (if 'removeDescendants' is true) are not found.
     */
    Registry.prototype.remove = function (entry, removeDescendants) {
        var parentId,
            childIds = [],
            reg = this.registry,
            removed = [];

        if (!this.has(entry)) {
            throw new Error(ENTRY_NOT_FOUND.replace(/_entry_/g, entry));
        }

        if (this.hasChild(entry)) {
            parentId = this.registry[entry];
            childIds = findChildren(this.registry, entry);

            if (removeDescendants) {
                removed = removed.concat(remDescendants(this, childIds));
            } else {
                childIds.forEach(function(childId) {
                    reg[childId] = parentId;
                });
            }
        }

        delete this.registry[entry];
        removed.push(entry);

        return removed;
    };

    Registry.prototype.size = function () {
        return Object.keys(this.registry).length;
    };

    Registry.prototype.toString = function () {
        var value, key,
            output = [];

        for (key in this.registry) {
            if (this.registry.hasOwnProperty(key)) {
                value = this.registry[key];
                output.push('\t');
                output.push(key);
                if (key.length >= 8) {
                    output.push('\t - \t');
                } else {
                    output.push('\t\t - \t');
                }
                if (value === '') {
                    output.push('*');
                } else {
                    output.push(value);
                }
                output.push('\n');
            }
        }

        return output.join('');
    };

    function remDescendants(reg, entryIds) {
        var removed = [];

        entryIds.forEach(function (entryId) {
            //registry.remove(entryId);
            delete reg.registry[entryId];
            removed.push(entryId);
            while (reg.hasChild(entryId)) {
                removed = removed.concat(remDescendants(reg, findChildren(reg.registry, entryId)));
            }
        });

        return removed;
    }

    function findChildren(registry, parentId) {
        var key,
            children = [];

        for (key in registry) {
            if (registry.hasOwnProperty(key)) {
                if (registry[key] === parentId) {
                    children.push(key);
                }
            }
        }

        return children;
    }

    function Acl(perms, resourceReg, roleReg) {
        this.perms = perms;
        this.resources = resourceReg;
        this.roles = roleReg;
    }

    function getValue(val) {
        if (!val) {
            return null;
        }
        if (typeof val === 'string') {
            return val;
        }
        if (typeof val.getId === 'function' &&
                typeof val.getId() === 'string') {
            return val.getId();
        }
        throw new Error('Invalid entry type - expected a string or object with the getId() method.');
    }

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
        this.perms.allow(roleValue, '*');
    };

    Acl.prototype.allowAllRole = function (resource) {
        var resValue = getValue(resource);

        try {
            this.resources.add(resValue);
        } catch (e) { //duplicate entry
            //do nothing
        }
        this.perms.allow('*', resValue);
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
        this.perms.allow(roleValue, resValue, action);
    };

    Acl.prototype.clear = function () {
        this.perms.clear();
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
        this.perms.deny(roleValue, '*');
    };

    Acl.prototype.denyAllRole = function (resource) {
        var resValue = getValue(resource);

        try {
            this.resources.add(resValue);
        } catch (e) {
            //do nothing
        }
        this.perms.deny('*', resValue);
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
        this.perms.deny(roleValue, resValue, action);
    };

    Acl.prototype.exportPermissions = function () {
        return this.perms.export();
    };

    Acl.prototype.exportResources = function () {
        return this.resources.export();
    };

    Acl.prototype.exportRoles = function () {
        return this.roles.export();
    };

    Acl.prototype.importPermissions = function (permissions) {
        if (this.perms.size() !== 0) {
            throw new Error(NON_EMPTY.replace(/_reg_/, 'permissions'));
        }
        this.perms.importMap(permissions);
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
            action = Permission.Types.ALL;
        }

        //check role-resource
        for (r in rolePath) {
            if (rolePath.hasOwnProperty(r)) {
                aro = rolePath[r];
                for (c in resPath) {
                    if (resPath.hasOwnProperty(c)) {
                        aco = resPath[c];
                        if (action === Permission.Types.ALL) {
                            grant = this.perms.isAllowedAll(aro, aco);
                        } else {
                            grant = this.perms.isAllowed(aro, aco, action);
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
            action = Permission.Types.ALL;
        }

        //check role-resource
        for (r in rolePath) {
            if (rolePath.hasOwnProperty(r)) {
                aro = rolePath[r];
                for (c in resPath) {
                    if (resPath.hasOwnProperty(c)) {
                        aco = resPath[c];
                        if (action === Permission.Types.ALL) {
                            grant = this.perms.isDeniedAll(aro, aco);
                        } else {
                            grant = this.perms.isDenied(aro, aco, action);
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
        this.perms.makeDefaultAllow();
    };

    Acl.prototype.makeDefaultDeny = function () {
        this.perms.makeDefaultDeny();
    };

    Acl.prototype.remove = function (role, resource, action) {
        this.perms.remove(getValue(role), getValue(resource), action);
    };

    Acl.prototype.removeResource = function (resource, removeDescendants) {
        var i, resources,
            resId = getValue(resource);

        if (resId === null) {
            throw new Error('Cannot remove null resource');
        }
        resources = this.resources.remove(resId, removeDescendants);
        for (i = 0; i < resources.length; i++) {
            this.perms.removeByResource(resources[i]);
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
            this.perms.removeByRole(roles[i]);
        }
    };

//    win.Archly = {
//        Acl: new Acl(PERMS, RESOURCE_REGISTRY, ROLE_REGISTRY),
//        PermissionTypes: Permission.Types,
//        Permissions: PERMS,
//        ResourceRegistry: RESOURCE_REGISTRY,
//        RoleRegistry: ROLE_REGISTRY,
//    };

    win.PERMISSION_TYPES = Permission.Types;
    win.PERMISSIONS = new Permission();
    win.ROLE_REGISTRY = new Registry();
    win.RESOURCE_REGISTRY = new Registry();
    win.ACL = new Acl(win.PERMISSIONS, win.RESOURCE_REGISTRY, win.ROLE_REGISTRY);
}(window));

