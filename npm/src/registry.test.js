const Registry = require('./registry');

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

test('Add/Remove Entry', () => {
  var reg = new Registry(),
      r1 = 'RES-1',
      r2 = 'RES-2',
      r = 'RES',
      removed;

  // Registry starts out empty.
  expect(reg.size()).toBe(0);

  reg.add(r1);
  expect(reg.size()).toBe(1);

  // Cannot have duplicate entries in registry.
  expect(() => {
    reg.add(r1)
  }).toThrow("Entry 'RES-1' is already in the registry.");

  reg.add(r2);
  expect(reg.size()).toBe(2);

  expect(() => {
    reg.remove(r, false)
  }).toThrow("Entry 'RES' is not in registry.");

  removed = reg.remove(r1);
  expect(reg.size()).toBe(1);
  expect(removed.length).toBe(1);
  expect(removed[0]).toBe(r1);
});

test('Add/Remove parents', () => {
  var reg = new Registry(),
      r1    = 'RES-1',
      r2    = 'RES-2',
      r1a   = 'RES-1-A',
      r1b   = 'RES-1-B',
      r2a   = 'RES-2-A',
      r2a1  = 'RES-2-A-1',
      r2a1i = 'RES-2-A-1-i',
      r1b1  = 'ReS-1-B-1',
      removed,
      thrown = false;

  expect(() => {
    reg.add(r1a, r1);
  }).toThrow("Entry 'RES-1' is not in registry."); // Parent not in registry.

  //add the parents
  reg.add(r1);
  reg.add(r2);
  //add the children
  reg.add(r1a, r1);
  reg.add(r1b, r1);
  expect(reg.size()).toBe(4);

  expect(() => {
    reg.add(r1b, r1);
  }).toThrow("Entry 'RES-1-B' is already in the registry.");

  reg.add(r2a, r2);
  reg.add(r1b1, r1b);
  expect(reg.size()).toBe(6);
  
  reg.add(r2a1, r2a);
  reg.add(r2a1i, r2a1);
  expect(reg.size()).toBe(8);
  
  //remove r2 and all descendants
  removed = reg.remove(r2, true);
  expect(reg.size()).toBe(4);
  // Removed r2, r2a, r2a1, r2a1i
  expect(removed.length).toBe(4);

  //remove 1rb and expect r1b1 to be under r1
  removed = reg.remove(r1b, false);
  expect(reg.size()).toBe(3);
  expect(reg.has(r1b1)).toBeTruthy();
  expect(reg.hasChild(r1)).toBeTruthy();
  expect(removed.length).toBe(1);
  //same element
  expect(removed[0]).toBe(r1b);

  //remove r1b1 and r1a and expect r1 to be childless
  reg.remove(r1b1, false);
  reg.remove(r1a, true);
  expect(reg.size()).toBe(1);
  expect(reg.hasChild(r1)).toBeFalsy();
});

test('Role Registry functions', () => {
  var reg = new Registry(),
      r1 = 'Rr1';

  expect(reg.has(r1)).toBeFalsy();
  reg.add(r1);
  expect(reg.has(r1)).toBeTruthy();
});

test("Traversal", () => {
  var path,
      r1 = 'ROLE-1',
      r2 = 'ROLE-2',
      r11 = 'ROLE-1-1',
      r12 = 'ROLE-1-2',
      r111 = 'ROLE-1-1-1',
      reg = new Registry();

  path = reg.traverseRoot(r1);
  // Registry starts empty.
  expect(path.length).toBe(1);

  reg.add(r1);
  path = reg.traverseRoot(r1);
  expect(path.length).toBe(2);

  reg.add(r2);
  path = reg.traverseRoot(r1);
  // Addition of siblig ROLE-2 has no effect.
  expect(path.length).toBe(2);

  reg.add(r11, r1);
  path = reg.traverseRoot(r1);
  // Length is still 2 because same traversal starting point.
  expect(path.length).toBe(2);
  path = reg.traverseRoot(r11);
  // Length is 3 because of addition of child.
  expect(path.length).toBe(3);

  reg.add(r12, r1);
  path = reg.traverseRoot(r1);
  // Length is still 2 because same traversal starting point.
  expect(path.length).toBe(2);
  path = reg.traverseRoot(r12);
  // Length is 3 because of addition of child.
  expect(path.length).toBe(3);

  reg.add(r111, r11);
  path = reg.traverseRoot(r111);
  // Length is 4 because of addition of grandchild.
  expect(path.length).toBe(4);

  //test the output
  expect(reg.display(new Role(r1))).toBeTruthy();
  expect(reg.toString()).toBeTruthy();
  expect(Registry.display(path)).toBeTruthy();
});
