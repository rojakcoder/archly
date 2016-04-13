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
		String res1 = new String("RES-1");
		String rol1 = new String("ROLE-1");
		String nullres = null;
		String nullrol = null;

		//null because permissions not specified
		Assert.assertNull(p.isAllowed(rol1, res1));
		Assert.assertNull(p.isDenied(rol1, res1));

		Assert.assertNull(p.isAllowed(rol1, null));
		Assert.assertNull(p.isDenied(rol1, null));

		Assert.assertNull(p.isAllowed(null, res1));
		Assert.assertNull(p.isDenied(null, res1));

		Assert.assertFalse(p.isAllowed(nullres, nullrol));
		Assert.assertTrue(p.isDenied(nullres, nullrol)); //default deny

		p.makeDefaultAllow();

		Assert.assertNull(p.isAllowed(rol1, res1));
		Assert.assertNull(p.isAllowed(rol1, null));
		Assert.assertNull(p.isAllowed(null, res1));
		Assert.assertTrue(p.isAllowed(nullrol, nullres));
		Assert.assertTrue(p
				.isAllowed(nullrol, nullres, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(nullrol, nullres));

		p.makeDefaultDeny();

		Assert.assertNull(p.isDenied(rol1, res1));
		Assert.assertNull(p.isDenied(rol1, null));
		Assert.assertNull(p.isDenied(null, res1));
		Assert.assertTrue(p.isDenied(nullrol, nullres, Permission.Types.CREATE));
		Assert.assertFalse(p.isAllowed(nullres, nullrol));

	}

	public void testAllow() {
		String res1 = "RES-1";
		String rol1 = "ROLE-1";

		Assert.assertNull(p.isAllowed(rol1, res1));
		Assert.assertNull(p.isDenied(rol1, res1));

		p.allow(rol1, res1);
		Assert.assertTrue(p.isAllowed(rol1, res1));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		//false because explicit allow
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
		Assert.assertNull(p.isAllowed(rol1, res1)); //is now NULL
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertNull(p.isDenied(rol1, res1));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.deny(rol1, res1, Permission.Types.DELETE);
		//ALL is removed, DELETE is false, others are NULL
		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		//no explicit deny on ALL, explicit deny on DELETE
		Assert.assertNull(p.isDenied(rol1, res1));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.allow(rol1, res1, Permission.Types.CREATE);
		p.allow(rol1, res1, Permission.Types.READ);
		p.allow(rol1, res1, Permission.Types.DELETE);
		//no explicit deny so ALL is NULL
		Assert.assertNull(p.isAllowed(rol1, res1));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		//no explicit deny on ALL, explicit allow on some
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.UPDATE));
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
		String res1 = "RES-A";
		String rol1 = "ROLE-A";

		Assert.assertNull(p.isAllowed(rol1, res1));
		Assert.assertNull(p.isDenied(rol1, res1));

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
		//false because there is an explicit deny on ALL
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
		//false because there is an explicit deny on ALL
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
		Assert.assertNull(p.isAllowed(rol1, res1));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		//now NULL since removed
		Assert.assertNull(p.isDenied(rol1, res1));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.allow(rol1, res1, Permission.Types.DELETE);
		//no explicit deny so ALL is NULL
		Assert.assertNull(p.isAllowed(rol1, res1));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertTrue(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		//explicit allow on DELETE so false
		Assert.assertFalse(p.isDenied(rol1, res1));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isDenied(rol1, res1, Permission.Types.DELETE));

		p.deny(rol1, res1, Permission.Types.CREATE);
		p.deny(rol1, res1, Permission.Types.READ);
		p.deny(rol1, res1, Permission.Types.DELETE);
		//false because of explicit deny
		Assert.assertFalse(p.isAllowed(rol1, res1));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.CREATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.READ));
		Assert.assertNull(p.isAllowed(rol1, res1, Permission.Types.UPDATE));
		Assert.assertFalse(p.isAllowed(rol1, res1, Permission.Types.DELETE));
		Assert.assertNull(p.isDenied(rol1, res1));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.CREATE));
		Assert.assertTrue(p.isDenied(rol1, res1, Permission.Types.READ));
		Assert.assertNull(p.isDenied(rol1, res1, Permission.Types.UPDATE));
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
		String res1 = new String("RES-1");
		String rol1 = new String("ROLE-1");
		String resna = new String("RES-NA");
		String rolna = new String("ROLE-NA");

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
