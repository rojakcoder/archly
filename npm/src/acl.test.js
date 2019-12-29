const { Types } = require('./permission');
const archly = require('./archly');

var Resource = function (id) {
  this.id = id;
};
Resource.prototype.getEntryDescription = function () {
  return this.id;
};
Resource.prototype.getId = function () {
  return this.id;
};
Resource.prototype.retrieveEntry = function (id) {
  return new Resource(id);
};

var Role = function (id) {
  this.id = id;
};
Role.prototype.getEntryDescription = function () {
  return this.id;
};
Role.prototype.getId = function () {
  return this.id;
};
Role.prototype.retrieveEntry = function (id) {
  return new Role(id);
};

test('Resource Registry present', () => {
  var acl = archly.newAcl();
  expect(acl.resources).not.toBeNull();
});

test('Role Registry present', () => {
  var acl = archly.newAcl();
  expect(acl.roles).not.toBeNull();
});

test('Default', function () {
  var A = archly.newAcl(),
      rol1 = 'r1',
      res1 = 'r1';

  expect(A.isAllowed()).toBe(false);
  A.makeDefaultAllow();
  expect(A.isAllowed()).toBe(true);
  A.makeDefaultDeny();
  expect(A.isAllowed()).toBe(false);

  A.resources.add(res1);
  A.roles.add(rol1);
  expect(A.isAllowed(rol1, res1)).toBe(false); // Default false with explicit role and resource.
  expect(A.isAllowed(rol1, res1, Types.ALL)).toBe(false);
  expect(A.isAllowed(rol1, res1, Types.CREATE)).toBe(false);
  expect(A.isAllowed(rol1, res1, Types.READ)).toBe(false);
  expect(A.isAllowed(rol1, res1, Types.UPDATE)).toBe(false);
  expect(A.isAllowed(rol1, res1, Types.DELETE)).toBe(false);
  expect(A.isDenied(rol1, res1)).toBe(true);
  expect(A.isDenied(rol1, res1, Types.ALL)).toBe(true);
  expect(A.isDenied(rol1, res1, Types.CREATE)).toBe(true);
  expect(A.isDenied(rol1, res1, Types.READ)).toBe(true);
  expect(A.isDenied(rol1, res1, Types.UPDATE)).toBe(true);
  expect(A.isDenied(rol1, res1, Types.DELETE)).toBe(true);

  A.makeDefaultAllow();
  expect(A.isAllowed(rol1, res1)).toBe(true); // Default false with explicit role and resource.
  expect(A.isAllowed(rol1, res1, Types.ALL)).toBe(true);
  expect(A.isAllowed(rol1, res1, Types.CREATE)).toBe(true);
  expect(A.isAllowed(rol1, res1, Types.READ)).toBe(true);
  expect(A.isAllowed(rol1, res1, Types.UPDATE)).toBe(true);
  expect(A.isAllowed(rol1, res1, Types.DELETE)).toBe(true);
  expect(A.isDenied(rol1, res1)).toBe(false);
  expect(A.isDenied(rol1, res1, Types.ALL)).toBe(false);
  expect(A.isDenied(rol1, res1, Types.CREATE)).toBe(false);
  expect(A.isDenied(rol1, res1, Types.READ)).toBe(false);
  expect(A.isDenied(rol1, res1, Types.UPDATE)).toBe(false);
  expect(A.isDenied(rol1, res1, Types.DELETE)).toBe(false);
});

test('Test Resource', function () {
  var res1 = new Resource('ACO-1'),
      res1a = new Resource('ACO-1-A'),
      acl = archly.newAcl();

  acl.addResource(res1);
  expect(() => {
    acl.addResource(res1)
  }).toThrow(Error);

  acl.addResource(res1a, res1);
  expect(() => {
    acl.addResource(res1a);
  }).toThrow(Error);
  expect(() => {
    acl.addResource(res1a, res1);
  }).toThrow(Error);
});

test('Test Role', function () {
  var rol1 = new Role('ARO-1'),
      rol1a = new Role('ARO-1-A'),
      acl = archly.newAcl();

  acl.addRole(rol1);
  expect(() => {
    acl.addRole(rol1);
  }).toThrow(Error);
  acl.addRole(rol1a, rol1);
  expect(() => {
    acl.addRole(rol1a);
  }).toThrow(Error);
  expect(() => {
    acl.addRole(rol1a, rol1);
  }).toThrow(Error);
});

