package archly

import (
	"fmt"
	"testing"

	"google.golang.org/appengine/aetest"

	"golang.org/x/net/context"
)

var p = NewPermission(true)

type Case struct {
	title  string
	size   int
	err    error
	prereq func(context.Context, *Registry) error
}

type SimpleEntry struct {
	ID string
}

func (this *SimpleEntry) GetID() string {
	return this.ID
}

func (this *SimpleEntry) GetEntryDesc() string {
	return this.ID
}

func (this *SimpleEntry) RetrieveEntry(resID string) Entry {
	return &SimpleEntry{resID}
}

func TestAddRemoveEntry(t *testing.T) {
	ctx, done, err := aetest.NewContext()
	if err != nil {
		t.Fatal(err)
	}
	defer done()

	cases := make([]Case, 0)
	cases = append(cases, Case{
		"Initial 0",
		0,
		nil,
		nil,
	})

	cases = append(cases, Case{
		"Add res1",
		1,
		nil,
		func(ctx context.Context, reg *Registry) error {
			res1 := "res1"
			return reg.Add(res1)
		},
	})

	cases = append(cases, Case{
		"Add duplicate res1",
		1,
		ErrDupEntry,
		func(ctx context.Context, reg *Registry) error {
			res1 := "res1"
			reg.Add(res1)
			return reg.Add(res1)
		},
	})

	cases = append(cases, Case{
		"Add res2",
		2,
		nil,
		func(ctx context.Context, reg *Registry) error {
			res1 := "res1"
			res2 := "res2"
			reg.Add(res1)
			return reg.Add(res2)
		},
	})

	cases = append(cases, Case{
		"Removing non-existent res1",
		0,
		ErrEntryNotFound,
		func(ctx context.Context, reg *Registry) error {
			res1 := "res1"
			_, err := reg.Remove(res1, false)
			return err
		},
	})

	cases = append(cases, Case{
		"Add res1, res2 then remove res1",
		1,
		nil,
		func(ctx context.Context, reg *Registry) error {
			res1 := "res1"
			res2 := "res2"
			reg.Add(res1)
			reg.Add(res2)
			rem, err := reg.Remove(res1, false)
			if len(rem) != 1 {
				t.Errorf("expect %d resource removed; got %d", 1, 0)
			}
			return err
		},
	})

	for _, c := range cases {
		reg := NewRegistry()
		var err error

		if c.prereq != nil {
			err = c.prereq(ctx, reg)
		}
		if c.err != nil {
			if c.err != err {
				t.Errorf("-- %v\n - expect error '%v'; got '%v'", c.title, c.err, err)
			}
		}
		if c.size != reg.Size() {
			t.Errorf("-- %v\n - expect registry size to be %d; got %d", c.title, c.size, reg.Size())
		}
	}
}

