package com.rojakcoder.archly;

import org.junit.Assert;
import org.testng.annotations.Test;

import com.rojakcoder.archly.exceptions.DuplicateEntryException;

public class AclTest {
	static Acl acl = Acl.getInstance();

	@Test(priority = 4)
	public void testRunner() {
		Assert.assertFalse(acl.isAllowed(null, null)); //default false
		acl.makeDefaultAllow();
		Assert.assertTrue(acl.isAllowed(null, null));

		testResource();
		testRole();
		testAllow();
		testDeny();
		testRemove();
		testHierarchy();

		AclEntry root = new RootEntry();
		//test coverage
		Assert.assertEquals(root.getEntryDescription(), "ROOT");
		Assert.assertNull(root.retrieveEntry(null));
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

		//restore to default false
		acl.makeDefaultDeny();

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
		Assert.assertTrue(acl.isDenied(rol1, res1c));
		Assert.assertTrue(acl.isDenied(rol1, res1c1));
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
		Assert.assertTrue(acl.isDenied(rol2, res2));
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
		Assert.assertTrue(acl.isDenied(rol1, res1c));
		Assert.assertTrue(acl.isDenied(rol1, res1c1));
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
		Assert.assertTrue(acl.isDenied(rol2, res2));
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
		Assert.assertTrue(acl.isDenied(rol1, res1c));
		Assert.assertTrue(acl.isDenied(rol1, res1c1));
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
		Assert.assertTrue(acl.isDenied(rol2, res2));
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
		Assert.assertTrue(acl.isDenied(rol1, res1c));
		Assert.assertTrue(acl.isDenied(rol1, res1c1));
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
		Assert.assertTrue(acl.isDenied(rol2, res2));
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
		Assert.assertTrue(acl.isDenied(rol1, res1c));
		Assert.assertTrue(acl.isDenied(rol1, res1c1));
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
		Assert.assertTrue(acl.isDenied(rol2, res2));
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

	private void printReg() {
		Res res = new Res("ACO");
		Rol rol = new Rol("ARO");

		System.out.println(">>> RESOURCES");
		System.out.println(ResourceRegistry.getSingleton().print(res, null,
				null));
		System.out.println(">>> ROLES");
		System.out.println(RoleRegistry.getSingleton().print(rol, null, null));
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
