package com.rojakcoder.archly;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.rojakcoder.archly.exceptions.DuplicateEntryException;
import com.rojakcoder.archly.exceptions.EntryNotFoundException;
import com.rojakcoder.archly.exceptions.NonEmptyException;

public class AclTest {
	static Acl acl = Acl.makeInstance();

	@Test(priority = 4)
	public void testRunner() {
		Assert.assertFalse(acl.isAllowed(null, null)); //default false
		acl.makeDefaultAllow();
		Assert.assertTrue(acl.isAllowed(null, null));

		//restore to default false
		acl.makeDefaultDeny();
		Assert.assertFalse(acl.isAllowed(null, null));

		testResource();
		testRole();
		testAllow();
		testDeny();
		testRemove();
		testHierarchy();
		testRemoveNull();
		testRemoveResourceRole();
	}

	@Test(priority = 49)
	public void testCoverage() {
		AclEntry root = new RootEntry();
		AclEntry nulle = null;
		//test coverage
		Assert.assertEquals(root.getEntryDescription(), "ROOT");
		Assert.assertNull(root.retrieveEntry(null));

		Permission p = new Permission();
		p.permissions.remove("*::*");
		Assert.assertFalse(acl.isAllowed(nulle, nulle));
		Assert.assertFalse(acl.isAllowed(nulle, nulle, "ALL"));
		Assert.assertFalse(acl.isDenied(nulle, nulle));
		Assert.assertFalse(acl.isDenied(nulle, nulle, "ALL"));
	}

	private void testResource() {
		Res res1 = new Res("ACO-1");
		Res res1a = new Res("ACO-1-A");
		boolean thrown = false;

		acl.addResource(res1);
		try {
			acl.addResource(res1);
		} catch (DuplicateEntryException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);

		acl.addResource(res1a, res1);
		thrown = false;
		try {
			acl.addResource(res1a);
		} catch (DuplicateEntryException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);
		thrown = false;
		try {
			acl.addResource(res1a, res1);
		} catch (DuplicateEntryException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);
	}

	private void testRole() {
		Rol rol1 = new Rol("ARO-1");
		Rol rol1a = new Rol("ARO-1-A");
		boolean thrown = false;

		acl.addRole(rol1);
		try {
			acl.addRole(rol1);
		} catch (DuplicateEntryException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);

		acl.addRole(rol1a, rol1);
		thrown = false;
		try {
			acl.addRole(rol1a);
		} catch (DuplicateEntryException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);
		thrown = false;
		try {
			acl.addRole(rol1a, rol1);
		} catch (DuplicateEntryException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);
	}

	private void testAllow() {
		Res res0 = new Res("ACO-0");
		Rol rol0 = new Rol("ARO-0");
		Res res1 = new Res("ACO-1");
		Rol rol1 = new Rol("ARO-1");
		Res res2 = new Res("ACO-2");
		Rol rol2 = new Rol("ARO-2");

		//grant role1 to res1
		Assert.assertFalse(acl.isAllowed(rol1, res1));
		acl.allow(rol1, res1);
		Assert.assertTrue(acl.isAllowed(rol1, res1));

		//test all access grant
		Assert.assertFalse(acl.isAllowed(rol0, res1));
		Assert.assertFalse(acl.isAllowed(rol0, res2));
		acl.allowAllResource(rol0);
		Assert.assertTrue(acl.isAllowed(rol0, res1));
		Assert.assertTrue(acl.isAllowed(rol0, res2));
		Assert.assertFalse(acl.isAllowed(rol1, res0));
		Assert.assertFalse(acl.isAllowed(rol2, res0));
		acl.allowAllRole(res0);
		Assert.assertTrue(acl.isAllowed(rol1, res0));
		Assert.assertTrue(acl.isAllowed(rol2, res0));

		//test specific grant
		Assert.assertFalse(acl.isAllowed(rol2, res2, "CREATE"));
		Assert.assertFalse(acl.isAllowed(rol2, res2, "READ"));
		acl.allow(rol2, res2, "CREATE");
		Assert.assertTrue(acl.isAllowed(rol2, res2, "CREATE"));
		Assert.assertFalse(acl.isAllowed(rol2, res2, "READ"));

		//test coverage - exceptions are not re-thrown
		acl.allowAllResource(rol0);
		acl.allowAllRole(res0);
		acl.allow(rol2, res2, "CREATE");
	}

