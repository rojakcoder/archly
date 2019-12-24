const Acl = require('./acl');
const perm = require('./permission');
const Registry = require('./registry');

function newAcl() {
  var roles = new Registry(),
      resources = new Registry(),
      permissions = new perm.Permission();

  return new Acl(permissions, resources, roles);
};

module.exports = {
  newAcl,
  Types: perm.Types,
};