func TestAddRemoveParents(t *testing.T) {
	ctx, done, err := aetest.NewContext()
	if err != nil {
		t.Fatal(err)
	}
	defer done()

	reg := NewRegistry()
	r1 := "res1"
	r2 := "res2"
	r1a := "res1-a"
	r1b := "res1-b"
	r1b1 := "res1-b-1"
	r2a := "res2-a"
	r2a1 := "res2-a-1"
	r2a1i := "res2-a-1-i"

	cases := []struct {
		title  string
		size   int
		err    error
		prereq func(context.Context) error
	}{
		{
			"Parent does not exist",
			0,
			ErrEntryNotFound,
			func(ctx context.Context) error {
				return reg.AddChild(r2, r1)
			},
		},
		{
			"Parents and children added",
			4,
			nil,
			func(ctx context.Context) error {
				if e := reg.Add(r1); e != nil {
					t.Fatal(e)
				}
				if e := reg.Add(r2); e != nil {
					t.Fatal(e)
				}
				//add the children
				if e := reg.AddChild(r1a, r1); e != nil {
					t.Fatal(e)
				}
				if e := reg.AddChild(r1b, r1); e != nil {
					t.Fatal(e)
				}
				return nil
			},
		},
		{
			"Duplicate child",
			4, //no change
			ErrDupEntry,
			func(ctx context.Context) error {
				return reg.AddChild(r1b, r1)
			},
		},
		{
			"Add 2 more children",
			6,
			nil,
			func(ctx context.Context) error {
				if e := reg.AddChild(r2a, r2); e != nil {
					t.Fatal(e)
				}
				if e := reg.AddChild(r1b1, r1b); e != nil {
					t.Fatal(e)
				}
				return nil
			},
		},
		{
			"Add 2 more children",
			8,
			nil,
			func(ctx context.Context) error {
				if e := reg.AddChild(r2a1, r2a); e != nil {
					t.Fatal(e)
				}
				if e := reg.AddChild(r2a1i, r2a1); e != nil {
					t.Fatal(e)
				}
				return nil
			},
		},
		{
			"Remove r2 and all descendants",
			4,
			nil,
			func(ctx context.Context) error {
				rem, err := reg.Remove(r2, true)
				if len(rem) != 4 {
					t.Errorf("expect %d elements to be removed; got %d", 4, len(rem))
				}
				return err
			},
		},
		{
			"Remove r1b and expect r1b1 to be under r1",
			3,
			nil,
			func(ctx context.Context) error {
				rem, err := reg.Remove(r1b, false)
				if len(rem) != 1 {
					t.Errorf("expect %d elements to be removed; got %d", 1, len(rem))
				}
				if !reg.Has(r1b1) {
					t.Errorf("expect registry to contain %v", r1b1)
				}
				if !reg.HasChild(r1) {
					t.Errorf("expect registry to have children for %v", r1)
				}
				if rem[0] != r1b {
					t.Errorf("expect removed element to be %v; got %v", r1b, rem[0])
				}
				return err
			},
		},
		{
			"Remove r1b1 and r1a and expect r1 to be childless",
			1,
			nil,
			func(ctx context.Context) error {
				rem, err := reg.Remove(r1b1, false)
				if len(rem) != 1 {
					t.Errorf("expect %d elements to be removed; got %d", 1, len(rem))
				}
				rem, err = reg.Remove(r1a, true)
				if len(rem) != 1 {
					t.Errorf("expect %d elements to be removed; got %d", 1, len(rem))
				}
				if reg.HasChild(r1) {
					t.Errorf("expect registry to not have children for %v", r1)
				}
				return err
			},
		},
	}

	for _, c := range cases {
		var err error

		if c.prereq != nil {
			err = c.prereq(ctx)
		}
		if c.err != nil && c.err != err {
			t.Errorf("-- %v\n - expect error '%v'; got '%v'", c.title, c.err, err)
		}
		if c.size != reg.Size() {
			t.Errorf("-- %v\n - expect registry size to be %d; got %d", c.title, c.size, reg.Size())
		}
	}

	disp := reg.Display(&SimpleEntry{r1}, "", "")
	dispExp := "- " + r1 + "\n"
	if dispExp != disp {
		t.Errorf("expected Display() to return:\n%v,\ngot:\n%v", dispExp, disp)
	}
	str := reg.String()
	strExp := "\t" + r1 + "\t\t - \t*\n"
	if strExp != str {
		t.Errorf("expected String to return:\n%v,\ngot:\n%v", strExp, str)
	}
}

func TestTraversal(t *testing.T) {
	equals := func(title string, exp, act int) {
		if exp != act {
			t.Errorf("%v: expected %d; got %d", title, exp, act)
		}
	}

	reg := NewRegistry()
	var path []string
	r1 := "RES1"
	r2 := "RES2"
	r11 := "RES1-1"
	r12 := "RES1-2"
	r111 := "RES1-1-1"

	path = reg.TraverseRoot(r1)
	equals("No node", len(path), 1)

	reg.Add(r1)
	path = reg.TraverseRoot(r1)
	equals("Has RES1", len(path), 2)
	path = reg.TraverseRoot("")
	equals("Has RES1 but no node specified", len(path), 1)

	reg.Add(r2)
	path = reg.TraverseRoot(r1)
	equals("Has RES1 & RES2; no change in path", len(path), 2)

	reg.AddChild(r11, r1)
	path = reg.TraverseRoot(r1)
	equals("Added RES1-1; path from parent RES1 is still 2", len(path), 2)
	path = reg.TraverseRoot(r11)
	equals("Added RES1-1; path from RES1-1 is 3", len(path), 3)

	reg.AddChild(r12, r1)
	path = reg.TraverseRoot(r1)
	equals("Added RES1-2; path from parent RES1 is still 2", len(path), 2)
	path = reg.TraverseRoot(r12)
	equals("Added RES1-1; path from RES1-2 is 3", len(path), 3)

	reg.AddChild(r111, r11)
	path = reg.TraverseRoot(r111)
	equals("Added RES1-1-1; path is now 4", len(path), 4)

	disp := reg.Display(&SimpleEntry{r1}, "", "")
	fmt.Printf("\n\n%v", disp)
	fmt.Printf("\n\n%v", reg)
	fmt.Printf("\n\n%v", RegPrintPath(path))
}