	private void testDeny() {
		Res res0 = new Res("ACO-ZZ");
		Rol rol0 = new Rol("ARO-ZZ");
		Res res1 = new Res("ACO-A");
		Rol rol1 = new Rol("ARO-A");
		Res res2 = new Res("ACO-B");
		Rol rol2 = new Rol("ARO-B");

		//make default allow otherwise isDenied will also return false if not present
		acl.makeDefaultAllow();

		//deny role1 to res1
		Assert.assertFalse(acl.isDenied(rol1, res1));
		acl.deny(rol1, res1);
		Assert.assertTrue(acl.isDenied(rol1, res1));

		//test all access deny
		Assert.assertFalse(acl.isDenied(rol0, res1));
		Assert.assertFalse(acl.isDenied(rol0, res2));
		acl.denyAllResource(rol0);
		Assert.assertTrue(acl.isDenied(rol0, res1));
		Assert.assertTrue(acl.isDenied(rol0, res2));
		Assert.assertFalse(acl.isDenied(rol1, res0));
		Assert.assertFalse(acl.isDenied(rol2, res0));
		acl.denyAllRole(res0);
		Assert.assertTrue(acl.isDenied(rol1, res0));
		Assert.assertTrue(acl.isDenied(rol2, res0));

		//test specific deny
		Assert.assertFalse(acl.isDenied(rol2, res2, "CREATE"));
		Assert.assertFalse(acl.isDenied(rol2, res2, "READ"));
		acl.deny(rol2, res2, "CREATE");
		Assert.assertTrue(acl.isDenied(rol2, res2, "CREATE"));
		Assert.assertFalse(acl.isDenied(rol2, res2, "READ"));

		//test coverage - exceptions are not re-thrown
		acl.denyAllResource(rol0);

	}

	private void testRemove() {
		Res res1 = new Res("ACO-A");
		Rol rol1 = new Rol("ARO-A");
		Res res2 = new Res("ACO-B");
		Rol rol2 = new Rol("ARO-B");

		//set to default allow to test deny()
		acl.makeDefaultAllow();

		Assert.assertTrue(acl.isDenied(rol2, res2, "CREATE"));
		Assert.assertFalse(acl.isDenied(rol2, res2));
		acl.remove(rol2, res2, "CREATE");
		Assert.assertFalse(acl.isDenied(rol2, res2));
		Assert.assertFalse((new Permission()).permissions.containsKey(rol2
				.getId() + "::" + res2.getId()));

		Assert.assertTrue(acl.isDenied(rol1, res1, "CREATE"));
		Assert.assertTrue(acl.isDenied(rol1, res1, "READ"));
		Assert.assertTrue(acl.isDenied(rol1, res1, "UPDATE"));
		Assert.assertTrue(acl.isDenied(rol1, res1, "DELETE"));
		Assert.assertTrue(acl.isDenied(rol1, res1));
		acl.remove(rol1, res1, "CREATE");
		Assert.assertFalse(acl.isDenied(rol1, res1, "CREATE"));
		Assert.assertTrue(acl.isDenied(rol1, res1, "READ"));
		Assert.assertTrue(acl.isDenied(rol1, res1, "UPDATE"));
		Assert.assertTrue(acl.isDenied(rol1, res1, "DELETE"));
		Assert.assertFalse(acl.isDenied(rol1, res1));
		acl.remove(rol1, res1);
		Assert.assertFalse(acl.isDenied(rol1, res1, "CREATE"));
		Assert.assertFalse(acl.isDenied(rol1, res1, "READ"));
		Assert.assertFalse(acl.isDenied(rol1, res1, "UPDATE"));
		Assert.assertFalse(acl.isDenied(rol1, res1, "DELETE"));
		Assert.assertFalse(acl.isDenied(rol1, res1));
	}