test('Test Allow', function () {
  var res0 = new Resource('ACO-0'),
      rol0 = new Role('ARO-0'),
      res1 = new Resource('ACO-1'),
      rol1 = new Role('ARO-1'),
      res2 = new Resource('ACO-2'),
      rol2 = new Role('ARO-2'),
      acl = archly.newAcl();

  expect(acl.isAllowed(rol1, res1)).toBe(false); // Permission not set.
  acl.allow(rol1, res1);
  expect(acl.isAllowed(rol1, res1)).toBe(true);

  expect(acl.isAllowed(rol0, res1)).toBe(false); // Permission not set.
  expect(acl.isAllowed(rol0, res2)).toBe(false);
  acl.allowAllResource(rol0);
  expect(acl.isAllowed(rol0, res1)).toBe(true); // Granted via all resource.
  expect(acl.isAllowed(rol0, res2)).toBe(true);
  expect(acl.isAllowed(rol1, res0)).toBe(false);
  expect(acl.isAllowed(rol2, res0)).toBe(false);
  acl.allowAllRole(res0);
  expect(acl.isAllowed(rol1, res0)).toBe(true);
  expect(acl.isAllowed(rol2, res0)).toBe(true);

  expect(acl.isAllowed(rol2, res2, Types.CREATE)); // Permission not set.
  expect(acl.isAllowed(rol2, res2, Types.READ));
  acl.allow(rol2, res2, Types.CREATE);
  expect(acl.isAllowed(rol2, res2, Types.CREATE)); // Role2 allowed CREATE access.
  expect(acl.isAllowed(rol2, res2, Types.READ));

  acl.allowAllResource(rol0); // Repeated addition of roles does not throw error.
  acl.allowAllRole(res0); // Repeated addition of resource does not throw error.
  acl.allow(rol2, res2, Types.CREATE); // Repeated addition of role and resource does not throw error.
});

test('Test Deny', function () {
  var res0 = new Resource('ACO-ZZ'),
      rol0 = new Role('ARO-ZZ'),
      res1 = new Resource('ACO-A'),
      rol1 = new Role('ARO-A'),
      res2 = new Resource('ACO-B'),
      rol2 = new Role('ARO-B'),
      acl = archly.newAcl();

  // Make default allow otherwise isDenied() will also return false
  acl.makeDefaultAllow();
  expect(acl.isDenied(rol1, res1)).toBe(false);
  acl.deny(rol1, res1);
  // Role A denied access to Res A.
  expect(acl.isDenied(rol1, res1)).toBe(true);
  expect(acl.isDenied(rol0, res1)).toBe(false); // Permission not set.
  expect(acl.isDenied(rol0, res2)).toBe(false);  

  acl.denyAllResource(rol0);
  expect(acl.isDenied(rol0, res1)).toBe(true); // Denied via all resource.
  expect(acl.isDenied(rol0, res2)).toBe(true);
  expect(acl.isDenied(rol1, res0)).toBe(false); // Permission not set.
  expect(acl.isDenied(rol2, res0)).toBe(false);

  acl.denyAllRole(res0);
  expect(acl.isDenied(rol1, res0)).toBe(true); // Denied via all role.
  expect(acl.isDenied(rol2, res0)).toBe(true);

  expect(acl.isDenied(rol2, res2, Types.CREATE)).toBe(false); // Permission not set.
  expect(acl.isDenied(rol2, res2, Types.READ)).toBe(false);

  acl.deny(rol2, res2, Types.CREATE);
  expect(acl.isDenied(rol2, res2, Types.CREATE)).toBe(true); // Role  2 denied CREATE access to Res 2
  expect(acl.isDenied(rol2, res2, Types.READ)).toBe(false); // Permission not set.
  
  expect(acl.denyAllResource(rol0)).toBeUndefined(); // Repeated addition of role does not throw error.
});

test('Test Remove', function () {
  var res1 = new Resource('reso-a'),
      rol1 = new Role('role-a'),
      res2 = new Resource('reso-b'),
      rol2 = new Role('role-b'),
      acl = archly.newAcl();

  // Set to default allow to test deny().
  acl.makeDefaultAllow();
  acl.deny(rol2, res2, Types.CREATE);

  expect(acl.isDenied(rol2, res2, Types.CREATE)).toBe(true); // Denied from above.
  expect(acl.isDenied(rol2, res2)).toBe(false); // Default is allow.

  // Permission entry is in the registry.
  expect(acl.permissions.has(rol2.getId() + '::' + res2.getId())).toBe(true);

  acl.remove(rol2, res2, Types.CREATE);
  expect(acl.isDenied(rol2, res2)).toBe(false); // No change.
  // Empty permission entry is removed from the registry.
  expect(acl.permissions.has(rol2.getId() + '::' + res2.getId())).toBe(false);

  acl.deny(rol1, res1);
  expect(acl.isDenied(rol1, res1, Types.CREATE)).toBe(true); // Denied set on ALL.
  expect(acl.isDenied(rol1, res1, Types.READ)).toBe(true);
  expect(acl.isDenied(rol1, res1, Types.UPDATE)).toBe(true);
  expect(acl.isDenied(rol1, res1, Types.DELETE)).toBe(true);
  expect(acl.isDenied(rol1, res1)).toBe(true); // Deny set on ALL.

  acl.remove(rol1, res1, Types.CREATE);
  expect(acl.isDenied(rol1, res1, Types.CREATE)).toBe(false); // CREATE DENY permission removed.
  expect(acl.isDenied(rol1, res1, Types.READ)).toBe(true);
  expect(acl.isDenied(rol1, res1, Types.UPDATE)).toBe(true);
  expect(acl.isDenied(rol1, res1, Types.DELETE)).toBe(true);
  expect(acl.isDenied(rol1, res1)).toBe(false); // CREATE is removed.

  acl.remove(rol1, res1);
  expect(acl.isDenied(rol1, res1, Types.CREATE)).toBe(false); // DENY permissions removed.
  expect(acl.isDenied(rol1, res1, Types.READ)).toBe(false);
  expect(acl.isDenied(rol1, res1, Types.UPDATE)).toBe(false);
  expect(acl.isDenied(rol1, res1, Types.DELETE)).toBe(false);
  expect(acl.isDenied(rol1, res1)).toBe(false);
});

