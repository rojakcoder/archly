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
 * @param {string} entry - The entry to add.
 * @param {string} [parent] - The parent entry under which to
 * place the child entry.
 * @throws Will throw an error if the entry is already in the
 * registry or if the parent is not in the registry.
 */
Registry.prototype.add = function (entry, parent) {
  if (this.has(entry)) {
    throw new Error(DUPLICATE_ENTRIES.replace(/_entry_/g, entry));
  }
  if (parent) {
    if (!this.has(parent)) {
      throw new Error(ENTRY_NOT_FOUND.replace(/_entry_/g, parent));
    }
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
    var entry = loader.retrieveEntry(childId);
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
  var value, key, diff, i,
      len = 0,
      output = [];

  // Get the maximum length
  for (key in this.registry) {
    if (this.registry.hasOwnProperty(key)) {
      if (len < key.length) {
        len = key.length;
      }
    }
  }
  for (key in this.registry) {
    if (this.registry.hasOwnProperty(key)) {
      value = this.registry[key];
      output.push('\t');
      // Add the spaces in front
      diff = len - key.length;
      for (i = 0; i < diff; i++) {
          output.push(' ');
      }
      output.push(key);
      output.push(' - ');
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

module.exports = Registry;