func TestPermImportExport(t *testing.T) {
	equals := func(title string, exp, act int) {
		if exp != act {
			t.Errorf("%v: expected %d; got %d", title, exp, act)
		}
	}

	p := NewPermission(true)
	out := p.Export()
	equals("Empty permissions map", 1, len(out))
	p.Clear()
	out = p.Export()
	equals("Empty permissions map", 0, len(out))

	in := map[string]map[string]bool{
		"P1::Q1": {
			"ALL": true,
		},
		"P2::Q1": {
			"ALL": false,
		},
	}

	p.ImportMap(in)
	equals("Imported permissions", 2, p.Size())
}

func TestRegImportExport(t *testing.T) {
	equals := func(title string, exp, act int) {
		if exp != act {
			t.Errorf("%v: expected %d; got %d", title, exp, act)
		}
	}

	reg := NewRegistry()
	out := reg.Export()
	in := map[string]string{
		"ROLE1":     "",
		"ROLE2":     "",
		"ROLE1-1":   "ROLE1",
		"ROLE1-2-1": "ROLE1-2",
		"ROLE1-2":   "ROLE1",
	}

	equals("Empty registry", 0, len(out))

	reg.ImportRegistry(in)
	equals("Imported registry", 5, reg.Size())
	fmt.Printf("\n\n%v", reg.Display(&SimpleEntry{"ROLE1"}, "", ""))
	fmt.Printf("\n\n%v", reg)

	out = reg.Export()
	equals("Exported registry should be same as the import", 5, len(out))
}

func testNull(t *testing.T, title string, b1, b2 bool) {
	if b1 || b2 {
		t.Errorf("%v: expected false, false; got %v, %v", title, b1, b2)
	}
}
func testFalse(t *testing.T, title string, b1, b2 bool) {
	if b1 || !b2 {
		t.Errorf("%v: expected false, true; got %v, %v", title, b1, b2)
	}
}
func testTrue(t *testing.T, title string, b1, b2 bool) {
	if !b1 || b2 {
		t.Errorf("%v: expected true, false; got %v, %v", title, b1, b2)
	}
}

func TestAllPerms(t *testing.T) {
	tpermIsAllowedDenied(t)
	tpermAllow(t)
	tpermDeny(t)
	tpermRemove(t)
	tpermRemoveByResourceRole(t)
}

func tpermIsAllowedDenied(t *testing.T) {
	res1 := "RES1"
	rol1 := "ROL1"
	var aa, bb bool

	//whitelist
	aa, bb = p.IsAllowed(rol1, res1)
	testNull(t, "Empty permissions, actual entities - IsAllowed", aa, bb)
	aa, bb = p.IsDenied(rol1, res1)
	testNull(t, "Empty permissions, actual entities - IsDenied", aa, bb)

	aa, bb = p.IsAllowed(rol1, "")
	testNull(t, "Empty permissions, empty resource - IsAllowed", aa, bb)
	aa, bb = p.IsDenied(rol1, "")
	testNull(t, "Empty permissions, empty resource - IsDenied", aa, bb)

	aa, bb = p.IsAllowed("", res1)
	testNull(t, "Empty permissions, empty role - IsAllowed", aa, bb)
	aa, bb = p.IsDenied("", res1)
	testNull(t, "Empty permissions, empty role - IsDenied", aa, bb)

	aa, bb = p.IsAllowed("", "")
	testFalse(t, "Empty permission, empty entities - IsAllowed", aa, bb)
	aa, bb = p.IsDenied("", "")
	testTrue(t, "Empty permission, empty entities - IsDenied", aa, bb)

	aa, bb = p.IsAllowedAction("", "", 2)
	testFalse(t, "Empty permission, empty entities - IsAllowedAction", aa, bb)
	aa, bb = p.IsDeniedAction("", "", 2)
	testTrue(t, "Empty permission, empty entities - IsDeniedAction", aa, bb)

	//blacklist
	p = NewPermission(false)
	aa, bb = p.IsAllowed(rol1, res1)
	testNull(t, "Empty permissions, actual entities, blacklist - IsAllowed", aa, bb)
	aa, bb = p.IsDenied(rol1, res1)
	testNull(t, "Empty permissions, actual entities, blacklist - IsDenied", aa, bb)

	aa, bb = p.IsAllowed(rol1, "")
	testNull(t, "Empty permissions, empty resource, blacklist - IsAllowed", aa, bb)
	aa, bb = p.IsDenied(rol1, "")
	testNull(t, "Empty permissions, empty resource, blacklist - IsDenied", aa, bb)

	aa, bb = p.IsAllowed("", res1)
	testNull(t, "Empty permissions, empty role, blacklist - IsAllowed", aa, bb)
	aa, bb = p.IsDenied("", res1)
	testNull(t, "Empty permissions, empty role, blacklist - IsDenied", aa, bb)

	aa, bb = p.IsAllowed("", "")
	testTrue(t, "Empty permission, empty entities, blacklist - IsAllowed", aa, bb)
	aa, bb = p.IsDenied("", "")
	testFalse(t, "Empty permission, empty entities, blacklist - IsDenied", aa, bb)

	aa, bb = p.IsAllowedAction("", "", 2)
	testTrue(t, "Empty permission, empty entities, blacklist - IsAllowedAction", aa, bb)
	aa, bb = p.IsDeniedAction("", "", 2)
	testFalse(t, "Empty permission, empty entities, blacklist - IsDeniedAction", aa, bb)
}