test('Test Hierarchy', function () {
  var res1  = new Resource('RES1'),
      res2  = new Resource('RES2'),
      res3  = new Resource('RES3'),
      res4  = new Resource('RES4'),
      res1a = new Resource('RES1-a'),
      res1b = new Resource('RES1-b'),
      res1c = new Resource('RES1-c'),
      res1a1= new Resource('RES1-a-1'),
      res1a2= new Resource('RES1-a-2'),
      res1b1= new Resource('RES1-b-1'),
      res1c1= new Resource('RES1-c-1'),
      rol1  = new Role('R1'),
      rol2  = new Role('R2'),
      rol3  = new Role('R3'),
      rol4  = new Role('R4'),
      rol1a = new Role('R1-a'),
      rol1b = new Role('R1-b'),
      rol1c = new Role('R1-c'),
      rol1a1= new Role('R1-a-1'),
      rol1a2= new Role('R1-a-2'),
      rol1b1= new Role('R1-b-1'),
      acl = archly.newAcl();

  //add RES1, RES2, RES1-a
  acl.addResource(res1);
  acl.addResource(res2);
  acl.addResource(res1a, res1);
  acl.addResource(res1b, res1);
  acl.addResource(res1c, res1);
  acl.addResource(res1a1, res1a);
  acl.addResource(res1a2, res1a);
  acl.addResource(res1b1, res1b);
  acl.addResource(res1c1, res1c);

  //Add R1, R2, R1-a
  acl.addRole(rol1);
  acl.addRole(rol2);
  acl.addRole(rol1a, rol1);
  acl.addRole(rol1b, rol1);
  acl.addRole(rol1c, rol1);
  acl.addRole(rol1a1, rol1a);
  acl.addRole(rol1a2, rol1a);
  acl.addRole(rol1b1, rol1b);

  acl.makeDefaultDeny();

  // Grant ALL access to res1b, deny ALL to res1c.
  acl.allowAllRole(res1b);
  acl.denyAllRole(res1c);
  acl.allow(rol1, res1);

  //1-1
  expect(acl.isAllowed(rol1,  res1)).toBe(true);      // R1 allow RES1
  expect(acl.isAllowed(rol1,  res1a)).toBe(true);     // RES1-a child of RES1
  expect(acl.isAllowed(rol1,  res1a1)).toBe(true);    // RES1-a-1 descendent of RES1
  expect(acl.isAllowed(rol1,  res1a2)).toBe(true);    // RES1-a-2 descendent of RES1
  expect(acl.isAllowed(rol1,  res1b)).toBe(true);     // RES1-b child of RES1
  expect(acl.isAllowed(rol1,  res1b1)).toBe(true);    // RES1-b-1 descendant of RES1
  //R1 allowed to RES1 - overrides * deny RES1-c
  expect(acl.isAllowed(rol1,  res1c)).toBe(true);     // R1 allow to RES1 overrides * deny to RES1-c
  expect(acl.isAllowed(rol1,  res1c1)).toBe(true);    // R1 allow to RES1 overrides * deny to RES1-c
  //1-2
  expect(acl.isDenied(rol1,     res2)).toBe(true);    // Default deny
  //1-3
  expect(acl.isDenied(rol1,     res3)).toBe(true);    // Default deny
  //2-1
  expect(acl.isDenied(rol2,   res1)).toBe(true);      // Default deny
  expect(acl.isDenied(rol2,   res1a)).toBe(true);     // Default deny
  expect(acl.isDenied(rol2,   res1a1)).toBe(true);    // Default deny
  expect(acl.isDenied(rol2,   res1a2)).toBe(true);    // Default deny
  expect(acl.isAllowed(rol2,   res1b)).toBe(true);     // All access to RES1-b
  expect(acl.isAllowed(rol2,   res1b1)).toBe(true);    // RES1-b-1 child of RES1-b
  //2-2 - R2::RES2 should be CREATE-true
  acl.allow(rol2, res2, 'CREATE');
  expect(acl.isDenied(rol2,    res2)).toBe(false);  // To mirror the Java test
  //2-3
  expect(acl.isDenied(rol2,   res3)).toBe(true);      // Default deny
  //3-1
  expect(acl.isDenied(rol3,   res1)).toBe(true);      // Default deny
  expect(acl.isDenied(rol3,   res1a)).toBe(true);     // Default deny
  expect(acl.isDenied(rol3,   res1a1)).toBe(true);    // Default deny
  expect(acl.isDenied(rol3,   res1a2)).toBe(true);    // Default deny
  expect(acl.isAllowed(rol3,  res1b)).toBe(true);     // All access to RES1-b
  expect(acl.isAllowed(rol3,  res1b1)).toBe(true);    // RES1-b-1 child of RES1-b
  //3-2
  expect(acl.isDenied(rol3,   res2)).toBe(true);      // Default deny
  //3-3
  expect(acl.isDenied(rol3,   res3)).toBe(true);      // Default deny
  //4-1
  expect(acl.isDenied(rol4,   res1)).toBe(true);      // Default deny
  expect(acl.isDenied(rol4,   res1a)).toBe(true);     // Default deny
  expect(acl.isDenied(rol4,   res1a1)).toBe(true);    // Default deny
  expect(acl.isDenied(rol4,   res1a2)).toBe(true);    // Default deny
  expect(acl.isAllowed(rol4,  res1b)).toBe(true);     // All access to RES1-b
  expect(acl.isAllowed(rol4,  res1b1)).toBe(true);    // RES1-b-1 child of RES1-b
  expect(acl.isDenied(rol4,   res1c)).toBe(true);     // Default deny
  expect(acl.isDenied(rol4,   res1c1)).toBe(true);    // Default deny

  acl.deny(rol1, res1a);

  //1-1
  expect(acl.isAllowed(rol1,  res1)).toBe(true);      // R1 allow RES1
  expect(acl.isDenied(rol1,   res1a)).toBe(true);     // R1 deny RES1-a
  expect(acl.isDenied(rol1,   res1a1)).toBe(true);    // RES1-a-1 descendent of RES1
  expect(acl.isDenied(rol1,   res1a2)).toBe(true);    // RES1-a-2 descendent of RES1
  expect(acl.isAllowed(rol1,  res1b)).toBe(true);     // RES1-b child of RES1
  expect(acl.isAllowed(rol1,  res1b1)).toBe(true);    // RES1-b-1 descendant of RES1
  //R1 allowed to RES1 - overrides * deny RES1-c
  expect(acl.isAllowed(rol1,  res1c)).toBe(true);     // R1 allow to RES1 overrides * deny to RES1-c
  expect(acl.isAllowed(rol1,  res1c1)).toBe(true);    // R1 allow to RES1 overrides * deny to RES1-c
  //1-2
  expect(acl.isDenied(rol1,     res2)).toBe(true); // Default deny
  //1-3
  expect(acl.isDenied(rol1,     res3)).toBe(true); // Default deny
  //2-1
  expect(acl.isDenied(rol2,   res1)).toBe(true);      // Default deny
  expect(acl.isDenied(rol2,   res1a)).toBe(true);     // Default deny
  expect(acl.isDenied(rol2,   res1a1)).toBe(true);    // Default deny
  expect(acl.isDenied(rol2,   res1a2)).toBe(true);    // Default deny
  expect(acl.isAllowed(rol2,  res1b)).toBe(true);     // All access to RES1-b
  expect(acl.isAllowed(rol2,  res1b1)).toBe(true);    // RES1-b-1 child of RES1-b
  //2-2 - R2::RES2 should be CREATE-true from above
  expect(acl.isDenied(rol2,    res2)).toBe(false);  // To mirror the Java test
  //2-3
  expect(acl.isDenied(rol2,   res3)).toBe(true);      // Default deny
  //3-1
  expect(acl.isDenied(rol3,   res1)).toBe(true);      // Default deny
  expect(acl.isDenied(rol3,   res1a)).toBe(true);     // Default deny
  expect(acl.isDenied(rol3,   res1a1)).toBe(true);    // Default deny
  expect(acl.isDenied(rol3,   res1a2)).toBe(true);    // Default deny
  expect(acl.isAllowed(rol3,  res1b)).toBe(true);     // All access to RES1-b
  expect(acl.isAllowed(rol3,  res1b1)).toBe(true);    // RES1-b-1 child of RES1-b
  //3-2
  expect(acl.isDenied(rol3,   res2)).toBe(true);      // Default deny
  //3-3
  expect(acl.isDenied(rol3,   res3)).toBe(true);      // Default deny

  acl.allow(rol1, res1a1);

  //1-1
  expect(acl.isAllowed(rol1,  res1)).toBe(true);      // R1 allow RES1
  expect(acl.isDenied(rol1,   res1a)).toBe(true);     // R1 deny RES1-a
  expect(acl.isAllowed(rol1,  res1a1)).toBe(true);    // R1 allow RES1-a-1
  expect(acl.isDenied(rol1,   res1a2)).toBe(true);    // RES1-a-2 descendent of RES1-a
  expect(acl.isAllowed(rol1,  res1b)).toBe(true);     // RES1-b child of RES1
  expect(acl.isAllowed(rol1,  res1b1)).toBe(true);    // RES1-b-1 descendant of RES1
  //R1 allowed to RES1 - overrides * deny RES1-c
  expect(acl.isAllowed(rol1,  res1c)).toBe(true);     // R1 allow to RES1 overrides * deny to RES1-c
  expect(acl.isAllowed(rol1,  res1c1)).toBe(true);    // R1 allow to RES1 overrides * deny to RES1-c
  //1-2
  expect(acl.isDenied(rol1,   res2)).toBe(true);      // Default deny
  //1-3
  expect(acl.isDenied(rol1,   res3)).toBe(true); // Default deny
  //2-1
  expect(acl.isDenied(rol2,   res1)).toBe(true);      // Default deny
  expect(acl.isDenied(rol2,   res1a)).toBe(true);     // Default deny
  expect(acl.isDenied(rol2,   res1a1)).toBe(true);    // Default deny
  expect(acl.isDenied(rol2,   res1a2)).toBe(true);    // Default deny
  expect(acl.isAllowed(rol2,  res1b)).toBe(true);     // All access to RES1-b
  expect(acl.isAllowed(rol2,  res1b1)).toBe(true);    // RES1-b-1 child of RES1-b
  //2-2 - R2::RES2 should be CREATE-true from above
  expect(acl.isDenied(rol2,res2)).toBe(false);  // To mirror the Java test
  //2-3
  expect(acl.isDenied(rol2,   res3)).toBe(true);      // Default deny
  //3-1
  expect(acl.isDenied(rol3,   res1)).toBe(true);      // Default deny
  expect(acl.isDenied(rol3,   res1a)).toBe(true);     // Default deny
  expect(acl.isDenied(rol3,   res1a1)).toBe(true);    // Default deny
  expect(acl.isDenied(rol3,   res1a2)).toBe(true);    // Default deny
  expect(acl.isAllowed(rol3,  res1b)).toBe(true);     // All access to RES1-b
  expect(acl.isAllowed(rol3,  res1b1)).toBe(true);    // RES1-b-1 child of RES1-b
  //3-2
  expect(acl.isDenied(rol3,   res2)).toBe(true);      // Default deny
  //3-3
  expect(acl.isDenied(rol3,   res3)).toBe(true);      // Default deny

  //deny R2 to RES1-b; test overriding of ALL allow
  acl.deny(rol2, res1b);

  //1-1
  expect(acl.isAllowed(rol1,  res1)).toBe(true);      // R1 allow RES1
  expect(acl.isDenied(rol1,   res1a)).toBe(true);     // R1 deny RES1-a
  expect(acl.isAllowed(rol1,  res1a1)).toBe(true);    // R1 allow RES1-a-1
  expect(acl.isDenied(rol1,   res1a2)).toBe(true);    // RES1-a-2 descendent of RES1-a
  expect(acl.isAllowed(rol1,  res1b)).toBe(true);     // RES1-b child of RES1
  expect(acl.isAllowed(rol1,  res1b1)).toBe(true);    // RES1-b-1 descendant of RES1
  //R1 allowed to RES1 - overrides * deny RES1-c
  expect(acl.isAllowed(rol1,  res1c)).toBe(true);     // R1 allow to RES1 overrides * deny to RES1-c
  expect(acl.isAllowed(rol1,  res1c1)).toBe(true);    // R1 allow to RES1 overrides * deny to RES1-c
  //1-2
  expect(acl.isDenied(rol1,   res2)).toBe(true);      // Default deny
  //1-3
  expect(acl.isDenied(rol1,   res3)).toBe(true);      // Default deny
  //2-1
  expect(acl.isDenied(rol2,   res1)).toBe(true);      // Default deny
  expect(acl.isDenied(rol2,   res1a)).toBe(true);     // Default deny
  expect(acl.isDenied(rol2,   res1a1)).toBe(true);    // Default deny
  expect(acl.isDenied(rol2,   res1a2)).toBe(true);    // Default deny
  expect(acl.isDenied(rol2,   res1b)).toBe(true);     // R2 deny RES1-bb
  expect(acl.isDenied(rol2,   res1b1)).toBe(true);    // RES1-b-1 child of RES1-b
  //2-2 - R2::RES2 should be CREATE-true from above
  expect(acl.isDenied(rol2,    res2)).toBe(false);  // To mirror the Java test
  //2-3
  expect(acl.isDenied(rol2,   res3)).toBe(true);      // Default deny
  //3-1
  expect(acl.isDenied(rol3,   res1)).toBe(true);      // Default deny
  expect(acl.isDenied(rol3,   res1a)).toBe(true);     // Default deny
  expect(acl.isDenied(rol3,   res1a1)).toBe(true);    // Default deny
  expect(acl.isDenied(rol3,   res1a2)).toBe(true);    // Default deny
  expect(acl.isAllowed(rol3,  res1b)).toBe(true);     // All access to RES1-b
  expect(acl.isAllowed(rol3,  res1b1)).toBe(true);    // RES1-b-1 child of RES1-b
  //3-2
  expect(acl.isDenied(rol3,   res2)).toBe(true);      // Default deny
  //3-3
  expect(acl.isDenied(rol3,   res3)).toBe(true);      // Default deny

  //deny R3 o RES1-b-1; test specific deny over ALL allow
  acl.deny(rol3, res1b1);

  //1-1
  expect(acl.isAllowed(rol1,  res1)).toBe(true);      // R1 allow RES1
  expect(acl.isDenied(rol1,   res1a)).toBe(true);     // R1 deny RES1-a
  expect(acl.isAllowed(rol1,  res1a1)).toBe(true);    // R1 allow RES1-a-1
  expect(acl.isDenied(rol1,   res1a2)).toBe(true);    // RES1-a-2 descendent of RES1-a
  expect(acl.isAllowed(rol1,  res1b)).toBe(true);     // RES1-b child of RES1
  expect(acl.isAllowed(rol1,  res1b1)).toBe(true);    // RES1-b-1 descendant of RES1
  //R1 allowed to RES1 - overrides * deny RES1-c
  expect(acl.isAllowed(rol1,  res1c)).toBe(true);     // R1 allow to RES1 overrides * deny to RES1-c
  expect(acl.isAllowed(rol1,  res1c1)).toBe(true);    // R1 allow to RES1 overrides * deny to RES1-c
  //1-2
  expect(acl.isDenied(rol1,   res2)).toBe(true);      // Default deny
  //1-3
  expect(acl.isDenied(rol1,   res3)).toBe(true);      // Default deny
  //2-1
  expect(acl.isDenied(rol2,   res1)).toBe(true);      // Default deny
  expect(acl.isDenied(rol2,   res1a)).toBe(true);     // Default deny
  expect(acl.isDenied(rol2,   res1a1)).toBe(true);    // Default deny
  expect(acl.isDenied(rol2,   res1a2)).toBe(true);    // Default deny
  expect(acl.isDenied(rol2,   res1b)).toBe(true);     // R2 deny RES1-bb
  expect(acl.isDenied(rol2,   res1b1)).toBe(true);    // RES1-b-1 child of RES1-b
  //2-2 - R2::RES2 should be CREATE-true from above
  expect(acl.isDenied(rol2,    res2)).toBe(false);  // To mirror the Java test
  //2-3
  expect(acl.isDenied(rol2,   res3)).toBe(true);      // Default deny
  //3-1
  expect(acl.isDenied(rol3,   res1)).toBe(true);      // Default deny
  expect(acl.isDenied(rol3,   res1a)).toBe(true);     // Default deny
  expect(acl.isDenied(rol3,   res1a1)).toBe(true);    // Default deny
  expect(acl.isDenied(rol3,   res1a2)).toBe(true);    // Default deny
  expect(acl.isAllowed(rol3,  res1b)).toBe(true);     // All access to RES1-b
  expect(acl.isDenied(rol3,   res1b1)).toBe(true);    // RES1-b-1 child of RES1-b
  //3-2
  expect(acl.isDenied(rol3,   res2)).toBe(true);      // Default deny
  //3-3
  expect(acl.isDenied(rol3,   res3)).toBe(true);      // Default deny

  //test coverage
  acl.allow(rol4, res1c);
  //4-1
  expect(acl.isDenied(rol4,   res1)).toBe(true);      // Default deny
  expect(acl.isDenied(rol4,   res1a)).toBe(true);     // Default deny
  expect(acl.isDenied(rol4,   res1a1)).toBe(true);    // Default deny
  expect(acl.isDenied(rol4,   res1a2)).toBe(true);    // Default deny
  expect(acl.isAllowed(rol4,  res1b)).toBe(true);     // All access to RES1-b
  expect(acl.isAllowed(rol4,  res1b1)).toBe(true);    // All access to RES1-b
  expect(acl.isAllowed(rol4,  res1c)).toBe(true);     // R4 allow to RES1-c
  expect(acl.isAllowed(rol4,  res1c1)).toBe(true);    // RES1-c-1 child of RES1-c
  expect(acl.isDenied(rol4,   res4)).toBe(true);      // Default deny

  acl.allow(rol4, res4);
  expect(acl.isAllowed(rol4,  res4)).toBe(true);      // Default deny
});

