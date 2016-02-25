package com.rojakcoder.archly;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.rojakcoder.archly.exceptions.DuplicateEntryException;

public class RoleRegistryTest {
	@Test(priority = 1)
	public void testTraversal() throws DuplicateEntryException {
		RoleRegistry reg = RoleRegistry.getSingleton();
		List<String> path = null;
		Role r1 = new Role("ROLE-1");
		Role r2 = new Role("ROLE-2");
		Role r11 = new Role("ROLE-1-1");
		Role r12 = new Role("ROLE-1-2");
		Role r111 = new Role("ROLE-1-1-1");

		path = reg.traverseRoot(r1);
		Assert.assertEquals(path.size(), 1);

		reg.add(r1);
		path = reg.traverseRoot(r1);
		Assert.assertEquals(path.size(), 2);

		reg.add(r2);
		path = reg.traverseRoot(r1);
		Assert.assertEquals(path.size(), 2);

		reg.add(r11, r1);
		path = reg.traverseRoot(r1);
		Assert.assertEquals(path.size(), 2);
		path = reg.traverseRoot(r11);
		Assert.assertEquals(path.size(), 3);

		reg.add(r12, r1);
		path = reg.traverseRoot(r1);
		Assert.assertEquals(path.size(), 2);
		path = reg.traverseRoot(r12);
		Assert.assertEquals(path.size(), 3);

		reg.add(r111, r11);
		path = reg.traverseRoot(r111);
		Assert.assertEquals(path.size(), 4);

		System.out.println(reg.print(r1, null, null));
		System.out.println(reg);
		System.out.println(Registry.print(path));
	}
}

class Role implements AclEntry {
	String desc;
	String id;

	public Role(String id) {
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