func tpermAllow(t *testing.T) {
	//p := NewPermission(true)
	res1 := "RES1"
	rol1 := "ROL1"
	var aa, bb bool

	aa, bb = p.IsAllowed(rol1, res1)
	testNull(t, "tpermAllow - IsAllowed", aa, bb)
	aa, bb = p.IsDenied(rol1, res1)
	testNull(t, "tpermAllow - IsDenied", aa, bb)

	p.Allow(rol1, res1)
	aa, bb = p.IsAllowed(rol1, res1)
	testTrue(t, "Allow 1:1; allowed - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 2)
	testTrue(t, "Allow 1:1; allowed - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 3)
	testTrue(t, "Allow 1:1; allowed - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 4)
	testTrue(t, "Allow 1:1; allowed - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 5)
	testTrue(t, "Allow 1:1; allowed - IsAllowedAction DELETE", aa, bb)
	//false because explicit allow
	aa, bb = p.IsDenied(rol1, res1)
	testFalse(t, "Allow 1:1; allowed - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 2)
	testFalse(t, "Allow 1:1; allowed - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 3)
	testFalse(t, "Allow 1:1; allowed - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 4)
	testFalse(t, "Allow 1:1; allowed - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 5)
	testFalse(t, "Allow 1:1; allowed - IsDeniedAction DELETE", aa, bb)

	p.DenyAction(rol1, res1, PERMTYPE_UPDATE)
	p.DenyAction(rol1, res1, PERMTYPE_DELETE)
	aa, bb = p.IsAllowed(rol1, res1)
	testFalse(t, "Allow 1:1; allowed, deny UD - IsAllowed (no longer all true)", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_CREATE)
	testTrue(t, "Allow 1:1; allowed, deny UD - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_READ)
	testTrue(t, "Allow 1:1; allowed, deny UD - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_UPDATE)
	testFalse(t, "Allow 1:1; allowed, deny UD - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_DELETE)
	testFalse(t, "Allow 1:1; allowed, deny UD - IsAllowedAction DELETE", aa, bb)
	aa, bb = p.IsDenied(rol1, res1)
	testFalse(t, "Allow 1:1; allowed, deny UD - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_CREATE)
	testFalse(t, "Allow 1:1; allowed, deny UD - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_READ)
	testFalse(t, "Allow 1:1; allowed, deny UD - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_UPDATE)
	testTrue(t, "Allow 1:1; allowed, deny UD - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_DELETE)
	testTrue(t, "Allow 1:1; allowed, deny UD - IsDeniedAction DELETE", aa, bb)

	p.RemoveAction(rol1, res1, PERMTYPE_UPDATE)
	aa, bb = p.IsAllowed(rol1, res1)
	testFalse(t, "Allow 1:1; allowed, deny D - IsAllowed (no longer all true)", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_CREATE)
	testTrue(t, "Allow 1:1; allowed, deny D - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_READ)
	testTrue(t, "Allow 1:1; allowed, deny D - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_UPDATE)
	testTrue(t, "Allow 1:1; allowed, deny D - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_DELETE)
	testFalse(t, "Allow 1:1; allowed, deny D - IsAllowedAction DELETE", aa, bb)
	aa, bb = p.IsDenied(rol1, res1)
	testFalse(t, "Allow 1:1; allowed, deny D - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_CREATE)
	testFalse(t, "Allow 1:1; allowed, deny D - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_READ)
	testFalse(t, "Allow 1:1; allowed, deny D - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_UPDATE)
	testFalse(t, "Allow 1:1; allowed, deny D - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_DELETE)
	testTrue(t, "Allow 1:1; allowed, deny D - IsDeniedAction DELETE", aa, bb)

	p.Remove(rol1, res1)
	aa, bb = p.IsAllowed(rol1, res1)
	testNull(t, "Allow 1:1; removed - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_CREATE)
	testNull(t, "Allow 1:1; removed - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_READ)
	testNull(t, "Allow 1:1; removed - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_UPDATE)
	testNull(t, "Allow 1:1; removed - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_DELETE)
	testNull(t, "Allow 1:1; removed - IsAllowedAction DELETE", aa, bb)
	aa, bb = p.IsDenied(rol1, res1)
	testNull(t, "Allow 1:1; removed - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_CREATE)
	testNull(t, "Allow 1:1; removed - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_READ)
	testNull(t, "Allow 1:1; removed - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_UPDATE)
	testNull(t, "Allow 1:1; removed - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_DELETE)
	testNull(t, "Allow 1:1; removed - IsDeniedAction DELETE", aa, bb)

	p.DenyAction(rol1, res1, PERMTYPE_DELETE)
	//ALL is already removed, DELETE is false, others are NULL
	aa, bb = p.IsAllowed(rol1, res1)
	testFalse(t, "Allow 1:1;, deny D - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_CREATE)
	testNull(t, "Allow 1:1;, deny D - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_READ)
	testNull(t, "Allow 1:1;, deny D - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_UPDATE)
	testNull(t, "Allow 1:1;, deny D - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_DELETE)
	testFalse(t, "Allow 1:1;, deny D - IsAllowedAction DELETE", aa, bb)
	//no explicit deny on ALL, explicit deny on DELETE
	aa, bb = p.IsDenied(rol1, res1)
	testNull(t, "Allow 1:1;, deny D - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_CREATE)
	testNull(t, "Allow 1:1;, deny D - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_READ)
	testNull(t, "Allow 1:1;, deny D - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_UPDATE)
	testNull(t, "Allow 1:1;, deny D - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_DELETE)
	testTrue(t, "Allow 1:1;, deny D - IsDeniedAction DELETE", aa, bb)

	p.AllowAction(rol1, res1, PERMTYPE_CREATE)
	p.AllowAction(rol1, res1, PERMTYPE_READ)
	p.AllowAction(rol1, res1, PERMTYPE_DELETE)
	//no explicit deny so ALL is NULL
	aa, bb = p.IsAllowed(rol1, res1)
	testNull(t, "Allow 1:1;, allow CRD - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_CREATE)
	testTrue(t, "Allow 1:1;, allow CRD - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_READ)
	testTrue(t, "Allow 1:1;, allow CRD - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_UPDATE)
	testNull(t, "Allow 1:1;, allow CRD - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, PERMTYPE_DELETE)
	testTrue(t, "Allow 1:1;, allow CRD - IsAllowedAction DELETE", aa, bb)
	//no explicit deny on ALL, explicit allow on some
	aa, bb = p.IsDenied(rol1, res1)
	testFalse(t, "Allow 1:1;, allow CRD - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_CREATE)
	testFalse(t, "Allow 1:1;, allow CRD - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_READ)
	testFalse(t, "Allow 1:1;, allow CRD - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_UPDATE)
	testNull(t, "Allow 1:1;, allow CRD - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, PERMTYPE_DELETE)
	testFalse(t, "Allow 1:1;, allow CRD - IsDeniedAction DELETE", aa, bb)

	//equivalent of ALL allow
	p.AllowAction(rol1, res1, PERMTYPE_UPDATE)
	aa, bb = p.IsAllowed(rol1, res1)
	testTrue(t, "Allow 1:1; allow CRUD - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 2)
	testTrue(t, "Allow 1:1; allow CRUD - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 3)
	testTrue(t, "Allow 1:1; allow CRUD - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 4)
	testTrue(t, "Allow 1:1; allow CRUD - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 5)
	testTrue(t, "Allow 1:1; allow CRUD - IsAllowedAction DELETE", aa, bb)
	//false because explicit allow
	aa, bb = p.IsDenied(rol1, res1)
	testFalse(t, "Allow 1:1; allow CRUD - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 2)
	testFalse(t, "Allow 1:1; allow CRUD - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 3)
	testFalse(t, "Allow 1:1; allow CRUD - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 4)
	testFalse(t, "Allow 1:1; allow CRUD - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 5)
	testFalse(t, "Allow 1:1; allow CRUD - IsDeniedAction DELETE", aa, bb)
}

func tpermDeny(t *testing.T) {
	//p := NewPermission(true)
	res1 := "RESA"
	rol1 := "ROLA"
	var aa, bb bool

	aa, bb = p.IsAllowed(rol1, res1)
	testNull(t, "tpermDeny - IsAllowed", aa, bb)
	aa, bb = p.IsDenied(rol1, res1)
	testNull(t, "tpermDeny - IsDenied", aa, bb)

	p.Allow(rol1, res1)
	aa, bb = p.IsAllowed(rol1, res1)
	testTrue(t, "Deny 1:1; allowed - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 2)
	testTrue(t, "Deny 1:1; allowed - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 3)
	testTrue(t, "Deny 1:1; allowed - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 4)
	testTrue(t, "Deny 1:1; allowed - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 5)
	testTrue(t, "Deny 1:1; allowed - IsAllowedAction DELETE", aa, bb)
	aa, bb = p.IsDenied(rol1, res1)
	testFalse(t, "Deny 1:1; allowed - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 2)
	testFalse(t, "Deny 1:1; allowed - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 3)
	testFalse(t, "Deny 1:1; allowed - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 4)
	testFalse(t, "Deny 1:1; allowed - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 5)
	testFalse(t, "Deny 1:1; allowed - IsDeniedAction DELETE", aa, bb)

	p.Deny(rol1, res1)
	aa, bb = p.IsAllowed(rol1, res1)
	testFalse(t, "Deny 1:1; denied - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 2)
	testFalse(t, "Deny 1:1; denied - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 3)
	testFalse(t, "Deny 1:1; denied - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 4)
	testFalse(t, "Deny 1:1; denied - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 5)
	testFalse(t, "Deny 1:1; denied - IsAllowedAction DELETE", aa, bb)
	aa, bb = p.IsDenied(rol1, res1)
	testTrue(t, "Deny 1:1; denied - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 2)
	testTrue(t, "Deny 1:1; denied - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 3)
	testTrue(t, "Deny 1:1; denied - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 4)
	testTrue(t, "Deny 1:1; denied - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 5)
	testTrue(t, "Deny 1:1; denied - IsDeniedAction DELETE", aa, bb)

	p.AllowAction(rol1, res1, PERMTYPE_UPDATE)
	p.AllowAction(rol1, res1, PERMTYPE_DELETE)
	//false because there is an explicit deny on ALL
	aa, bb = p.IsAllowed(rol1, res1)
	testFalse(t, "Deny 1:1; denied, allow UD - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 2)
	testFalse(t, "Deny 1:1; denied, allow UD - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 3)
	testFalse(t, "Deny 1:1; denied, allow UD - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 4)
	testTrue(t, "Deny 1:1; denied, allow UD - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 5)
	testTrue(t, "Deny 1:1; denied, allow UD - IsAllowedAction DELETE", aa, bb)
	aa, bb = p.IsDenied(rol1, res1) //no longer ALL true
	testFalse(t, "Deny 1:1; denied, allow UD - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 2)
	testTrue(t, "Deny 1:1; denied, allow UD - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 3)
	testTrue(t, "Deny 1:1; denied, allow UD - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 4)
	testFalse(t, "Deny 1:1; denied, allow UD - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 5)
	testFalse(t, "Deny 1:1; denied, allow UD - IsDeniedAction DELETE", aa, bb)

	p.RemoveAction(rol1, res1, PERMTYPE_UPDATE)
	//false because there is an explicit deny on ALL
	aa, bb = p.IsAllowed(rol1, res1)
	testFalse(t, "Deny 1:1; denied, allow D - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 2)
	testFalse(t, "Deny 1:1; denied, allow D - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 3)
	testFalse(t, "Deny 1:1; denied, allow D - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 4)
	testFalse(t, "Deny 1:1; denied, allow D - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 5)
	testTrue(t, "Deny 1:1; denied, allow D - IsAllowedAction DELETE", aa, bb)
	aa, bb = p.IsDenied(rol1, res1)
	testFalse(t, "Deny 1:1; denied, allow D - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 2)
	testTrue(t, "Deny 1:1; denied, allow D - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 3)
	testTrue(t, "Deny 1:1; denied, allow D - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 4)
	testTrue(t, "Deny 1:1; denied, allow D - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 5)
	testFalse(t, "Deny 1:1; denied, allow D - IsDeniedAction DELETE", aa, bb)

	p.Remove(rol1, res1)
	aa, bb = p.IsAllowed(rol1, res1)
	testNull(t, "Deny 1:1; removed - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 2)
	testNull(t, "Deny 1:1; removed - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 3)
	testNull(t, "Deny 1:1; removed - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 4)
	testNull(t, "Deny 1:1; removed - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 5)
	testNull(t, "Deny 1:1; removed - IsAllowedAction DELETE", aa, bb)
	aa, bb = p.IsDenied(rol1, res1) //now NULL since removed
	testNull(t, "Deny 1:1; removed - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 2)
	testNull(t, "Deny 1:1; removed - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 3)
	testNull(t, "Deny 1:1; removed - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 4)
	testNull(t, "Deny 1:1; removed - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 5)
	testNull(t, "Deny 1:1; removed - IsDeniedAction DELETE", aa, bb)

	p.AllowAction(rol1, res1, PERMTYPE_DELETE)
	//no explicity deny so ALL is NULL
	aa, bb = p.IsAllowed(rol1, res1)
	testNull(t, "Deny 1:1; allow D - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 2)
	testNull(t, "Deny 1:1; allow D - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 3)
	testNull(t, "Deny 1:1; allow D - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 4)
	testNull(t, "Deny 1:1; allow D - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 5)
	testTrue(t, "Deny 1:1; allow D - IsAllowedAction DELETE", aa, bb)
	//explicit allow on DELETE so false
	aa, bb = p.IsDenied(rol1, res1)
	testFalse(t, "Deny 1:1; allow D - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 2)
	testNull(t, "Deny 1:1; allow D - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 3)
	testNull(t, "Deny 1:1; allow D - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 4)
	testNull(t, "Deny 1:1; allow D - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 5)
	testFalse(t, "Deny 1:1; allow D - IsDeniedAction DELETE", aa, bb)

	p.DenyAction(rol1, res1, PERMTYPE_CREATE)
	p.DenyAction(rol1, res1, PERMTYPE_READ)
	p.DenyAction(rol1, res1, PERMTYPE_DELETE)
	//false because of explicit deny
	aa, bb = p.IsAllowed(rol1, res1)
	testFalse(t, "Deny 1:1; deny CRD - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 2)
	testFalse(t, "Deny 1:1; deny CRD - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 3)
	testFalse(t, "Deny 1:1; deny CRD - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 4)
	testNull(t, "Deny 1:1; deny CRD - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 5)
	testFalse(t, "Deny 1:1; deny CRD - IsAllowedAction DELETE", aa, bb)
	aa, bb = p.IsDenied(rol1, res1)
	testNull(t, "Deny 1:1; deny CRD - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 2)
	testTrue(t, "Deny 1:1; deny CRD - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 3)
	testTrue(t, "Deny 1:1; deny CRD - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 4)
	testNull(t, "Deny 1:1; deny CRD - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 5)
	testTrue(t, "Deny 1:1; deny CRD - IsDeniedAction DELETE", aa, bb)

	//equivalent of ALL deny
	p.DenyAction(rol1, res1, PERMTYPE_UPDATE)
	aa, bb = p.IsAllowed(rol1, res1)
	testFalse(t, "Deny 1:1; allow CURD - IsAllowed", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 2)
	testFalse(t, "Deny 1:1; allow CURD - IsAllowedAction CREATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 3)
	testFalse(t, "Deny 1:1; allow CURD - IsAllowedAction READ", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 4)
	testFalse(t, "Deny 1:1; allow CURD - IsAllowedAction UPDATE", aa, bb)
	aa, bb = p.IsAllowedAction(rol1, res1, 5)
	testFalse(t, "Deny 1:1; allow CURD - IsAllowedAction DELETE", aa, bb)
	aa, bb = p.IsDenied(rol1, res1)
	testTrue(t, "Deny 1:1; allow CURD - IsDenied", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 2)
	testTrue(t, "Deny 1:1; allow CURD - IsDeniedAction CREATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 3)
	testTrue(t, "Deny 1:1; allow CURD - IsDeniedAction READ", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 4)
	testTrue(t, "Deny 1:1; allow CURD - IsDeniedAction UPDATE", aa, bb)
	aa, bb = p.IsDeniedAction(rol1, res1, 5)
	testTrue(t, "Deny 1:1; allow CURD - IsDeniedAction DELETE", aa, bb)
}

func tpermRemove(t *testing.T) {
	//p := NewPermission(true)
	res1 := "RES1"
	rol1 := "ROL1"
	na := "na"
	var e error

	testNil := func(title string, err error) {
		if err != nil {
			t.Errorf("%v: expected nil error; got %v", title, err)
		}
	}
	testErr := func(title string, err error) {
		if err == nil {
			t.Errorf("%v: expected non-nil error; got nil", title)
		}
	}

	//non-existing role & resource
	e = p.Remove(na, na)
	testErr("NA role, NA resource", e)
	e = p.Remove(rol1, na)
	testErr("NA resource", e)
	e = p.Remove(na, res1)
	testErr("NA role", e)

	e = p.RemoveAction(na, na, PERMTYPE_CREATE)
	testErr("NA role, NA resource; CREATE", e)
	e = p.RemoveAction(rol1, na, PERMTYPE_CREATE)
	testErr("NA resource; CREATE", e)
	e = p.RemoveAction(na, res1, PERMTYPE_CREATE)
	testErr("NA role; CREATE", e)

	e = p.RemoveAction(rol1, res1, PERMTYPE_CREATE)
	testNil("RemoveAction ROL1, RES1; CREATE - removing first time", e) //bcos cumulative
	e = p.RemoveAction(rol1, res1, PERMTYPE_CREATE)
	testErr("RemoveAction ROL1, RES1; CREATE - removing again", e) //exception when repeated

	//remove the root privileges
	e = p.Remove("", "")
	testNil("Remove root privileges", e)
	//trying to remove the root but already removed
	e = p.Remove("", "")
	testErr("Error when repeating removal of root privileges", e)
}

func tpermRemoveByResourceRole(t *testing.T) {
	testNil := func(title string, err error) {
		if err != nil {
			t.Errorf("%v: expected nil error; got %v", title, err)
		}
	}
	testSize := func(title string, act, exp int) {
		if exp != act {
			t.Errorf("%v: expected %d; got %d", title, exp, act)
		}
	}
	p.Clear()
	resources := []string{"Q1", "Q2", "Q3", "Q4"}
	roles := []string{"P1", "P2", "P3", "P4"}

	testSize("Empty permissions", p.Size(), 0)
	//create mappings for each key pair
	for _, res := range resources {
		for _, rol := range roles {
			p.Allow(rol, res)
		}
	}
	testSize("4x4 for each key pair", p.Size(), 16)

	for _, res := range resources {
		p.Allow("", res)
	}
	testSize("4x4 + 4: Add ALL access", p.Size(), 20)
	for _, rol := range roles {
		p.Allow(rol, "")
	}
	testSize("4x4+4 + 4: Add ALL access", p.Size(), 24)

	var removed int

	removed = p.RemoveByResource(resources[3])
	testSize("Remove all access on Q4", p.Size(), 19)
	testSize("Removed access on Q4", removed, 5)

	removed = p.RemoveByResource(resources[3])
	testSize("Repeated resource removal has no effect", p.Size(), 19)
	testSize("Repeated resource removal should yield 0", removed, 0)

	removed = p.RemoveByRole(roles[3])
	testSize("Remove all access from P4 (Q4 already removed)", p.Size(), 15)
	testSize("Removed access from P4", removed, 4)

	removed = p.RemoveByRole(roles[3])
	testSize("Repeated role removal has no effect", p.Size(), 15)
	testSize("Repeated role removal should yield 0", removed, 0)

	var err error
	err = p.RemoveAction(roles[2], resources[2], PERMTYPE_UPDATE)
	testNil("Removing UPDATE permission", err)
	err = p.RemoveAction(roles[2], resources[2], PERMTYPE_DELETE)
	testNil("Removing DELETE permission", err)
	err = p.RemoveAction(roles[2], resources[2], PERMTYPE_CREATE)
	testNil("Removing CREATE permission", err)
	err = p.RemoveAction(roles[2], resources[2], PERMTYPE_READ)
	testNil("Removing READ permission", err)
	testSize("Removing all CRUD access should remove the entry", p.Size(), 14)

	fmt.Printf("\n\n%v\n\n", p.String())
}