test('Test Remove NULL', function () {
  var rolna = new Role('NA-ROLE'),
      resna = new Resource('NA-RES'),
      nullrol = null,
      nullres = null,
      acl = archly.newAcl();

  expect(acl.isAllowed(rolna, resna)).toBe(false); // Default deny
  expect(acl.isDenied(rolna, resna)).toBe(true); // Default deny

  acl.remove(nullrol, nullres);

  expect(acl.isAllowed(rolna, resna)).toBe(false); // False because the root is removed
  expect(acl.isDenied(rolna, resna)).toBe(false);  // False because the root is removed

  expect(() => {
    acl.remove(nullrol, nullres, Types.CREATE);
  }).toThrow("Permission '*::*' not found on '*' for '*'.");
});

test('Test Remove Resource/Role', function () {
  var resources = ['C1', 'C2', 'C3', 'C4'],
      roles = ['R1', 'R2', 'R3', 'R4'],
      A = archly.newAcl(),
      P = A.permissions,
      RESR = A.resources,
      ROLR = A.roles;

  //create mappings for each key pair
  for (var i = 0; i < resources.length; i++) {
    for (var j = 0; j < roles.length; j++) {
      A.allow(new Role(roles[j]), new Resource(resources[i]));
      expect(A.isAllowed(new Role(roles[j]), new Resource(resources[i]))).toBe(true);
    }
  }

  //add children to both resources and roles
  for (var i = 1; i <= 4; i++) {
    A.addResource(new Resource('CC' + i), new Resource('C' + i));
    A.addRole(new Role('RC' + i), new Role('R' + i));
  }

  //assign permission for child1 to child2
  A.allow(new Role('RC1'), new Resource('CC2'));
  A.allow(new Role('RC2'), new Resource('CC1'));

  //add ALL access
  for (var i = 0; i < resources.length; i++) {
    A.allowAllRole(new Resource(resources[i]));
    expect(A.isAllowed(new Role('*'), new Resource(resources[i])));
  }
  for (var i = 0; i < roles.length; i++) {
    A.allowAllResource(new Role(roles[i]));
    expect(A.isAllowed(new Role(roles[i]), new Resource('*')));
  }

  //4x4 pairs, 2x4 ALL access, 1 child each
  expect(P.size()).toBe(27); // 1 + 4x4 + 4 + 4 + 1 + 1
  expect(RESR.size()).toBe(8); // 4+4; 4 resources each with a child
  expect(ROLR.size()).toBe(8); // 4+4; 4 roles each with a child

  //remove all access on C4
  A.removeResource(new Resource('C4'), false);
  expect(P.size()).toBe(22); // 1 + 4x3 + 4 + 3 + 1 + 1; less C4 but not its child
  expect(RESR.size()).toBe(7); // 3+4; less C4 but not its child
  expect(ROLR.size()).toBe(8); // 4+4

  //remove all access on R4
  A.removeRole(new Role('R4'), false);
  expect(P.size()).toBe(18); // 1 + 3x3 + 3 + 3 + 1 + 1; less R4 but not its child
  expect(RESR.size()).toBe(7); // 3+4
  expect(ROLR.size()).toBe(7); // 3+4; less R4 but not its child

  //remove all access on C3 and child
  A.removeResource(new Resource('C3'), true);
  expect(P.size()).toBe(14); // 1 + 3x2 + 3 + 2 + 1 + 1; less C3 and child
  expect(RESR.size()).toBe(5); // 2+3; less C3 and child
  expect(ROLR.size()).toBe(7); // 3+4

  //remove all access on R3 and child
  A.removeRole(new Role('R3'), true);
  expect(P.size()).toBe(11); // 1 + 2x2 + 2 + 2 + 1 + 1; less R3 and child
  expect(RESR.size()).toBe(5); // 2+3
  expect(ROLR.size()).toBe(5); // 2+3; less R3 and child

  //remove all access on C2 and child
  A.removeResource(new Resource('C2'), true);
  expect(P.size()).toBe(7); // 1 + 2x1 + 2 + 1 + 1 + 0; less child permission
  expect(RESR.size()).toBe(3); // 1+2; less C2 and child
  expect(ROLR.size()).toBe(5); // 2+3

  //remove all access on R2 and child
  A.removeRole(new Role('R2'), true);
  expect(P.size()).toBe(4); // 1 + 1x1 + 1 + 1 + 0 + 0; less child permission
  expect(RESR.size()).toBe(3); // 1+2
  expect(ROLR.size()).toBe(3); // 1+2; less R2 and child

  //remove all access on C1 and child
  A.removeResource('C1', true);
  expect(P.size()).toBe(2); // 1 + 1x0 + 1 + 0 + 0 + 0; less C1 and child
  expect(RESR.size()).toBe(1); // 0+1; less C1 and child
  expect(ROLR.size()).toBe(3); // 1+2

  //remove all access on R1 and child
  A.removeRole('R1', true);
  expect(P.size()).toBe(1); // 1 + 0x0 + 0 + 0 + 0 + 0; less R1 and child
  expect(RESR.size()).toBe(1); // 0+1
  expect(ROLR.size()).toBe(1); // 0+1; less R1 and child

  //test coverage
  expect(() => {
    A.removeResource(null, false);
  }).toThrow(Error);

  expect(() => {
    A.removeRole(null, false);
  }).toThrow(Error);
});

