package com.rojakcoder.archly;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.rojakcoder.archly.exceptions.DuplicateEntryException;
import com.rojakcoder.archly.exceptions.EntryNotFoundException;

public class ResourceRegistryTest {

	@Test(priority = 2)
	public void testRunner() throws DuplicateEntryException {
		testAddRemoveEntry();
		testAddRemoveParents();
	}

	public void testAddRemoveEntry() {
		System.out.println("testAddRemoveEntry");

		ResourceRegistry reg = ResourceRegistry.getSingleton();
		AclEntry u1 = new Resource("RES-1");
		AclEntry u2 = new Resource("RES-2");
		AclEntry u = new Resource("RES");
		boolean thrown = false;

		Assert.assertEquals(reg.size(), 0);

		reg.add(u1);
		Assert.assertEquals(reg.size(), 1);
		try {
			reg.add(u1);
		} catch (DuplicateEntryException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);

		reg.add(u2);
		Assert.assertEquals(reg.size(), 2);

		thrown = false;
		try {
			reg.remove(u, false);
		} catch (EntryNotFoundException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);

		reg.remove(u1, false);
		Assert.assertEquals(reg.size(), 1);

		//add RES-1 back
		reg.add(u1);
	}

	public void testAddRemoveParents() {
		System.out.println("testAddParents");
		boolean thrown = false;

		ResourceRegistry reg = ResourceRegistry.getSingleton();
		AclEntry u1 = new Resource("RES-1");
		AclEntry u2 = new Resource("RES-2");

		AclEntry u1a = new Resource("RES-1-A");
		AclEntry u1b = new Resource("RES-1-B");
		AclEntry u2a = new Resource("RES-2-A");
		AclEntry u2a1 = new Resource("RES-2-A-1");
		AclEntry u2a1i = new Resource("RES-2-A-1-i");
		AclEntry u1b1 = new Resource("RES-1-B-1");

		reg.add(u1a, u1);
		Assert.assertEquals(reg.size(), 3);

		reg.add(u1b, u1);
		Assert.assertEquals(reg.size(), 4);

		thrown = false;
		try {
			reg.add(u1b, u1);
		} catch (DuplicateEntryException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown);

		reg.add(u2a, u2);
		reg.add(u1b1, u1b);
		Assert.assertEquals(reg.size(), 6);

		reg.add(u2a1, u2a);
		reg.add(u2a1i, u2a1);
		Assert.assertEquals(reg.size(), 8);

		//remove u2 and all descendants
		reg.remove(u2, true);
		Assert.assertEquals(reg.size(), 4);

		//remove u1b and expect u1b1 to be under u1
		reg.remove(u1b, false);
		Assert.assertEquals(reg.size(), 3);
		Assert.assertTrue(reg.has(u1b1));
		Assert.assertTrue(reg.hasChild(u1.getId()));

		//remove u1b1 and u1a, and expect u1 to be childless
		reg.remove(u1b1, false);
		reg.remove(u1a, true);
		Assert.assertEquals(reg.size(), 1);
		Assert.assertFalse(reg.hasChild(u1.getId()));

		System.out.println(reg.print(u1, null, null));
		System.out.println(reg);
	}
}

class Resource implements AclEntry {
	String desc;
	String id;

	public Resource(String id) {
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
