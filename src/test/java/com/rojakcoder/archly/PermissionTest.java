package com.rojakcoder.archly;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.rojakcoder.archly.exceptions.EntryNotFoundException;

public class PermissionTest {
	static Permission p = Permission.getSingleton();

	@Test(priority = 3)
	public void testRunner() {
		testIsAllowedDenied();
		testAllow();
		testDeny();
		testRemove();

		int size = p.size();
		Permission pm = Permission.getSingleton();
		Assert.assertEquals(pm.size(), size);
	}

	public void testIsAllowedDenied() {
		Pres res1 = new Pres("RES-1");
		Prol rol1 = new Prol("ROLE-1");
		Pres nullres = null;
		Prol nullrol = null;

		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1));

		Assert.assertFalse(p.isAllowed(rol1, null));
		Assert.assertFalse(p.isDenied(rol1, null));

		Assert.assertFalse(p.isAllowed(null, res1));
		Assert.assertFalse(p.isDenied(null, res1));

		Assert.assertFalse(p.isAllowed(nullres, nullrol));
		Assert.assertTrue(p.isDenied(nullres, nullrol)); //default deny

		p.makeDefaultAllow();

		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isAllowed(rol1, null));
		Assert.assertFalse(p.isAllowed(null, res1));
		Assert.assertTrue(p.isAllowed(nullres, nullrol));
		Assert.assertTrue(p
				.isAllowed(nullres, nullrol, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(nullres, nullrol));

		p.makeDefaultDeny();

		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, null));
		Assert.assertFalse(p.isDenied(null, res1));
		Assert.assertTrue(p.isDenied(nullrol, nullres, Permission.Types.CREATE));
		Assert.assertFalse(p.isAllowed(nullres, nullrol));

	}

	public void testAllow() {
		Pres res1 = new Pres("RES-1");
		Prol rol1 = new Prol("ROLE-1");

		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1));

		p.allow(rol1, res1);
		Assert.assertTrue(p.isAllowed(rol1, res1));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.deny(rol1, res1, Permission.Types.UPDATE);
		p.deny(rol1, res1, Permission.Types.DELETE);
		Assert.assertFalse(p.isAllowed(rol1, res1)); //no longer all true
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.remove(rol1, res1, Permission.Types.UPDATE);
		Assert.assertFalse(p.isAllowed(rol1, res1)); //no longer all true
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.remove(rol1, res1);
		Assert.assertFalse(p.isAllowed(rol1, res1)); //is now true
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.deny(rol1, res1, Permission.Types.DELETE);
		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.allow(rol1, res1, Permission.Types.CREATE);
		p.allow(rol1, res1, Permission.Types.READ);
		p.allow(rol1, res1, Permission.Types.DELETE);
		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.DELETE));

		//equivalent of ALL allow
		p.allow(rol1, res1, Permission.Types.UPDATE);
		Assert.assertTrue(p.isAllowed(rol1, res1));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.DELETE));
	}

	public void testDeny() {
		//use different identifiers
		Pres res1 = new Pres("RES-A");
		Prol rol1 = new Prol("ROLE-A");

		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1));

		p.allow(rol1, res1);
		Assert.assertTrue(p.isAllowed(rol1, res1));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.deny(rol1, res1);
		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertTrue(p.isDenied(rol1, res1));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.allow(rol1, res1, Permission.Types.UPDATE);
		p.allow(rol1, res1, Permission.Types.DELETE);
		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1)); //no longer all true
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.remove(rol1, res1, Permission.Types.UPDATE);
		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.remove(rol1, res1);
		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1)); //is now true
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.allow(rol1, res1, Permission.Types.DELETE);
		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.deny(rol1, res1, Permission.Types.CREATE);
		p.deny(rol1, res1, Permission.Types.READ);
		p.deny(rol1, res1, Permission.Types.DELETE);
		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.DELETE));

		//equivalent of ALL deny
		p.deny(rol1, res1, Permission.Types.UPDATE);
		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertTrue(p.isDenied(rol1, res1));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.DELETE));
	}

	public void testRemove() {
		Pres res1 = new Pres("RES-1");
		Prol rol1 = new Prol("ROLE-1");
		Pres resna = new Pres("RES-NA");
		Prol rolna = new Prol("ROLE-NA");

		System.out.println(p.toString());
		//non-existing role and resource
		boolean thrown = false;
		try {
			p.remove(rolna, resna);
		} catch (EntryNotFoundException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);
		thrown = false;
		try {
			p.remove(rol1, resna);
		} catch (EntryNotFoundException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);
		thrown = false;
		try {
			p.remove(rolna, res1);
		} catch (EntryNotFoundException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);

		//non-existing role and resource
		thrown = false;
		try {
			p.remove(rolna, resna, Permission.Types.CREATE);
		} catch (EntryNotFoundException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);
		thrown = false;
		try {
			p.remove(rol1, resna, Permission.Types.CREATE);
		} catch (EntryNotFoundException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);
		thrown = false;
		try {
			p.remove(rolna, res1, Permission.Types.CREATE);
		} catch (EntryNotFoundException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);

		thrown = false;
		try {
			p.remove(rol1, res1, Permission.Types.CREATE);
		} catch (EntryNotFoundException e) {
			thrown = true;
		}
		Assert.assertFalse(thrown); //no exception
		thrown = false;
		try {
			p.remove(rol1, res1, Permission.Types.CREATE);
		} catch (EntryNotFoundException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown); //exception when repeated

		//remove the root privileges
//		Assert.assertTrue(p.has("*::*"));
		p.remove(null, null);
//		Assert.assertFalse(p.has("*::*"));

		//trying to remove the root but already removed
		thrown = false;
		try {
			p.remove(null, null, Permission.Types.CREATE);
		} catch (EntryNotFoundException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown); //exception when repeated
	}
}

class Pres implements AclEntry {
	String desc;
	String id;

	public Pres(String id) {
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

class Prol implements AclEntry {
	String desc;
	String id;

	public Prol(String id) {
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