	private void testHierarchy() {
		Res res1 = new Res("ACO-1");
		Res res2 = new Res("ACO-2");
		Res res3 = new Res("ACO-3");
		Res res4 = new Res("ACO-4");
		Res res1a = new Res("ACO-1-A");
		Res res1b = new Res("ACO-1-B");
		Res res1c = new Res("ACO-1-C");
		Res res1a1 = new Res("ACO-1-A-1");
		Res res1a2 = new Res("ACO-1-A-2");
		Res res1b1 = new Res("ACO-1-B-1");
		Res res1c1 = new Res("ACO-1-C-1");

		Rol rol1 = new Rol("ARO-1");
		Rol rol2 = new Rol("ARO-2");
		Rol rol3 = new Rol("ARO-3");
		Rol rol4 = new Rol("ARO-4");
		Rol rol1a = new Rol("ARO-1-A");
		Rol rol1b = new Rol("ARO-1-B");
		Rol rol1c = new Rol("ARO-1-C");
		Rol rol1a1 = new Rol("ARO-1-A-1");
		Rol rol1a2 = new Rol("ARO-1-A-2");
		Rol rol1b1 = new Rol("ARO-1-B-1");

		//ACO-1, ACO-2, ACO-1-A are already present
		acl.addResource(res1b, res1);
		acl.addResource(res1c, res1);
		acl.addResource(res1a1, res1a);
		acl.addResource(res1a2, res1a);
		acl.addResource(res1b1, res1b);
		acl.addResource(res1c1, res1c);
		//ARO-1, ARO-2, ARO-1-A are already present
		acl.addRole(rol1b, rol1);
		acl.addRole(rol1c, rol1);
		acl.addRole(rol1a1, rol1a);
		acl.addRole(rol1a2, rol1a);
		acl.addRole(rol1b1, rol1b);

		acl.makeDefaultDeny();
		//grant ALL access to ACO-1-B
		acl.allowAllRole(res1b);
		acl.denyAllRole(res1c);

		acl.allow(rol1, res1);

		//1-1
		Assert.assertTrue(acl.isAllowed(rol1, res1));
		Assert.assertTrue(acl.isAllowed(rol1, res1a));
		Assert.assertTrue(acl.isAllowed(rol1, res1a1));
		Assert.assertTrue(acl.isAllowed(rol1, res1a2));
		Assert.assertTrue(acl.isAllowed(rol1, res1b));
		Assert.assertTrue(acl.isAllowed(rol1, res1b1));
		// rol1 allowed to res1 - overrides * deny res1c
		Assert.assertFalse(acl.isDenied(rol1, res1c));
		Assert.assertTrue(acl.isAllowed(rol1, res1c));
		Assert.assertFalse(acl.isDenied(rol1, res1c1));
		Assert.assertTrue(acl.isAllowed(rol1, res1c1));
		//1-2
		Assert.assertFalse(acl.isAllowed(rol1, res2));
		Assert.assertFalse(acl.isAllowed(rol1, res2));
		//1-3
		Assert.assertTrue(acl.isDenied(rol1, res3));
		Assert.assertFalse(acl.isAllowed(rol1, res3));
		//2-1
		Assert.assertTrue(acl.isDenied(rol2, res1));
		Assert.assertTrue(acl.isDenied(rol2, res1a));
		Assert.assertTrue(acl.isDenied(rol2, res1a1));
		Assert.assertTrue(acl.isDenied(rol2, res1a2));
		Assert.assertTrue(acl.isAllowed(rol2, res1b));
		Assert.assertTrue(acl.isAllowed(rol2, res1b1));
		//2-2
		//false because ARO-2::ACO-2 was added with CREATE:true before
		Assert.assertFalse(acl.isDenied(rol2, res2));
		//2-3
		Assert.assertTrue(acl.isDenied(rol2, res3));
		//3-1
		Assert.assertTrue(acl.isDenied(rol3, res1));
		Assert.assertTrue(acl.isDenied(rol3, res1a));
		Assert.assertTrue(acl.isDenied(rol3, res1a1));
		Assert.assertTrue(acl.isDenied(rol3, res1a2));
		Assert.assertTrue(acl.isAllowed(rol3, res1b));
		Assert.assertTrue(acl.isAllowed(rol3, res1b1));
		//3-2
		Assert.assertTrue(acl.isDenied(rol3, res2));
		//3-3
		Assert.assertTrue(acl.isDenied(rol3, res3));
		//4-1
		Assert.assertTrue(acl.isDenied(rol4, res1));
		Assert.assertTrue(acl.isDenied(rol4, res1a));
		Assert.assertTrue(acl.isDenied(rol4, res1a1));
		Assert.assertTrue(acl.isDenied(rol4, res1a2));
		Assert.assertTrue(acl.isAllowed(rol4, res1b));
		Assert.assertTrue(acl.isAllowed(rol4, res1b1));
		Assert.assertTrue(acl.isDenied(rol4, res1c));
		Assert.assertTrue(acl.isDenied(rol4, res1c1));

		acl.deny(rol1, res1a);
		//1-1
		Assert.assertTrue(acl.isAllowed(rol1, res1));
		Assert.assertTrue(acl.isDenied(rol1, res1a));
		Assert.assertTrue(acl.isDenied(rol1, res1a1));
		Assert.assertTrue(acl.isDenied(rol1, res1a2));
		Assert.assertTrue(acl.isAllowed(rol1, res1b));
		Assert.assertTrue(acl.isAllowed(rol1, res1b1));
		//false because ARO-1::ACO-1 overrides *::ACO-1-C
		Assert.assertFalse(acl.isDenied(rol1, res1c));
		Assert.assertFalse(acl.isDenied(rol1, res1c1));
		//1-2
		Assert.assertTrue(acl.isDenied(rol1, res2));
		//1-3
		Assert.assertTrue(acl.isDenied(rol1, res3));
		//2-1
		Assert.assertTrue(acl.isDenied(rol2, res1));
		Assert.assertTrue(acl.isDenied(rol2, res1a));
		Assert.assertTrue(acl.isDenied(rol2, res1a1));
		Assert.assertTrue(acl.isDenied(rol2, res1a2));
		Assert.assertTrue(acl.isAllowed(rol2, res1b));
		Assert.assertTrue(acl.isAllowed(rol2, res1b1));
		//2-2
		//false because ARO-2::ACO-2 was added with CREATE:true before
		Assert.assertFalse(acl.isDenied(rol2, res2));
		//2-3
		Assert.assertTrue(acl.isDenied(rol2, res3));
		//3-1
		Assert.assertTrue(acl.isDenied(rol3, res1));
		Assert.assertTrue(acl.isDenied(rol3, res1a));
		Assert.assertTrue(acl.isDenied(rol3, res1a1));
		Assert.assertTrue(acl.isDenied(rol3, res1a2));
		Assert.assertTrue(acl.isAllowed(rol3, res1b));
		Assert.assertTrue(acl.isAllowed(rol3, res1b1));
		//3-2
		Assert.assertTrue(acl.isDenied(rol3, res2));
		//3-3
		Assert.assertTrue(acl.isDenied(rol3, res3));

		acl.allow(rol1, res1a1);
		//1-1
		Assert.assertTrue(acl.isAllowed(rol1, res1));
		Assert.assertTrue(acl.isDenied(rol1, res1a));
		Assert.assertTrue(acl.isAllowed(rol1, res1a1));
		Assert.assertTrue(acl.isDenied(rol1, res1a2));
		Assert.assertTrue(acl.isAllowed(rol1, res1b));
		Assert.assertTrue(acl.isAllowed(rol1, res1b1));
		//false because ARO-1::ACO-1 overrides *::ACO-1-C
		Assert.assertFalse(acl.isDenied(rol1, res1c));
		Assert.assertFalse(acl.isDenied(rol1, res1c1));
		//1-2
		Assert.assertTrue(acl.isDenied(rol1, res2));
		//1-3
		Assert.assertTrue(acl.isDenied(rol1, res3));
		//2-1
		Assert.assertTrue(acl.isDenied(rol2, res1));
		Assert.assertTrue(acl.isDenied(rol2, res1a));
		Assert.assertTrue(acl.isDenied(rol2, res1a1));
		Assert.assertTrue(acl.isDenied(rol2, res1a2));
		Assert.assertTrue(acl.isAllowed(rol2, res1b));
		Assert.assertTrue(acl.isAllowed(rol2, res1b1));
		//2-2
		//false because ARO-2::ACO-2 was added with CREATE:true before
		Assert.assertFalse(acl.isDenied(rol2, res2));
		//2-3
		Assert.assertTrue(acl.isDenied(rol2, res3));
		//3-1
		Assert.assertTrue(acl.isDenied(rol3, res1));
		Assert.assertTrue(acl.isDenied(rol3, res1a));
		Assert.assertTrue(acl.isDenied(rol3, res1a1));
		Assert.assertTrue(acl.isDenied(rol3, res1a2));
		Assert.assertTrue(acl.isAllowed(rol3, res1b));
		Assert.assertTrue(acl.isAllowed(rol3, res1b1));
		//3-2
		Assert.assertTrue(acl.isDenied(rol3, res2));
		//3-3
		Assert.assertTrue(acl.isDenied(rol3, res3));

		//deny ARO-2 to ACO-1-B; test overriding of ALL allow
		acl.deny(rol2, res1b);
		//1-1
		Assert.assertTrue(acl.isAllowed(rol1, res1));
		Assert.assertTrue(acl.isDenied(rol1, res1a));
		Assert.assertTrue(acl.isAllowed(rol1, res1a1));
		Assert.assertTrue(acl.isDenied(rol1, res1a2));
		Assert.assertTrue(acl.isAllowed(rol1, res1b));
		Assert.assertTrue(acl.isAllowed(rol1, res1b1));
		//false because ARO-1::ACO-1 overrides *::ACO-1-C
		Assert.assertFalse(acl.isDenied(rol1, res1c));
		Assert.assertFalse(acl.isDenied(rol1, res1c1));
		//1-2
		Assert.assertTrue(acl.isDenied(rol1, res2));
		//1-3
		Assert.assertTrue(acl.isDenied(rol1, res3));
		//2-1
		Assert.assertTrue(acl.isDenied(rol2, res1));
		Assert.assertTrue(acl.isDenied(rol2, res1a));
		Assert.assertTrue(acl.isDenied(rol2, res1a1));
		Assert.assertTrue(acl.isDenied(rol2, res1a2));
		Assert.assertTrue(acl.isDenied(rol2, res1b));
		Assert.assertTrue(acl.isDenied(rol2, res1b1));
		//2-2
		//false because ARO-2::ACO-2 was added with CREATE:true before
		Assert.assertFalse(acl.isDenied(rol2, res2));
		//2-3
		Assert.assertTrue(acl.isDenied(rol2, res3));
		//3-1
		Assert.assertTrue(acl.isDenied(rol3, res1));
		Assert.assertTrue(acl.isDenied(rol3, res1a));
		Assert.assertTrue(acl.isDenied(rol3, res1a1));
		Assert.assertTrue(acl.isDenied(rol3, res1a2));
		Assert.assertTrue(acl.isAllowed(rol3, res1b));
		Assert.assertTrue(acl.isAllowed(rol3, res1b1));
		//3-2
		Assert.assertTrue(acl.isDenied(rol3, res2));
		//3-3
		Assert.assertTrue(acl.isDenied(rol3, res3));

		//deny ARO-3 to ACO-1-B-1; test specific deny over ALL allow
		acl.deny(rol3, res1b1);
		//1-1
		Assert.assertTrue(acl.isAllowed(rol1, res1));
		Assert.assertTrue(acl.isDenied(rol1, res1a));
		Assert.assertTrue(acl.isAllowed(rol1, res1a1));
		Assert.assertTrue(acl.isDenied(rol1, res1a2));
		Assert.assertTrue(acl.isAllowed(rol1, res1b));
		Assert.assertTrue(acl.isAllowed(rol1, res1b1));
		//false because ARO-1::ACO-1 overrides *::ACO-1-C
		Assert.assertFalse(acl.isDenied(rol1, res1c));
		Assert.assertFalse(acl.isDenied(rol1, res1c1));
		//1-2
		Assert.assertTrue(acl.isDenied(rol1, res2));
		//1-3
		Assert.assertTrue(acl.isDenied(rol1, res3));
		//2-1
		Assert.assertTrue(acl.isDenied(rol2, res1));
		Assert.assertTrue(acl.isDenied(rol2, res1a));
		Assert.assertTrue(acl.isDenied(rol2, res1a1));
		Assert.assertTrue(acl.isDenied(rol2, res1a2));
		Assert.assertTrue(acl.isDenied(rol2, res1b));
		Assert.assertTrue(acl.isDenied(rol2, res1b1));
		//2-2
		//false because ARO-2::ACO-2 was added with CREATE:true before
		Assert.assertFalse(acl.isDenied(rol2, res2));
		//2-3
		Assert.assertTrue(acl.isDenied(rol2, res3));
		//3-1
		Assert.assertTrue(acl.isDenied(rol3, res1));
		Assert.assertTrue(acl.isDenied(rol3, res1a));
		Assert.assertTrue(acl.isDenied(rol3, res1a1));
		Assert.assertTrue(acl.isDenied(rol3, res1a2));
		Assert.assertTrue(acl.isAllowed(rol3, res1b));
		Assert.assertTrue(acl.isDenied(rol3, res1b1));
		//3-2
		Assert.assertTrue(acl.isDenied(rol3, res2));
		//3-3
		Assert.assertTrue(acl.isDenied(rol3, res3));

		//test coverage
		acl.allow(rol4, res1c);
		//4-1
		Assert.assertTrue(acl.isDenied(rol4, res1));
		Assert.assertTrue(acl.isDenied(rol4, res1a));
		Assert.assertTrue(acl.isDenied(rol4, res1a1));
		Assert.assertTrue(acl.isDenied(rol4, res1a2));
		Assert.assertTrue(acl.isAllowed(rol4, res1b));
		Assert.assertTrue(acl.isAllowed(rol4, res1b1));
		Assert.assertTrue(acl.isAllowed(rol4, res1c));
		Assert.assertTrue(acl.isAllowed(rol4, res1c1));
		Assert.assertTrue(acl.isDenied(rol4, res4));
		acl.allow(rol4, res4);
		Assert.assertTrue(acl.isAllowed(rol4, res4));
	}

