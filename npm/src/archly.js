const Acl = require('./acl');
const { Permission, Types } = require('./permission');
const Registry = require('./registry');

function newAcl() {
  var roles = new Registry(),
      resources = new Registry(),
      permissions = new Permission();

  return new Acl(permissions, resources, roles);
};

module.exports = {
  newAcl,
  Types,
};