test('Test Export/Import', function () {
  //test export
  var A = archly.newAcl();
  var resources = A.exportResources();
  var roles = A.exportRoles();
  var perms = A.exportPermissions();

  //verify that the exported data are snapshots
  A.addResource('laser-gun');
  A.addRole('jedi');
  A.deny('jedi', 'laser-gun');

  var newRes = A.exportResources();
  var newRoles = A.exportRoles();
  var newPerms = A.exportPermissions();

  expect(Object.keys(newRes).length > Object.keys(resources).length).toBe(true); // Exported resources is unaffected
  expect(Object.keys(newRoles).length > Object.keys(roles).length).toBe(true); // Exported roles is unaffected
  expect(Object.keys(newPerms).length > Object.keys(perms).length).toBe(true); // Exported permissions is unaffected

  //simulate saved data
  newRes = {
    'laser-gun': '',
    'light-sabre': '',
    'staff': 'light-sabre',
    't': 'light-sabre'
  };
  newRoles = {
    'jedi': '',
    'sith': '',
    'obiwan': 'jedi',
    'luke': 'jedi',
    'darth-vader': 'sith',
    'darth-maul': 'sith'
  };
  newPerms = {
    '*::*': { ALL: false },
    'jedi::light-sabre':    { ALL: true },
    'jedi::laser-gun':      { ALL: false },
    'sith::light-sabre':    { ALL: true },
    'sith::laser-gun':      { ALL: true },
    'luke::laser-gun':      { ALL: true },
    'jedi::t':              { ALL: false }
  };

  //test import
  expect(() => {
    A.importResources(newRes);
  }).toThrow(Error); // Resources are non-empty.
  expect(() => {
    A.importRoles(newRoles);
  }).toThrow(Error); // Roles are non-empty.
  expect(() => {
    A.importPermissions(newPerms);
  }).toThrow(Error); // Permissions are non-empty.

  A.clear();
  A.importResources(newRes);
  A.importRoles(newRoles);
  A.importPermissions(newPerms);
  expect(A.visualize()).toBeTruthy();
  expect(A.visualizePermissions()).toBeTruthy();
  expect(A.visualizeResources(new Role())).toBeTruthy();
  expect(A.visualizeRoles(new Role())).toBeTruthy();

  //verify that the permissions are correct
  expect(A.isAllowed('jedi',        'light-sabre')).toBe(true);
  expect(A.isDenied('jedi',         'laser-gun')).toBe(true);
  expect(A.isAllowed('luke',        'laser-gun')).toBe(true);
  expect(A.isAllowed('sith',        'laser-gun')).toBe(true);
  expect(A.isDenied('jedi',         't')).toBe(true);

  //change permission
  A.deny('sith', 'laser-gun');
  //add resource, role and permission
  A.addResource('double', 'light-sabre');
  A.addRole('anakin', 'jedi');
  A.allow('jedi', 'double', 'UPDATE');

  //verify permissions are correct
  expect(A.isAllowed('jedi', 'double')).toBe(true); // True because UPDATE on double is redundant.
  expect(A.isAllowed('jedi', 'double', 'UPDATE')).toBe(true); // True because UPDATE on double is redundant.

  //change double to be not redundant
  A.deny('jedi', 'double', 'CREATE');

  expect(A.isAllowed('jedi', 'double')).toBe(false); // False because jedi::double DENY CREATE.
  expect(A.isAllowed('jedi', 'double', 'UPDATE')).toBe(true); // Still true.
  expect(A.isDenied('luke', 'double', 'CREATE')).toBe(true); // True because inherits jedi.
  expect(A.isDenied('sith', 'laser-gun')).toBe(true);
  expect(A.isDenied('jedi', 't')).toBe(true);

  resources = A.exportResources();
  roles = A.exportRoles();
  perms = A.exportPermissions();

  //verify that the maps are disconnected
  expect(Object.keys(newRes).length < Object.keys(resources).length); // Exported resources is unaffected
  expect(Object.keys(newRoles).length < Object.keys(roles).length); // Exported roles is unaffected
  expect(Object.keys(newPerms).length < Object.keys(perms).length); // Exported permissions is unaffected
});