	private void testRemoveNull() {
		Rol rolna = new Rol("NA-ROLE");
		Res resna = new Res("NA-RES");
		Rol nullrol = null;
		Res nullres = null;

		Assert.assertFalse(acl.isAllowed(rolna, resna));
		Assert.assertTrue(acl.isDenied(rolna, resna));

		acl.remove(nullrol, nullres);

		//false for both because the root is removed
		Assert.assertFalse(acl.isAllowed(rolna, resna));
		Assert.assertFalse(acl.isDenied(rolna, resna));

		boolean thrown = false;
		try {
			//removing null again should throw exception
			acl.remove(nullrol, nullres, "CREATE");
		} catch (EntryNotFoundException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);
	}

	private void testRemoveResourceRole() {
		String[] rols = {
			"R1", "R2", "R3", "R4"
		};
		String[] ress = {
			"C1", "C2", "C3", "C4"
		};

		acl.clear();
		//create mappings for each key pair
		for (String c: ress) {
			for (String r: rols) {
				acl.allow(new Rol(r), new Res(c));
				Assert.assertTrue(acl.isAllowed(new Rol(r), new Res(c)));
			}
		}
		//add children to both resources and roles
		for (int i = 1; i <= 4; i++) {
			acl.addResource(new Res("CC" + i), new Res("C" + i));
			acl.addRole(new Rol("RC" + i), new Rol("R" + i));
		}
		//assign permission for child1 to child2
		acl.allow(new Rol("RC1"), new Res("CC2"));
		acl.allow(new Rol("RC2"), new Res("CC1"));

		//add ALL access
		for (String c: ress) {
			acl.allowAllRole(new Res(c));
			Assert.assertTrue(acl.isAllowed(new Rol("*"), new Res(c)));
		}
		for (String r: rols) {
			acl.allowAllResource(new Rol(r));
			Assert.assertTrue(acl.isAllowed(new Rol(r), new Res("*")));
		}
		//4x4 pairs, 2x4 ALL access, 1 child each
		Assert.assertEquals(acl.exportPermissions().size(), 26); //4x4+4+4+1+1
		Assert.assertEquals(acl.exportResources().size(), 8); //4+4
		Assert.assertEquals(acl.exportRoles().size(), 8); //4+4

		//remove all access on C4
		acl.removeResource(new Res("C4"), false);
		Assert.assertEquals(acl.exportPermissions().size(), 21); //4x3+4+3+1+1
		Assert.assertEquals(acl.exportResources().size(), 7); //3+4, less C4 but not its child
		Assert.assertEquals(acl.exportRoles().size(), 8); //4+4

		//remove all access on R4
		acl.removeRole(new Rol("R4"), false);
		Assert.assertEquals(acl.exportPermissions().size(), 17); //3x3+3+3+1+1
		Assert.assertEquals(acl.exportResources().size(), 7); //3+4
		Assert.assertEquals(acl.exportRoles().size(), 7); //3+4, less R4 but not its child

		//remove all access on C3 and child
		acl.removeResource(new Res("C3"), true);
		Assert.assertEquals(acl.exportPermissions().size(), 13); //3x2+3+2+1+1
		Assert.assertEquals(acl.exportResources().size(), 5); //2+3, less C3 and child
		Assert.assertEquals(acl.exportRoles().size(), 7); //3+4

		//remove all access on R3 and child
		acl.removeRole(new Rol("R3"), true);
		Assert.assertEquals(acl.exportPermissions().size(), 10); //2x2+2+2+1+1
		Assert.assertEquals(acl.exportResources().size(), 5); //2+3
		Assert.assertEquals(acl.exportRoles().size(), 5); //2+3, less R3 and child

		//remove all access on C2 and child
		acl.removeResource(new Res("C2"), true);
		Assert.assertEquals(acl.exportPermissions().size(), 6); //2x1+2+1+1+0, less child permission
		Assert.assertEquals(acl.exportResources().size(), 3); //1+2, less C2 and child
		Assert.assertEquals(acl.exportRoles().size(), 5); //2+3

		//remove all access on R2 and child
		acl.removeRole(new Rol("R2"), true);
		Assert.assertEquals(acl.exportPermissions().size(), 3); //1x1+1+1+0+0, less child permission
		Assert.assertEquals(acl.exportResources().size(), 3); //1+2
		Assert.assertEquals(acl.exportRoles().size(), 3); //1+2, less R2 and child

		//remove all access on C1 and child
		acl.removeResource(new Res("C1"), true);
		Assert.assertEquals(acl.exportPermissions().size(), 1); //1x0+1+0+0+0
		Assert.assertEquals(acl.exportResources().size(), 1); //0+1
		Assert.assertEquals(acl.exportRoles().size(), 3); //1+2

		//remove all access on R1 and child
		acl.removeRole(new Rol("R1"), true);
		Assert.assertEquals(acl.exportPermissions().size(), 0); //0x0+0+0+0+0
		Assert.assertEquals(acl.exportResources().size(), 1); //0+1
		Assert.assertEquals(acl.exportRoles().size(), 1); //0+1

		//test coverage
		AclEntry nullEntry = null;
		String nullString = null;
		boolean thrown = false;
		try {
			acl.removeResource(nullEntry, false);
		} catch (RuntimeException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);

		thrown = false;
		try {
			acl.removeRole(nullEntry, false);
		} catch (RuntimeException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);

		thrown = false;
		try {
			acl.removeResource(nullString, false);
		} catch (RuntimeException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);

		thrown = false;
		try {
			acl.removeRole(nullString, false);
		} catch (RuntimeException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);
	}

