package com.rojakcoder.archly;

import java.util.List;

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

		ResourceRegistry reg = new ResourceRegistry();
		String u1 = new String("RES-1");
		String u2 = new String("RES-2");
		String u = new String("RES");
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

		List<String> removed = reg.remove(u1, false);
		Assert.assertEquals(reg.size(), 1);
		Assert.assertEquals(removed.size(), 1);

		//add RES-1 back
		reg.add(u1);
	}

	public void testAddRemoveParents() {
		System.out.println("testAddParents");
		boolean thrown = false;

		ResourceRegistry reg = new ResourceRegistry();
		String u1 = new String("RES-1");
		String u2 = new String("RES-2");

		String u1a = new String("RES-1-A");
		String u1b = new String("RES-1-B");
		String u2a = new String("RES-2-A");
		String u2a1 = new String("RES-2-A-1");
		String u2a1i = new String("RES-2-A-1-i");
		String u1b1 = new String("RES-1-B-1");

		try {
			reg.add(u1a, u1);
		} catch (EntryNotFoundException e) {
			thrown = true;
		}
		Assert.assertTrue(thrown, "Parent does not exist.");

		//add the parents
		reg.add(u1);
		reg.add(u2);
		//add the children
		reg.add(u1a, u1);
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
		List<String> removed = reg.remove(u2, true);
		Assert.assertEquals(reg.size(), 4);
		Assert.assertEquals(removed.size(), 4);

		//remove u1b and expect u1b1 to be under u1
		removed = reg.remove(u1b, false);
		Assert.assertEquals(reg.size(), 3);
		Assert.assertTrue(reg.has(u1b1));
		Assert.assertTrue(reg.hasChild(u1));
		Assert.assertEquals(removed.size(), 1);
		//same element
		Assert.assertEquals(removed.get(0), u1b);

		//remove u1b1 and u1a, and expect u1 to be childless
		reg.remove(u1b1, false);
		reg.remove(u1a, true);
		Assert.assertEquals(reg.size(), 1);
		Assert.assertFalse(reg.hasChild(u1));

		System.out.println(reg.display(new Resource(u1), null, null));
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
