package com.rojakcoder.archly;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.rojakcoder.archly.exceptions.DuplicateEntryException;
import com.rojakcoder.archly.exceptions.EntryNotFoundException;
import com.rojakcoder.archly.exceptions.NonEmptyException;

public class AclTest {
	static Acl acl = Acl.getInstance();

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
	}

	@Test(priority = 49)
	public void testCoverage() {
		AclEntry root = new RootEntry();
		AclEntry nulle = null;
		//test coverage
		Assert.assertEquals(root.getEntryDescription(), "ROOT");
		Assert.assertNull(root.retrieveEntry(null));

		Permission p = Permission.getSingleton();
		p.permissions.remove("*::*");
		Assert.assertFalse(acl.isAllowed(nulle, nulle));
		Assert.assertFalse(acl.isAllowed(nulle, nulle, "ALL"));
		Assert.assertFalse(acl.isDenied(nulle, nulle));
		Assert.assertFalse(acl.isDenied(nulle, nulle, "ALL"));
	}

	public void testResource() {
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

	public void testRole() {
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

	public void testAllow() {
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

	public void testDeny() {
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

	public void testRemove() {
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
		Assert.assertFalse(Permission.getSingleton().permissions
				.containsKey(rol2.getId() + "::" + res2.getId()));

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

	public void testHierarchy() {
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
		Assert.assertTrue(acl.isDenied(rol1, res2));
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

	public void testRemoveNull() {
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

	@Test(priority = 41)
	//following the example from http://book.cakephp.org/2.0/en/core-libraries/components/access-control-lists.html
	public void testCakeExampleAndImport() {
		Acl a = Acl.getInstance();
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

		printReg();

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

	private void printReg() {
		Res res = new Res("ACO");
		Rol rol = new Rol("ARO");

		System.out.println(">>> RESOURCES");
		System.out.println(ResourceRegistry.getSingleton().display(res, null,
				null));
		System.out.println(">>> ROLES");
		System.out
				.println(RoleRegistry.getSingleton().display(rol, null, null));
		System.out.println(">>> PERMISSIONS");
		System.out.println(Permission.getSingleton());
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