	@Test(priority = 48)
	public void testExportImport() {
		Acl acl = Acl.makeInstance();
		//test export
		Map<String, String> resources = acl.exportResources();
		Map<String, String> roles = acl.exportRoles();
		Map<String, Map<String, Boolean>> perms = acl.exportPermissions();

		Assert.assertTrue(resources.size() == 0, "Existing resources");
		Assert.assertTrue(roles.size() == 0, "Existing roles");
		Assert.assertTrue(perms.size() == 1, "Existing default permission");

		//verify that the exported ones are indeed snapshots
		acl.addResource(new Resource("laser-gun"));
		acl.addRole(new Role("jedi"));
		acl.deny(new Role("jedi"), new Resource("laser-gun"));

		Map<String, String> newRes = acl.exportResources();
		Map<String, String> newRoles = acl.exportRoles();
		Map<String, Map<String, Boolean>> newPerms = acl.exportPermissions();

		Assert.assertTrue(newRes.size() > resources.size(),
				"Exported resources are not affected");
		Assert.assertTrue(newRoles.size() > roles.size(),
				"Exported roles are not affected");
		Assert.assertTrue(newPerms.size() > perms.size(),
				"Exported permissions are not affected");

		//simulate saved data
		newRes = new HashMap<String, String>();
		newRes.put("laser-gun", "");
		newRes.put("light-sabre", "");
		newRes.put("staff", "light-sabre");
		newRes.put("t", "light-sabre");
		newRoles = new HashMap<String, String>();
		newRoles.put("jedi", "");
		newRoles.put("sith", "");
		newRoles.put("obiwan", "jedi");
		newRoles.put("luke", "jedi");
		newRoles.put("darth-vader", "sith");
		newRoles.put("darth-maul", "sith");
		newPerms = new HashMap<>();
		Map<String, Boolean> allTrue = new HashMap<>();
		allTrue.put("ALL", true);
		Map<String, Boolean> allFalse = new HashMap<>();
		allFalse.put("ALL", false);
		newPerms.put("*::*", allFalse);
		newPerms.put("jedi::light-sabre", allTrue);
		newPerms.put("jedi::laser-gun", allFalse);
		newPerms.put("sith::light-sabre", allTrue);
		newPerms.put("sith::laser-gun", allTrue);
		newPerms.put("luke::laser-gun", allTrue);
		newPerms.put("jedi::t", allFalse);

		//test import
		boolean thrown = false;
		try {
			acl.importResources(resources);
		} catch (NonEmptyException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown, "Resources are non-empty");
		thrown = false;
		try {
			acl.importRoles(roles);
		} catch (NonEmptyException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown, "Roles are non-empty");
		thrown = false;
		try {
			acl.importPermissions(newPerms);
		} catch (NonEmptyException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown, "Permissions are non-empty");

		acl.clear();
		acl.importResources(newRes);
		acl.importRoles(newRoles);
		acl.importPermissions(newPerms);

		//verify the permissions are correct
		Assert.assertTrue(acl.isAllowed(new Role("jedi"), new Resource(
				"light-sabre")));
		Assert.assertTrue(acl.isDenied(new Role("jedi"), new Resource(
				"laser-gun")));
		Assert.assertTrue(acl.isAllowed(new Role("luke"), new Resource(
				"laser-gun")));
		Assert.assertTrue(acl.isAllowed(new Role("sith"), new Resource(
				"laser-gun")));
		Assert.assertTrue(acl.isDenied(new Role("jedi"), new Resource("t")));

		//change permissions
		acl.deny(new Role("sith"), new Resource("laser-gun"));
		//add resource, role and permission
		acl.addResource(new Resource("double"), new Resource("light-sabre"));
		acl.addRole(new Role("Anakin"), new Role("jedi"));
		acl.allow(new Role("jedi"), new Resource("double"), "UPDATE");

		//verify permissions are correct
		Assert.assertTrue(
				acl.isAllowed(new Role("jedi"), new Resource("double")),
				"True because UPDATE on double is redundant");
		Assert.assertTrue(acl.isAllowed(new Role("jedi"),
				new Resource("double"), "UPDATE"));

		//change double to be not redundant
		acl.deny(new Role("jedi"), new Resource("double"), "CREATE");

		Assert.assertFalse(
				acl.isAllowed(new Role("jedi"), new Resource("double")),
				"False because jedi::double DENY CREATE");
		Assert.assertTrue(acl.isAllowed(new Role("jedi"),
				new Resource("double"), "UPDATE"), "Still true");
		Assert.assertTrue(acl.isDenied(new Role("luke"),
				new Resource("double"), "CREATE"),
				"True because inherits jedi.");
		Assert.assertTrue(acl.isDenied(new Role("sith"), new Resource(
				"laser-gun")));
		Assert.assertTrue(acl.isDenied(new Role("jedi"), new Resource("t")));

		resources = acl.exportResources();
		roles = acl.exportRoles();
		perms = acl.exportPermissions();

		//verify that the maps are disconnected
		Assert.assertTrue(newRes.size() < resources.size(),
				"Exported resources are not affected");
		Assert.assertTrue(newRoles.size() < roles.size(),
				"Exported roles are not affected");
		Assert.assertTrue(newPerms.size() < perms.size(),
				"Exported permissions are not affected");

	}

	@Test(priority = 41)
	public void testRealLife() {
		Acl acl = Acl.makeInstance();
		String GENERAL = "GENERAL";
		String SYSTEM = "SYSTEM";
		String FINANCE = "FINANCE";
		String TECH = "TECH";

		Map<String, String> roles = new HashMap<>();
		roles.put(GENERAL, "*");
		roles.put(SYSTEM, GENERAL);
		roles.put("chuacheehow@gsatech.com.sg", SYSTEM);
		roles.put(FINANCE, "*");
		roles.put(TECH, "*");
		roles.put("rahman@gsatech.com.sg", TECH);
		roles.put("julia@gsatech.com.sg", FINANCE);

		Map<String, String> res = new HashMap<>();
		res.put("organization", "*");
		res.put("device", "*");
		res.put("make", "*");

		Map<String, Boolean> allFalse = new HashMap<>();
		allFalse.put("ALL", false);
		Map<String, Boolean> allTrue = new HashMap<>();
		allTrue.put("ALL", true);
		Map<String, Map<String, Boolean>> perms = new HashMap<>();
		perms.put("*::*", allFalse);
		perms.put(SYSTEM + "::*", allTrue);
		perms.put(TECH + "::device", allTrue);
		perms.put(FINANCE + "::organization", allTrue);

		acl.clear();
		acl.importRoles(roles);
		acl.importResources(res);
		acl.importPermissions(perms);

		System.out.println(acl.visualize());
	}

	@Test
	//following the example from http://book.cakephp.org/2.0/en/core-libraries/components/access-control-lists.html
	public void testCakeExampleAndImport() {
		Acl a = Acl.makeInstance();
		a.clear();

		Rol warriors = new Rol("Warriors");
		Rol aragorn = new Rol("Aragon");
		Rol legolas = new Rol("Legolas");
		Rol gimli = new Rol("Gimli");
		Rol wizards = new Rol("Wizards");
		Rol gandalf = new Rol("Gandalf");
		Rol hobbits = new Rol("Hobbits");
		Rol frodo = new Rol("Frodo");
		Rol bilbo = new Rol("Bilbo");
		Rol merry = new Rol("Merry");
		Rol pippin = new Rol("Pippin");
		Rol visitors = new Rol("visitors");
		Rol gollum = new Rol("Gollum");

		//test importRoles
		Map<String, String> roles = new HashMap<>();
		roles.put(warriors.getId(), "");
		roles.put(wizards.getId(), "");
		roles.put(hobbits.getId(), "");
		roles.put(visitors.getId(), "");
		a.importRoles(roles);
//		a.addRole(warriors);
//		a.addRole(wizards);
//		a.addRole(hobbits);
//		a.addRole(visitors);
		boolean thrown = false;
		//cannot import twice
		try {
			a.importRoles(roles);
		} catch (NonEmptyException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);
		//test exportRoles
		Map<String, String> expRoles = a.exportRoles();
		Assert.assertEquals(expRoles, roles);

		a.addRole(gimli, warriors);
		a.addRole(legolas, warriors);
		a.addRole(aragorn, warriors);

		a.addRole(gandalf, wizards);

		a.addRole(frodo, hobbits);
		a.addRole(bilbo, hobbits);
		a.addRole(merry, hobbits);
		a.addRole(pippin, hobbits);

		a.addRole(gollum, visitors);

		Res weapons = new Res("Weapons");
		Res ring = new Res("The One Ring");
		Res pork = new Res("Salted Pork");
		Res diplomacy = new Res("Diplomacy");
		Res ale = new Res("Ale");

		//test importResources
		Map<String, String> resources = new HashMap<>();
		resources.put(weapons.getId(), "");
		resources.put(ring.getId(), "");
		resources.put(pork.getId(), "");
		resources.put(diplomacy.getId(), "");
		resources.put(ale.getId(), "");
		a.importResources(resources);
//		a.addResource(weapons);
//		a.addResource(ring);
//		a.addResource(pork);
//		a.addResource(diplomacy);
//		a.addResource(ale);
		thrown = false;
		//cannot import twice
		try {
			a.importResources(roles);
		} catch (NonEmptyException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);
		//test exportResources
		Map<String, String> expRes = a.exportResources();
		Assert.assertEquals(expRes, resources);

		//deny all
		a.makeDefaultDeny();

		//allow warriors
		a.allow(warriors, weapons);
		a.allow(warriors, ale);
		a.allow(warriors, pork);
		a.allow(aragorn, diplomacy);
		a.deny(gimli, weapons, "DELETE");
		a.deny(legolas, weapons, "DELETE");

		//allow wizards
		a.allow(wizards, ale);
		a.allow(wizards, pork);
		a.allow(wizards, diplomacy);

		//allow hobbits
		a.allow(hobbits, ale);
		a.allow(frodo, ring);
		a.deny(merry, ale);
		a.allow(pippin, diplomacy);

		//allow visitors
		a.allow(visitors, pork);

		printRegistries(a);

		//Pippin can access ale
		Assert.assertTrue(a.isAllowed(pippin, ale));
		//Merry cannot
		Assert.assertTrue(a.isDenied(merry, ale));

		//aragorn
		Assert.assertTrue(a.isAllowed(aragorn, weapons));
		Assert.assertTrue(a.isAllowed(aragorn, weapons, "CREATE"));
		Assert.assertTrue(a.isAllowed(aragorn, weapons, "READ"));
		Assert.assertTrue(a.isAllowed(aragorn, weapons, "UPDATE"));
		Assert.assertTrue(a.isAllowed(aragorn, weapons, "DELETE"));
		//legolas
		Assert.assertFalse(a.isAllowed(legolas, weapons));
		Assert.assertTrue(a.isAllowed(legolas, weapons, "CREATE"));
		Assert.assertTrue(a.isAllowed(legolas, weapons, "READ"));
		Assert.assertTrue(a.isAllowed(legolas, weapons, "UPDATE"));
		Assert.assertFalse(a.isAllowed(legolas, weapons, "DELETE"));
		//gimli
		Assert.assertFalse(a.isAllowed(gimli, weapons));
		Assert.assertTrue(a.isAllowed(gimli, weapons, "CREATE"));
		Assert.assertTrue(a.isAllowed(gimli, weapons, "READ"));
		Assert.assertTrue(a.isAllowed(gimli, weapons, "UPDATE"));
		Assert.assertFalse(a.isAllowed(gimli, weapons, "DELETE"));
	}

	private void printRegistries(Acl a) {
		Res res = new Res("ACO");
		Rol rol = new Rol("ARO");

		System.out.println(">>> RESOURCES");
		System.out.println(a.visualizeResources(res));
		System.out.println(">>> ROLES");
		System.out.println(a.visualizeRoles(rol));
		System.out.println(">>> PERMISSIONS");
		System.out.println(a.visualizePermissions());
	}
}

class Res implements AclEntry {
	String desc;
	String id;

	public Res(String id) {
		this.desc = id;
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getEntryDescription() {
		return desc;
	}

	@Override
	public AclEntry retrieveEntry(String resourceId) {
		return new Resource(resourceId);
	}
}

class Rol implements AclEntry {
	String desc;
	String id;

	public Rol(String id) {
		this.desc = id;
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getEntryDescription() {
		return desc;
	}

	@Override
	public AclEntry retrieveEntry(String resourceId) {
		return new Resource(resourceId);
	}
}
