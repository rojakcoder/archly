package archly

import (
	"fmt"
	"strconv"
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

func TestRegAddRemoveEntry(t *testing.T) {
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

func TestRegAddRemoveParents(t *testing.T) {
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

func TestRegTraversal(t *testing.T) {
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

func TestAllAcl(t *testing.T) {
	acl := NewAcl()
	if acl.IsAllowed(nil, nil) != false {
		t.Errorf("Default: expect IsAllowed() to return false; got true")
	}
	acl.MakeDefaultAllow()
	if acl.IsAllowed(nil, nil) != true {
		t.Errorf("MakeDefaultAllow: expect IsAllowed() to return true; got false")
	}

	//restore to default false
	acl.MakeDefaultDeny()
	if acl.IsAllowed(nil, nil) != false {
		t.Errorf("MakeDefaultDeny: expect IsAllowed() to return false; got true")
	}

	taclResource(t)
	taclRole(t)
	taclAllow(t)
	taclDeny(t)
	taclRemove(t)
	taclHierarchy(t)
	taclRemoveNull(t)
	taclRemoveResourceRole(t)
}

func taclResource(t *testing.T) {
	acl := NewAcl()
	res1 := &SimpleEntry{"ACO-1"}
	res1a := &SimpleEntry{"ACO-1-A"}
	res2 := &SimpleEntry{"ACO-2"}
	res2a := &SimpleEntry{"ACO-2-A"}
	var e error

	testNil := func(title string, err error) {
		if err != nil {
			t.Errorf("%v: expected nil error; got %v", title, err)
		}
	}
	testErrDup := func(title string, err error) {
		if err == nil {
			t.Errorf("%v: expected non-nil error; got nil", title)
		}
		if err != ErrDupEntry {
			t.Errorf("%v: expected ErrDupEntry error; got %v", title, err)
		}
	}
	testErrMissing := func(title string, err error) {
		if err == nil {
			t.Errorf("%v: expected non-nil error; got nil", title)
		}
		if err != ErrEntryNotFound {
			t.Errorf("%v: expected ErrEntryNotFound error; got %v", title, err)
		}
	}

	e = acl.AddResource(res1)
	testNil("AddResource (1) - 1st entry", e)
	e = acl.AddResource(res1)
	testErrDup("AddResource (2) - Duplicate 1st entry", e)

	e = acl.AddResourceParent(res1a, res1)
	testNil("AddResourceParent (1) - 2nd entry", e)
	e = acl.AddResource(res1a)
	testErrDup("AddResourceParent (2) - Duplicate 2nd entry", e)
	e = acl.AddResourceParent(res1a, res1)
	testErrDup("AddResourceParent (3) - Duplicate 2nd child entry", e)

	e = acl.AddResourceParent(res2a, res2)
	testErrMissing("AddResourceParent (1) - Missing parent entry", e)
}

func taclRole(t *testing.T) {
	acl := NewAcl()
	rol1 := &SimpleEntry{"ARO-1"}
	rol1a := &SimpleEntry{"ARO-1-A"}
	rol2 := &SimpleEntry{"ARO-2"}
	rol2a := &SimpleEntry{"ARO-2-A"}
	var e error

	testNil := func(title string, err error) {
		if err != nil {
			t.Errorf("%v: expected nil error; got %v", title, err)
		}
	}
	testErrDup := func(title string, err error) {
		if err == nil {
			t.Errorf("%v: expected non-nil error; got nil", title)
		}
		if err != ErrDupEntry {
			t.Errorf("%v: expected ErrDupEntry error; got %v", title, err)
		}
	}
	testErrMissing := func(title string, err error) {
		if err == nil {
			t.Errorf("%v: expected non-nil error; got nil", title)
		}
		if err != ErrEntryNotFound {
			t.Errorf("%v: expected ErrEntryNotFound error; got %v", title, err)
		}
	}

	e = acl.AddRole(rol1)
	testNil("AddRole (1) - 1st entry", e)
	e = acl.AddRole(rol1)
	testErrDup("AddRole (2) - Duplicate 1st entry", e)

	e = acl.AddRoleParent(rol1a, rol1)
	testNil("AddRoleParent (1) - 2nd entry", e)
	e = acl.AddRole(rol1a)
	testErrDup("AddRoleParent (2) - Duplicate 2nd entry", e)
	e = acl.AddRoleParent(rol1a, rol1)
	testErrDup("AddRoleParent (3) - Duplicate 2nd child entry", e)

	e = acl.AddRoleParent(rol2a, rol2)
	testErrMissing("AddRoleParent (1) - Missing parent entry", e)
}

func taclAllow(t *testing.T) {
	acl := NewAcl()
	res0 := &SimpleEntry{"ACO-0"}
	res1 := &SimpleEntry{"ACO-1"}
	res2 := &SimpleEntry{"ACO-2"}
	rol0 := &SimpleEntry{"ARO-0"}
	rol1 := &SimpleEntry{"ARO-1"}
	rol2 := &SimpleEntry{"ARO-2"}
	testTrue := func(title string, res bool) {
		if res != true {
			t.Errorf("%v: expected true; got false", title)
		}
	}
	testFalse := func(title string, res bool) {
		if res != false {
			t.Errorf("%v: expected false; got true", title)
		}
	}

	//grant rol1 to res1
	testFalse("IsAllowed (1)", acl.IsAllowed(rol1, res1))
	acl.Allow(rol1, res1)
	testTrue("IsAllowed (2)", acl.IsAllowed(rol1, res1))

	//test all access grant
	testFalse("IsAllowed (3a)", acl.IsAllowed(rol0, res1))
	testFalse("IsAllowed (3b)", acl.IsAllowed(rol0, res2))
	acl.AllowAllResource(rol0)
	testTrue("IsAllowed (3c)", acl.IsAllowed(rol0, res1))
	testTrue("IsAllowed (3d)", acl.IsAllowed(rol0, res2))
	testFalse("IsAllowed (4a)", acl.IsAllowed(rol1, res0))
	testFalse("IsAllowed (4b)", acl.IsAllowed(rol2, res0))
	acl.AllowAllRole(res0)
	testTrue("IsAllowed (4c)", acl.IsAllowed(rol1, res0))
	testTrue("IsAllowed (4d)", acl.IsAllowed(rol2, res0))

	//test specific grant
	testFalse("IsAllowedAction (5a)", acl.IsAllowedAction(rol2, res2, PERMTYPE_CREATE))
	testFalse("IsAllowedAction (5b)", acl.IsAllowedAction(rol2, res2, PERMTYPE_READ))
	acl.AllowAction(rol2, res2, PERMTYPE_CREATE)
	testTrue("IsAllowedAction (5c)", acl.IsAllowedAction(rol2, res2, PERMTYPE_CREATE))
	testFalse("IsAllowedAction (5d)", acl.IsAllowedAction(rol2, res2, PERMTYPE_READ))
}

func taclDeny(t *testing.T) {
	acl := NewAcl()
	res0 := &SimpleEntry{"ACO-ZZ"}
	res1 := &SimpleEntry{"ACO-A"}
	res2 := &SimpleEntry{"ACO-B"}
	rol0 := &SimpleEntry{"ARO-ZZ"}
	rol1 := &SimpleEntry{"ARO-A"}
	rol2 := &SimpleEntry{"ARO-B"}
	testTrue := func(title string, res bool) {
		if res != true {
			t.Errorf("%v: expected true; got false", title)
		}
	}
	testFalse := func(title string, res bool) {
		if res != false {
			t.Errorf("%v: expected false; got true", title)
		}
	}

	//make default allow otherwise isDenied will also return false if not explicitly denied
	acl.MakeDefaultAllow()

	//deny role1 to res1
	testFalse("IsDenied (1)", acl.IsDenied(rol1, res1))
	acl.Deny(rol1, res1)
	testTrue("IsDenied (2)", acl.IsDenied(rol1, res1))

	//test all access deny
	testFalse("IsDenied (3a)", acl.IsDenied(rol0, res1))
	testFalse("IsDenied (3b)", acl.IsDenied(rol0, res2))
	acl.DenyAllResource(rol0)
	testTrue("IsDenied (3c)", acl.IsDenied(rol0, res1))
	testTrue("IsDenied (3d)", acl.IsDenied(rol0, res2))
	testFalse("IsDenied (4a)", acl.IsDenied(rol1, res0))
	testFalse("IsDenied (4b)", acl.IsDenied(rol2, res0))
	acl.DenyAllRole(res0)
	testTrue("IsDenied (4c)", acl.IsDenied(rol1, res0))
	testTrue("IsDenied (4d)", acl.IsDenied(rol2, res0))

	//test specific deny
	testFalse("IsDeniedAction (5a)", acl.IsDeniedAction(rol2, res2, PERMTYPE_CREATE))
	testFalse("IsDeniedAction (5b)", acl.IsDeniedAction(rol2, res2, PERMTYPE_READ))
	acl.DenyAction(rol2, res2, PERMTYPE_CREATE)
	testTrue("IsDeniedAction (5c)", acl.IsDeniedAction(rol2, res2, PERMTYPE_CREATE))
	testFalse("IsDeniedAction (5d)", acl.IsDeniedAction(rol2, res2, PERMTYPE_READ))
}

func taclRemove(t *testing.T) {
	acl := NewAcl()
	res1 := &SimpleEntry{"ACO-A"}
	res2 := &SimpleEntry{"ACO-B"}
	rol1 := &SimpleEntry{"ARO-A"}
	rol2 := &SimpleEntry{"ARO-B"}
	testTrue := func(title string, res bool) {
		if res != true {
			t.Errorf("%v: expected true; got false", title)
		}
	}
	testFalse := func(title string, res bool) {
		if res != false {
			t.Errorf("%v: expected false; got true", title)
		}
	}

	//replicate the settings in taclDeny
	acl.MakeDefaultAllow()
	acl.Deny(rol1, res1)
	acl.DenyAction(rol2, res2, PERMTYPE_CREATE)

	testTrue("Remove (1a)", acl.IsDeniedAction(rol2, res2, PERMTYPE_CREATE))
	testFalse("Remove (1b)", acl.IsDenied(rol2, res2))
	acl.RemoveAction(rol2, res2, PERMTYPE_CREATE)
	testFalse("Remove (1c)", acl.IsDeniedAction(rol2, res2, PERMTYPE_CREATE))
	testFalse("Remove (1d)", acl.IsDenied(rol2, res2))
	_, has := acl.Perms.Perms[rol2.GetID()+"::"+res2.GetID()]
	testFalse("Remove (1e)", has)

	testTrue("Remove (2a)", acl.IsDeniedAction(rol1, res1, PERMTYPE_CREATE))
	testTrue("Remove (2b)", acl.IsDeniedAction(rol1, res1, PERMTYPE_READ))
	testTrue("Remove (2c)", acl.IsDeniedAction(rol1, res1, PERMTYPE_UPDATE))
	testTrue("Remove (2d)", acl.IsDeniedAction(rol1, res1, PERMTYPE_DELETE))
	testTrue("Remove (2e)", acl.IsDenied(rol1, res1))
	acl.RemoveAction(rol1, res1, PERMTYPE_CREATE)
	testFalse("Remove (2f)", acl.IsDeniedAction(rol1, res1, PERMTYPE_CREATE))
	testTrue("Remove (2g)", acl.IsDeniedAction(rol1, res1, PERMTYPE_READ))
	testTrue("Remove (2h)", acl.IsDeniedAction(rol1, res1, PERMTYPE_UPDATE))
	testTrue("Remove (2i)", acl.IsDeniedAction(rol1, res1, PERMTYPE_DELETE))
	testFalse("Remove (2j)", acl.IsDenied(rol1, res1))
	acl.Remove(rol1, res1)
	testFalse("Remove (2a)", acl.IsDeniedAction(rol1, res1, PERMTYPE_CREATE))
	testFalse("Remove (2b)", acl.IsDeniedAction(rol1, res1, PERMTYPE_READ))
	testFalse("Remove (2c)", acl.IsDeniedAction(rol1, res1, PERMTYPE_UPDATE))
	testFalse("Remove (2d)", acl.IsDeniedAction(rol1, res1, PERMTYPE_DELETE))
	testFalse("Remove (2e)", acl.IsDenied(rol1, res1))
}

func taclHierarchy(t *testing.T) {
	acl := NewAcl()
	res1 := &SimpleEntry{"ACO-1"}
	res2 := &SimpleEntry{"ACO-2"}
	res3 := &SimpleEntry{"ACO-3"}
	//res4 := &SimpleEntry{"ACO-4"}
	res1a := &SimpleEntry{"ACO-1-A"}
	res1b := &SimpleEntry{"ACO-1-B"}
	res1c := &SimpleEntry{"ACO-1-C"}
	res1a1 := &SimpleEntry{"ACO-1-A-1"}
	res1a2 := &SimpleEntry{"ACO-1-A-2"}
	res1b1 := &SimpleEntry{"ACO-1-B-1"}
	res1c1 := &SimpleEntry{"ACO-1-C-1"}

	rol1 := &SimpleEntry{"ARO-1"}
	rol2 := &SimpleEntry{"ARO-2"}
	rol3 := &SimpleEntry{"ARO-3"}
	rol4 := &SimpleEntry{"ARO-4"}
	rol1a := &SimpleEntry{"ARO-1-A"}
	rol1b := &SimpleEntry{"ARO-1-B"}
	rol1c := &SimpleEntry{"ARO-1-C"}
	rol1a1 := &SimpleEntry{"ARO-1-A-1"}
	rol1a2 := &SimpleEntry{"ARO-1-A-2"}
	rol1b1 := &SimpleEntry{"ARO-1-B-1"}

	testTrue := func(title string, res bool) {
		if res != true {
			t.Errorf("%v: expected true; got false", title)
		}
	}
	testFalse := func(title string, res bool) {
		if res != false {
			t.Errorf("%v: expected false; got true", title)
		}
	}

	acl.AddResource(res1)
	acl.AddResource(res2)
	acl.AddResourceParent(res1a, res1)
	acl.AddResourceParent(res1b, res1)
	acl.AddResourceParent(res1c, res1)
	acl.AddResourceParent(res1a1, res1a)
	acl.AddResourceParent(res1a2, res1a)
	acl.AddResourceParent(res1b1, res1b)
	acl.AddResourceParent(res1c1, res1c)

	acl.AddRole(rol1)
	acl.AddRole(rol2)
	acl.AddRoleParent(rol1a, rol1)
	acl.AddRoleParent(rol1b, rol1)
	acl.AddRoleParent(rol1c, rol1)
	acl.AddRoleParent(rol1a1, rol1a)
	acl.AddRoleParent(rol1a2, rol1a)
	acl.AddRoleParent(rol1b1, rol1b)

	//grant all access to ACO-1-B
	acl.AllowAllRole(res1b)
	acl.DenyAllRole(res1c)

	acl.Allow(rol1, res1)
	//1-1
	testTrue("taclHierarchy (1) - rol1 on res1", acl.IsAllowed(rol1, res1))
	testTrue("taclHierarchy (1) - rol1 on res1a", acl.IsAllowed(rol1, res1a))
	testTrue("taclHierarchy (1) - rol1 on res1a1", acl.IsAllowed(rol1, res1a1))
	testTrue("taclHierarchy (1) - rol1 on res1a2", acl.IsAllowed(rol1, res1a2))
	testTrue("taclHierarchy (1) - rol1 on res1b", acl.IsAllowed(rol1, res1b))
	testTrue("taclHierarchy (1) - rol1 on res1b1", acl.IsAllowed(rol1, res1b1))
	//rol1 allowed on res1 - overrides * deny res1c
	testFalse("taclHierarchy (1) - rol1 on res1c", acl.IsDenied(rol1, res1c))
	testTrue("taclHierarchy (1) - rol1 on res1c", acl.IsAllowed(rol1, res1c))
	testFalse("taclHierarchy (1) - rol1 on res1c1", acl.IsDenied(rol1, res1c1))
	testTrue("taclHierarchy (1) - rol1 on res1c1", acl.IsAllowed(rol1, res1c1))
	//1-2
	testTrue("taclHierarchy (2) - rol1 on res2", acl.IsDenied(rol1, res2))
	testFalse("taclHierarchy (2) - rol1 on res2", acl.IsAllowed(rol1, res2))
	//1-3
	testTrue("taclHierarchy (3) - rol1 on res3", acl.IsDenied(rol1, res3))
	testFalse("taclHierarchy (3) - rol1 on res3", acl.IsAllowed(rol1, res3))
	//2-1
	testTrue("taclHierarchy (4) - rol2 on res1", acl.IsDenied(rol2, res1))
	testTrue("taclHierarchy (4) - rol2 on res1a", acl.IsDenied(rol2, res1a))
	testTrue("taclHierarchy (4) - rol2 on res1a1", acl.IsDenied(rol2, res1a1))
	testTrue("taclHierarchy (4) - rol2 on res1a2", acl.IsDenied(rol2, res1a2))
	testTrue("taclHierarchy (4) - rol2 on res1b", acl.IsAllowed(rol2, res1b))
	testTrue("taclHierarchy (4) - rol2 on res1b1", acl.IsAllowed(rol2, res1b1))
	//2-2
	acl.AllowAction(rol2, res2, PERMTYPE_CREATE) //replicate the settings from testAllow (Java)
	//false because ARO-2::ACO-2 was added with CREATE:true before
	testFalse("taclHierarchy (5) - rol2 on res2", acl.IsDenied(rol2, res2))
	//2-3
	testTrue("taclHierarchy (6) - rol2 on res3", acl.IsDenied(rol2, res3))
	//3-1
	testTrue("taclHierarchy (7) - rol3 on res1", acl.IsDenied(rol3, res1))
	testTrue("taclHierarchy (7) - rol3 on res1a", acl.IsDenied(rol3, res1a))
	testTrue("taclHierarchy (7) - rol3 on res1a1", acl.IsDenied(rol3, res1a1))
	testTrue("taclHierarchy (7) - rol3 on res1a2", acl.IsDenied(rol3, res1a2))
	testTrue("taclHierarchy (7) - rol3 on res1b", acl.IsAllowed(rol3, res1b))
	testTrue("taclHierarchy (7) - rol3 on res1b1", acl.IsAllowed(rol3, res1b1))
	//3-2
	testTrue("taclHierarchy (8) - rol3 on res2", acl.IsDenied(rol3, res2))
	//3-3
	testTrue("taclHierarchy (9) - rol3 on res3", acl.IsDenied(rol3, res3))
	//4-1
	testTrue("taclHierarchy (10) - rol4 on res1", acl.IsDenied(rol4, res1))
	testTrue("taclHierarchy (10) - rol4 on res1a", acl.IsDenied(rol4, res1a))
	testTrue("taclHierarchy (10) - rol4 on res1a1", acl.IsDenied(rol4, res1a1))
	testTrue("taclHierarchy (10) - rol4 on res1a2", acl.IsDenied(rol4, res1a2))
	testTrue("taclHierarchy (10) - rol4 on res1b", acl.IsAllowed(rol4, res1b))
	testTrue("taclHierarchy (10) - rol4 on res1b1", acl.IsAllowed(rol4, res1b1))
	testTrue("taclHierarchy (10) - rol4 on res1c", acl.IsDenied(rol4, res1c))
	testTrue("taclHierarchy (10) - rol4 on res1c1", acl.IsDenied(rol4, res1c1))

	acl.Deny(rol1, res1a)
	//1-1
	testTrue("taclHierarchy (11) - rol1 on res1", acl.IsAllowed(rol1, res1))
	testTrue("taclHierarchy (11) - rol1 on res1a", acl.IsDenied(rol1, res1a))
	testTrue("taclHierarchy (11) - rol1 on res1a1", acl.IsDenied(rol1, res1a1))
	testTrue("taclHierarchy (11) - rol1 on res1a2", acl.IsDenied(rol1, res1a2))
	testTrue("taclHierarchy (11) - rol1 on res1b", acl.IsAllowed(rol1, res1b))
	testTrue("taclHierarchy (11) - rol1 on res1b1", acl.IsAllowed(rol1, res1b1))
	//false because ARO-1::ACO-1 overrides *::ACO-1-C
	testFalse("taclHierarchy (11) - rol1 on res1c", acl.IsDenied(rol1, res1c))
	testFalse("taclHierarchy (11) - rol1 on res1c1", acl.IsDenied(rol1, res1c1))
	//1-2
	testTrue("taclHierarchy (12) - rol1 on res2", acl.IsDenied(rol1, res2))
	testFalse("taclHierarchy (12) - rol1 on res2", acl.IsAllowed(rol1, res2))
	//1-3
	testTrue("taclHierarchy (13) - rol1 on res3", acl.IsDenied(rol1, res3))
	testFalse("taclHierarchy (13) - rol1 on res3", acl.IsAllowed(rol1, res3))
	//2-1
	testTrue("taclHierarchy (14) - rol2 on res1", acl.IsDenied(rol2, res1))
	testTrue("taclHierarchy (14) - rol2 on res1a", acl.IsDenied(rol2, res1a))
	testTrue("taclHierarchy (14) - rol2 on res1a1", acl.IsDenied(rol2, res1a1))
	testTrue("taclHierarchy (14) - rol2 on res1a2", acl.IsDenied(rol2, res1a2))
	testTrue("taclHierarchy (14) - rol2 on res1b", acl.IsAllowed(rol2, res1b))
	testTrue("taclHierarchy (14) - rol2 on res1b1", acl.IsAllowed(rol2, res1b1))
	//2-2
	//false because ARO-2::ACO-2 was added with CREATE:true before
	testFalse("taclHierarchy (15) - rol2 on res2", acl.IsDenied(rol2, res2))
	//2-3
	testTrue("taclHierarchy (16) - rol2 on res3", acl.IsDenied(rol2, res3))
	//3-1
	testTrue("taclHierarchy (17) - rol3 on res1", acl.IsDenied(rol3, res1))
	testTrue("taclHierarchy (17) - rol3 on res1a", acl.IsDenied(rol3, res1a))
	testTrue("taclHierarchy (17) - rol3 on res1a1", acl.IsDenied(rol3, res1a1))
	testTrue("taclHierarchy (17) - rol3 on res1a2", acl.IsDenied(rol3, res1a2))
	testTrue("taclHierarchy (17) - rol3 on res1b", acl.IsAllowed(rol3, res1b))
	testTrue("taclHierarchy (17) - rol3 on res1b1", acl.IsAllowed(rol3, res1b1))
	//3-2
	testTrue("taclHierarchy (18) - rol3 on res2", acl.IsDenied(rol3, res2))
	//3-3
	testTrue("taclHierarchy (19) - rol3 on res3", acl.IsDenied(rol3, res3))

	acl.Allow(rol1, res1a1)
	//1-1
	testTrue("taclHierarchy (21) - rol1 on res1", acl.IsAllowed(rol1, res1))
	testTrue("taclHierarchy (21) - rol1 on res1a", acl.IsDenied(rol1, res1a))
	testTrue("taclHierarchy (21) - rol1 on res1a1", acl.IsAllowed(rol1, res1a1))
	testTrue("taclHierarchy (21) - rol1 on res1a2", acl.IsDenied(rol1, res1a2))
	testTrue("taclHierarchy (21) - rol1 on res1b", acl.IsAllowed(rol1, res1b))
	testTrue("taclHierarchy (21) - rol1 on res1b1", acl.IsAllowed(rol1, res1b1))
	//false because ARO-1::ACO-1 overrides *::ACO-1-C
	testFalse("taclHierarchy (21) - rol1 on res1c", acl.IsDenied(rol1, res1c))
	testFalse("taclHierarchy (21) - rol1 on res1c1", acl.IsDenied(rol1, res1c1))
	//1-2
	testTrue("taclHierarchy (22) - rol1 on res2", acl.IsDenied(rol1, res2))
	//1-3
	testTrue("taclHierarchy (23) - rol1 on res3", acl.IsDenied(rol1, res3))
	//2-1
	testTrue("taclHierarchy (24) - rol2 on res1", acl.IsDenied(rol2, res1))
	testTrue("taclHierarchy (24) - rol2 on res1a", acl.IsDenied(rol2, res1a))
	testTrue("taclHierarchy (24) - rol2 on res1a1", acl.IsDenied(rol2, res1a1))
	testTrue("taclHierarchy (24) - rol2 on res1a2", acl.IsDenied(rol2, res1a2))
	testTrue("taclHierarchy (24) - rol2 on res1b", acl.IsAllowed(rol2, res1b))
	testTrue("taclHierarchy (24) - rol2 on res1b1", acl.IsAllowed(rol2, res1b1))
	//2-2
	//false because ARO-2::ACO-2 was added with CREATE:true before
	testFalse("taclHierarchy (25) - rol2 on res2", acl.IsDenied(rol2, res2))
	//2-3
	testTrue("taclHierarchy (26) - rol2 on res3", acl.IsDenied(rol2, res3))
	//3-1
	testTrue("taclHierarchy (27) - rol3 on res1", acl.IsDenied(rol3, res1))
	testTrue("taclHierarchy (27) - rol3 on res1a", acl.IsDenied(rol3, res1a))
	testTrue("taclHierarchy (27) - rol3 on res1a1", acl.IsDenied(rol3, res1a1))
	testTrue("taclHierarchy (27) - rol3 on res1a2", acl.IsDenied(rol3, res1a2))
	testTrue("taclHierarchy (27) - rol3 on res1b", acl.IsAllowed(rol3, res1b))
	testTrue("taclHierarchy (27) - rol3 on res1b1", acl.IsAllowed(rol3, res1b1))
	//3-2
	testTrue("taclHierarchy (28) - rol3 on res2", acl.IsDenied(rol3, res2))
	//3-3
	testTrue("taclHierarchy (29) - rol3 on res3", acl.IsDenied(rol3, res3))

	//deny ARO-2 to ACO-1-B; test overriding of ALL allow
	acl.Deny(rol2, res1b)
	//1-1
	testTrue("taclHierarchy (31) - rol1 on res1", acl.IsAllowed(rol1, res1))
	testTrue("taclHierarchy (31) - rol1 on res1a", acl.IsDenied(rol1, res1a))
	testTrue("taclHierarchy (31) - rol1 on res1a1", acl.IsAllowed(rol1, res1a1))
	testTrue("taclHierarchy (31) - rol1 on res1a2", acl.IsDenied(rol1, res1a2))
	testTrue("taclHierarchy (31) - rol1 on res1b", acl.IsAllowed(rol1, res1b))
	testTrue("taclHierarchy (31) - rol1 on res1b1", acl.IsAllowed(rol1, res1b1))
	//false because ARO-1::ACO-1 overrides *::ACO-1-C
	testFalse("taclHierarchy (31) - rol1 on res1c", acl.IsDenied(rol1, res1c))
	testFalse("taclHierarchy (31) - rol1 on res1c1", acl.IsDenied(rol1, res1c1))
	//1-2
	testTrue("taclHierarchy (32) - rol1 on res2", acl.IsDenied(rol1, res2))
	//1-3
	testTrue("taclHierarchy (33) - rol1 on res3", acl.IsDenied(rol1, res3))
	//2-1
	testTrue("taclHierarchy (34) - rol2 on res1", acl.IsDenied(rol2, res1))
	testTrue("taclHierarchy (34) - rol2 on res1a", acl.IsDenied(rol2, res1a))
	testTrue("taclHierarchy (34) - rol2 on res1a1", acl.IsDenied(rol2, res1a1))
	testTrue("taclHierarchy (34) - rol2 on res1a2", acl.IsDenied(rol2, res1a2))
	testTrue("taclHierarchy (34) - rol2 on res1b", acl.IsDenied(rol2, res1b))
	testTrue("taclHierarchy (34) - rol2 on res1b1", acl.IsDenied(rol2, res1b1))
	//2-2
	//false because ARO-2::ACO-2 was added with CREATE:true before
	testFalse("taclHierarchy (35) - rol2 on res2", acl.IsDenied(rol2, res2))
	//2-3
	testTrue("taclHierarchy (36) - rol2 on res3", acl.IsDenied(rol2, res3))
	//3-1
	testTrue("taclHierarchy (37) - rol3 on res1", acl.IsDenied(rol3, res1))
	testTrue("taclHierarchy (37) - rol3 on res1a", acl.IsDenied(rol3, res1a))
	testTrue("taclHierarchy (37) - rol3 on res1a1", acl.IsDenied(rol3, res1a1))
	testTrue("taclHierarchy (37) - rol3 on res1a2", acl.IsDenied(rol3, res1a2))
	testTrue("taclHierarchy (37) - rol3 on res1b", acl.IsAllowed(rol3, res1b))
	testTrue("taclHierarchy (37) - rol3 on res1b1", acl.IsAllowed(rol3, res1b1))
	//3-2
	testTrue("taclHierarchy (38) - rol3 on res2", acl.IsDenied(rol3, res2))
	//3-3
	testTrue("taclHierarchy (39) - rol3 on res3", acl.IsDenied(rol3, res3))

	//deny ARO-3 to ACO-1-B-1; test specific deny over ALL allow
	acl.Deny(rol3, res1b1)
	//1-1
	testTrue("taclHierarchy (31) - rol1 on res1", acl.IsAllowed(rol1, res1))
	testTrue("taclHierarchy (31) - rol1 on res1a", acl.IsDenied(rol1, res1a))
	testTrue("taclHierarchy (31) - rol1 on res1a1", acl.IsAllowed(rol1, res1a1))
	testTrue("taclHierarchy (31) - rol1 on res1a2", acl.IsDenied(rol1, res1a2))
	testTrue("taclHierarchy (31) - rol1 on res1b", acl.IsAllowed(rol1, res1b))
	testTrue("taclHierarchy (31) - rol1 on res1b1", acl.IsAllowed(rol1, res1b1))
	//false because ARO-1::ACO-1 overrides *::ACO-1-C
	testFalse("taclHierarchy (31) - rol1 on res1c", acl.IsDenied(rol1, res1c))
	testFalse("taclHierarchy (31) - rol1 on res1c1", acl.IsDenied(rol1, res1c1))
	//1-2
	testTrue("taclHierarchy (32) - rol1 on res2", acl.IsDenied(rol1, res2))
	//1-3
	testTrue("taclHierarchy (33) - rol1 on res3", acl.IsDenied(rol1, res3))
	//2-1
	testTrue("taclHierarchy (34) - rol2 on res1", acl.IsDenied(rol2, res1))
	testTrue("taclHierarchy (34) - rol2 on res1a", acl.IsDenied(rol2, res1a))
	testTrue("taclHierarchy (34) - rol2 on res1a1", acl.IsDenied(rol2, res1a1))
	testTrue("taclHierarchy (34) - rol2 on res1a2", acl.IsDenied(rol2, res1a2))
	testTrue("taclHierarchy (34) - rol2 on res1b", acl.IsDenied(rol2, res1b))
	testTrue("taclHierarchy (34) - rol2 on res1b1", acl.IsDenied(rol2, res1b1))
	//2-2
	//false because ARO-2::ACO-2 was added with CREATE:true before
	testFalse("taclHierarchy (35) - rol2 on res2", acl.IsDenied(rol2, res2))
	//2-3
	testTrue("taclHierarchy (36) - rol2 on res3", acl.IsDenied(rol2, res3))
	//3-1
	testTrue("taclHierarchy (37) - rol3 on res1", acl.IsDenied(rol3, res1))
	testTrue("taclHierarchy (37) - rol3 on res1a", acl.IsDenied(rol3, res1a))
	testTrue("taclHierarchy (37) - rol3 on res1a1", acl.IsDenied(rol3, res1a1))
	testTrue("taclHierarchy (37) - rol3 on res1a2", acl.IsDenied(rol3, res1a2))
	testTrue("taclHierarchy (37) - rol3 on res1b", acl.IsAllowed(rol3, res1b))
	testTrue("taclHierarchy (37) - rol3 on res1b1", acl.IsDenied(rol3, res1b1))
	//3-2
	testTrue("taclHierarchy (38) - rol3 on res2", acl.IsDenied(rol3, res2))
	//3-3
	testTrue("taclHierarchy (39) - rol3 on res3", acl.IsDenied(rol3, res3))

	/*A
	//test coverage
	acl.Allow(rol4, res1c)
	//4-1
	testTrue("taclHierarchy (40) - rol4 on res1", acl.IsDenied(rol4, res1))
	testTrue("taclHierarchy (40) - rol4 on res1a", acl.IsDenied(rol4, res1a))
	testTrue("taclHierarchy (40) - rol4 on res1a1", acl.IsDenied(rol4, res1a1))
	testTrue("taclHierarchy (40) - rol4 on res1a2", acl.IsDenied(rol4, res1a2))
	testTrue("taclHierarchy (40) - rol4 on res1b", acl.IsAllowed(rol4, res1b))
	testTrue("taclHierarchy (40) - rol4 on res1b1", acl.IsAllowed(rol4, res1b1))
	testTrue("taclHierarchy (40) - rol4 on res1c", acl.IsAllowed(rol4, res1c))
	testTrue("taclHierarchy (40) - rol4 on res1c1", acl.IsAllowed(rol4, res1c1))
	testTrue("taclHierarchy (40) - rol4 on res4", acl.IsDenied(rol4, res4))
	acl.Allow(rol4, res4)
	testTrue("taclHierarchy (40) - rol4 on res4", acl.IsAllowed(rol4, res4))
	*/
}

func taclRemoveNull(t *testing.T) {
	acl := NewAcl()
	rolna := &SimpleEntry{"NA-ROLE"}
	resna := &SimpleEntry{"NA-RES"}
	testTrue := func(title string, res bool) {
		if res != true {
			t.Errorf("%v: expected true; got false", title)
		}
	}
	testFalse := func(title string, res bool) {
		if res != false {
			t.Errorf("%v: expected false; got true", title)
		}
	}
	var e error

	testFalse("taclRemoveNull (1)", acl.IsAllowed(rolna, resna))
	testTrue("taclRemoveNull (2)", acl.IsDenied(rolna, resna))
	testFalse("taclRemoveNull (1a)", acl.IsAllowedAction(rolna, resna, PERMTYPE_CREATE))
	testTrue("taclRemoveNull (2a)", acl.IsDeniedAction(rolna, resna, PERMTYPE_CREATE))

	e = acl.Remove(nil, nil) //removes the default permissions
	if e != nil {
		t.Errorf("expect Remove(nil, nil) to return nil; got %v", e)
	}
	//false for both because the root is removed
	testFalse("taclRemoveNull (3)", acl.IsAllowed(rolna, resna))
	testFalse("taclRemoveNull (4)", acl.IsDenied(rolna, resna))
	testFalse("taclRemoveNull (3a)", acl.IsAllowedAction(rolna, resna, PERMTYPE_CREATE))
	testFalse("taclRemoveNull (4a)", acl.IsDeniedAction(rolna, resna, PERMTYPE_CREATE))

	//removing nil again should return error
	e = acl.Remove(nil, nil)
	if e == nil {
		t.Errorf("taclRemoveNull (5): expected error; got nil")
	}
	e = acl.RemoveAction(nil, nil, PERMTYPE_CREATE)
	if e == nil {
		t.Errorf("taclRemoveNull (6): expected error; got nil")
	}
}

func taclRemoveResourceRole(t *testing.T) {
	acl := NewAcl()
	rols := [...]string{"R1", "R2", "R3", "R4"}
	ress := [...]string{"C1", "C2", "C3", "C4"}
	testTrue := func(title string, res bool) {
		if res != true {
			t.Errorf("%v: expected true; got false", title)
		}
	}
	testEqual := func(title string, exp, act int) {
		if exp != act {
			t.Errorf(title, exp, act)
		}
	}
	testErrNil := func(title string, e error) {
		if e == nil {
			t.Errorf(title + "; got nil")
		}
		if e != ErrNilEntry {
			t.Errorf(title+"; got %v", e)
		}
	}
	testErrEntryNotFound := func(title string, e error) {
		if e == nil {
			t.Errorf(title + "; got nil")
		}
		if e != ErrEntryNotFound {
			t.Errorf(title+"; got %v", e)
		}
	}

	acl.Clear()
	//create mappings for each key pair
	for _, c := range ress {
		for _, r := range rols {
			acl.Allow(&SimpleEntry{r}, &SimpleEntry{c})
			testTrue("taclRemoveResourceRole (A)", acl.IsAllowed(&SimpleEntry{r}, &SimpleEntry{c}))
		}
	}
	//add children to both resources and roles
	for i := 1; i <= 4; i++ {
		acl.AddResourceParent(&SimpleEntry{"CC" + strconv.Itoa(i)}, &SimpleEntry{"C" + strconv.Itoa(i)})
		acl.AddRoleParent(&SimpleEntry{"RC" + strconv.Itoa(i)}, &SimpleEntry{"R" + strconv.Itoa(i)})
	}
	//assign permission for child1 to child2
	acl.Allow(&SimpleEntry{"RC1"}, &SimpleEntry{"CC2"})
	acl.Allow(&SimpleEntry{"RC2"}, &SimpleEntry{"CC1"})
	//add ALL access
	for _, c := range ress {
		acl.AllowAllRole(&SimpleEntry{c})
		testTrue("taclRemoveResourceRole (B)", acl.IsAllowed(&SimpleEntry{"*"}, &SimpleEntry{c}))
	}
	for _, r := range rols {
		acl.AllowAllResource(&SimpleEntry{r})
		testTrue("taclRemoveResourceRole (C)", acl.IsAllowed(&SimpleEntry{r}, &SimpleEntry{"*"}))
	}

	//4x4 pairs, 2x4 ALL access, 1 child each
	if 26 != len(acl.ExportPermissions()) {
		t.Errorf("Expected 4x4+4+4+1+1=>%d; got %d", len(acl.ExportPermissions()))
	}
	testEqual("Expected 4+4=>%d; got %d", 8, len(acl.ExportResources()))
	testEqual("Expected 4+4=>%d; got %d", 8, len(acl.ExportRoles()))

	//remove all access on C4
	acl.RemoveResource(&SimpleEntry{"C4"}, false)
	testEqual("Expected 4x3+4+3+1+1=>%d; got %d", 21, len(acl.ExportPermissions()))
	testEqual("Expected 3+4=>%d; got %d", 7, len(acl.ExportResources()))
	testEqual("Expected 4+4=>%d; got %d", 8, len(acl.ExportRoles()))

	//remove all access on R4
	acl.RemoveRole(&SimpleEntry{"R4"}, false)
	testEqual("Expected 3x3+3+3+1+1=>%d; got %d", 17, len(acl.ExportPermissions()))
	testEqual("Expected 3+4=>%d; got %d", 7, len(acl.ExportResources()))
	testEqual("Expected 3+4=>%d (less R4 but not its child); got %d", 7, len(acl.ExportRoles()))

	//remove all access on C3 and child
	acl.RemoveResource(&SimpleEntry{"C3"}, true)
	testEqual("Expected 3x2+3+2+1+1=>%d; got %d", 13, len(acl.ExportPermissions()))
	testEqual("Expected 2+3=>%d (less C3 and child); got %d", 5, len(acl.ExportResources()))
	testEqual("Expected 3+4=>%d; got %d", 7, len(acl.ExportRoles()))

	//remove all access on R3 and child
	acl.RemoveRole(&SimpleEntry{"R3"}, true)
	testEqual("Expected 2x2+2+2+1+1=>%d; got %d", 10, len(acl.ExportPermissions()))
	testEqual("Expected 2+3=>%d; got %d", 5, len(acl.ExportResources()))
	testEqual("Expected 2+3=>%d (less R3 and child); got %d", 5, len(acl.ExportRoles()))

	//remove all access on C2 and child
	acl.RemoveResource(&SimpleEntry{"C2"}, true)
	testEqual("Expected 2x1+2+1+1+0=>%d (less child permission); got %d", 6, len(acl.ExportPermissions()))
	testEqual("Expected 1+2=>%d (less C2 and child); got %d", 3, len(acl.ExportResources()))
	testEqual("Expected 2+3=>%d; got %d", 5, len(acl.ExportRoles()))

	//remove all access on R2 and child
	acl.RemoveRole(&SimpleEntry{"R2"}, true)
	testEqual("Expected 1x1+1+1+0+0=>%d (less child permission); got %d", 3, len(acl.ExportPermissions()))
	testEqual("Expected 1+2=>%d; got %d", 3, len(acl.ExportResources()))
	testEqual("Expected 1+2=>%d (less R2 and child); got %d", 3, len(acl.ExportRoles()))

	//remove all access on C1 and child
	acl.RemoveResource(&SimpleEntry{"C1"}, true)
	testEqual("Expected 1x0+1+0+0+0=>%d; got %d", 1, len(acl.ExportPermissions()))
	testEqual("Expected 0+1=>%d; got %d", 1, len(acl.ExportResources()))
	testEqual("Expected 1+2=>%d; got %d", 3, len(acl.ExportRoles()))

	//remove all access on R1 and child
	acl.RemoveRole(&SimpleEntry{"R1"}, true)
	testEqual("Expected 0x0+0+0+0+0=>%d; got %d", 0, len(acl.ExportPermissions()))
	testEqual("Expected 0+1=>%d; got %d", 1, len(acl.ExportResources()))
	testEqual("Expected 0+1=>%d; got %d", 1, len(acl.ExportRoles()))

	//test coverage
	e := acl.RemoveResource(nil, false)
	testErrNil("taclRemoveResourceRole (1) - expected ErrNilEntry", e)
	e = acl.RemoveRole(nil, false)
	testErrNil("taclRemoveResourceRole (2) - expected ErrNilEntry", e)
	e = acl.RemoveResource(&SimpleEntry{"C1"}, true)
	testErrEntryNotFound("taclRemoveResourceRole (3) - expected ErrEntryNotFound", e)
	acl.RemoveRole(&SimpleEntry{"R1"}, true)
	testErrEntryNotFound("taclRemoveResourceRole (4) - expected ErrEntryNotFound", e)
}

func TestAclExportImport(t *testing.T) {
	acl := NewAcl()
	//test export
	resources := acl.ExportResources()
	roles := acl.ExportRoles()
	perms := acl.ExportPermissions()
	testTrue := func(title string, res bool) {
		if res != true {
			t.Errorf("%v: expected true; got false", title)
		}
	}
	testFalse := func(title string, res bool) {
		if res != false {
			t.Errorf("%v: expected false; got true", title)
		}
	}
	testNil := func(title string, err error) {
		if err != nil {
			t.Errorf("%v: expected nil error; got %v", title, err)
		}
	}
	testEqual := func(title string, exp, act int) {
		if exp != act {
			t.Errorf(title, exp, act)
		}
	}
	testErrNonEmpty := func(title string, e error) {
		if e == nil {
			t.Errorf(title + "; got nil")
		}
		if e != ErrNonEmpty {
			t.Errorf(title+"; got %v", e)
		}
	}

	testEqual("TestAclExportImport(1) - existing resources", 0, len(resources))
	testEqual("TestAclExportImport(1) - existing roles", 0, len(roles))
	testEqual("TestAclExportImport(1) - existing default permission", 1, len(perms))

	//verify that the exported ones are indeed snapshots
	acl.AddResource(&SimpleEntry{"laser-gun"})
	acl.AddRole(&SimpleEntry{"jedi"})
	acl.Deny(&SimpleEntry{"jedi"}, &SimpleEntry{"laser-gun"})

	newResources := acl.ExportResources()
	newRoles := acl.ExportRoles()
	newPerms := acl.ExportPermissions()

	testTrue("TestAclExportImport(2) - exported resources are not affected", len(newResources) > len(resources))
	testTrue("TestAclExportImport(2) - exported roles are not affected", len(newRoles) > len(roles))
	testTrue("TestAclExportImport(2) - exported permissions are not affected", len(newPerms) > len(perms))

	//simulate saved data
	res := make(map[string]string)
	res["laser-gun"] = ""
	res["light-sabre"] = ""
	res["staff"] = "light-sabre"
	res["t"] = "light-sabre"
	rol := make(map[string]string)
	rol["jedi"] = ""
	rol["sith"] = ""
	rol["obiwan"] = "jedi"
	rol["luke"] = "jedi"
	rol["darth-vader"] = "sith"
	rol["darth-maul"] = "sith"
	per := make(map[string]map[string]bool)
	allTrue := make(map[string]bool)
	allTrue["ALL"] = true
	allFalse := make(map[string]bool)
	allFalse["ALL"] = false
	per["*::*"] = allFalse
	per["jedi::light-sabre"] = allTrue
	per["jedi::laser-gun"] = allFalse
	per["sith::light-sabre"] = allTrue
	per["sith::laser-gun"] = allTrue
	per["luke::laser-gun"] = allTrue
	per["jedi::t"] = allFalse

	//test import
	var e error
	e = acl.ImportResources(res)
	testErrNonEmpty("TestAclExportImport(3) - resources are non-empty", e)
	e = acl.ImportRoles(rol)
	testErrNonEmpty("TestAclExportImport(3) - roles are non-empty", e)
	e = acl.ImportPermissions(per)
	testErrNonEmpty("TestAclExportImport(3) - permissions are non-empty", e)

	acl.Clear()
	e = acl.ImportResources(res)
	testNil("TestAclExportImport(4a)", e)
	e = acl.ImportRoles(rol)
	testNil("TestAclExportImport(4b)", e)
	e = acl.ImportPermissions(per)
	testNil("TestAclExportImport(4c)", e)

	//verify that the permissions are correct
	testTrue("TestAclExportImport(5) - allow for jedi::light-sabre",
		acl.IsAllowed(&SimpleEntry{"jedi"}, &SimpleEntry{"light-sabre"}))
	testTrue("TestAclExportImport(5) - deny for jedi::laser-gun",
		acl.IsDenied(&SimpleEntry{"jedi"}, &SimpleEntry{"light-gun"}))
	testTrue("TestAclExportImport(5) - allow for luke::laser-gun",
		acl.IsAllowed(&SimpleEntry{"luke"}, &SimpleEntry{"laser-gun"}))
	testTrue("TestAclExportImport(5) - allow for sith::laser-gun",
		acl.IsAllowed(&SimpleEntry{"sith"}, &SimpleEntry{"laser-gun"}))
	testTrue("TestAclExportImport(5) - deny for jedi::t",
		acl.IsDenied(&SimpleEntry{"jedi"}, &SimpleEntry{"t"}))

	//change permissions
	acl.Deny(&SimpleEntry{"sith"}, &SimpleEntry{"laser-gun"})
	//add resource, role and permission
	acl.AddResourceParent(&SimpleEntry{"double"}, &SimpleEntry{"light-sabre"})
	acl.AddRoleParent(&SimpleEntry{"anakin"}, &SimpleEntry{"jedi"})
	acl.AllowAction(&SimpleEntry{"jedi"}, &SimpleEntry{"double"}, PERMTYPE_UPDATE)

	//verify permissions are correct
	testTrue("TestAclExportImport(6) - true because UPDATE on double is redundant",
		acl.IsAllowed(&SimpleEntry{"jedi"}, &SimpleEntry{"double"}))
	testTrue("TestAclExportImport(6)",
		acl.IsAllowedAction(&SimpleEntry{"jedi"}, &SimpleEntry{"double"}, PERMTYPE_UPDATE))

	//change double to be not redeundant
	acl.DenyAction(&SimpleEntry{"jedi"}, &SimpleEntry{"double"}, PERMTYPE_CREATE)

	testFalse("TestAclExportImport(7a) - false because jedi::double DENY CREATE",
		acl.IsAllowed(&SimpleEntry{"jedi"}, &SimpleEntry{"double"}))
	testTrue("TestAclExportImport(7b) - still true",
		acl.IsAllowedAction(&SimpleEntry{"jedi"}, &SimpleEntry{"double"}, PERMTYPE_UPDATE))
	testTrue("TestAclExportImport(7c) - true because inherits jedi",
		acl.IsDeniedAction(&SimpleEntry{"luke"}, &SimpleEntry{"double"}, PERMTYPE_CREATE))
	testTrue("TestAclExportImport(7d)",
		acl.IsDenied(&SimpleEntry{"sith"}, &SimpleEntry{"laser-gun"}))
	testTrue("TestAclExportImport(7e)",
		acl.IsDenied(&SimpleEntry{"jedi"}, &SimpleEntry{"t"}))

	newResources = acl.ExportResources()
	newRoles = acl.ExportRoles()
	newPerms = acl.ExportPermissions()
	testTrue("TestAclExportImport(8) - exported resources are not affected", len(newResources) > len(res))
	testTrue("TestAclExportImport(8) - exported roles are not affected", len(newRoles) > len(rol))
	testTrue("TestAclExportImport(8) - exported permissions are not affected", len(newPerms) > len(per))
}

func TestAclRealLife(t *testing.T) {
	acl := NewAcl()
	gen := "GENERAL"
	sys := "SYSTEM"
	fin := "FINANCE"
	tec := "TECH"

	roles := make(map[string]string)
	roles[gen] = "*"
	roles[sys] = gen
	roles["cch"] = sys
	roles[fin] = "*"
	roles[tec] = "*"
	roles["rahman"] = tec
	roles["julia"] = fin

	res := make(map[string]string)
	res["organization"] = "*"
	res["device"] = "*"
	res["make"] = "*"

	allF := make(map[string]bool)
	allF["ALL"] = false
	allT := make(map[string]bool)
	allT["ALL"] = true
	perms := make(map[string]map[string]bool)
	perms[DEFAULT_KEY] = allF
	perms[sys+"::*"] = allT
	perms[tec+"::device"] = allT
	perms[fin+"::organization"] = allT

	acl.Clear()
	acl.ImportRoles(roles)
	acl.ImportResources(res)
	acl.ImportPermissions(perms)

	fmt.Println(acl.Visualize())
}

func TestAclCakeExample(t *testing.T) {
	acl := NewAcl()
	acl.Clear()
	testTrue := func(title string, res bool) {
		if res != true {
			t.Errorf("%v: expected true; got false", title)
		}
	}
	testFalse := func(title string, res bool) {
		if res != false {
			t.Errorf("%v: expected false; got true", title)
		}
	}
	var e error

	warriors := &SimpleEntry{"Warriors"}
	aragorn := &SimpleEntry{"Aragorn"}
	legolas := &SimpleEntry{"Legolas"}
	gimli := &SimpleEntry{"Gimli"}
	wizards := &SimpleEntry{"Wizards"}
	gandalf := &SimpleEntry{"Gandalf"}
	hobbits := &SimpleEntry{"Hobbits"}
	frodo := &SimpleEntry{"Frodo"}
	bilbo := &SimpleEntry{"Bilbo"}
	merry := &SimpleEntry{"Merry"}
	pippin := &SimpleEntry{"Pippin"}
	visitors := &SimpleEntry{"Visitors"}
	gollum := &SimpleEntry{"Gollum"}

	roles := make(map[string]string)
	roles[warriors.GetID()] = ""
	roles[wizards.GetID()] = ""
	roles[hobbits.GetID()] = ""
	roles[visitors.GetID()] = ""
	e = acl.ImportRoles(roles)
	if e != nil {
		t.Errorf("expected ImportRoles to return nil; got %v", e)
	}
	//acl.AddRole(warriors)
	//acl.AddRole(wizards)
	//acl.AddRole(hobbits)
	//acl.AddRole(visitors)
	//cannot import twice
	e = acl.ImportRoles(roles)
	if e == nil {
		t.Errorf("expected ImportRoles to return error; got nil")
	}

	acl.AddRoleParent(gimli, warriors)
	acl.AddRoleParent(legolas, warriors)
	acl.AddRoleParent(aragorn, warriors)
	acl.AddRoleParent(gandalf, wizards)
	acl.AddRoleParent(frodo, hobbits)
	acl.AddRoleParent(bilbo, hobbits)
	acl.AddRoleParent(merry, hobbits)
	acl.AddRoleParent(pippin, hobbits)
	acl.AddRoleParent(gollum, visitors)

	weapons := &SimpleEntry{"Weapons"}
	ring := &SimpleEntry{"The One Ring"}
	pork := &SimpleEntry{"Salted Pork"}
	diplomacy := &SimpleEntry{"Diplomacy"}
	ale := &SimpleEntry{"Ale"}

	//test importResources
	res := make(map[string]string)
	res[weapons.GetID()] = ""
	res[ring.GetID()] = ""
	res[pork.GetID()] = ""
	res[diplomacy.GetID()] = ""
	res[ale.GetID()] = ""

	e = acl.ImportResources(res)
	if e != nil {
		t.Errorf("expected ImportResources to return nil; got %v", e)
	}
	//cannot import twice
	e = acl.ImportResources(res)
	if e == nil {
		t.Errorf("expected ImportResources to return error; got nil")
	}

	//deny all
	acl.MakeDefaultDeny()

	//allow warriors
	acl.Allow(warriors, weapons)
	acl.Allow(warriors, ale)
	acl.Allow(warriors, pork)
	acl.Allow(aragorn, diplomacy)
	acl.DenyAction(gimli, weapons, PERMTYPE_DELETE)
	acl.DenyAction(legolas, weapons, PERMTYPE_DELETE)

	//allow wizards
	acl.Allow(wizards, ale)
	acl.Allow(wizards, pork)
	acl.Allow(wizards, diplomacy)

	//allow hobbits
	acl.Allow(hobbits, ale)
	acl.Allow(frodo, ring)
	acl.Deny(merry, ale)
	acl.Allow(pippin, diplomacy)

	//allow visitors
	acl.Allow(visitors, pork)

	printRegistries(*acl)

	//pippin
	testTrue("Pippin can access ale", acl.IsAllowed(pippin, ale))
	testTrue("Merry cannot", acl.IsDenied(merry, ale))

	//aragorn
	testTrue("Aragorn access", acl.IsAllowed(aragorn, weapons))
	testTrue("Aragorn access", acl.IsAllowedAction(aragorn, weapons, PERMTYPE_CREATE))
	testTrue("Aragorn access", acl.IsAllowedAction(aragorn, weapons, PERMTYPE_READ))
	testTrue("Aragorn access", acl.IsAllowedAction(aragorn, weapons, PERMTYPE_UPDATE))
	testTrue("Aragorn access", acl.IsAllowedAction(aragorn, weapons, PERMTYPE_DELETE))
	//legolas
	testFalse("Legolas access", acl.IsAllowed(legolas, weapons))
	testTrue("Legolas access", acl.IsAllowedAction(legolas, weapons, PERMTYPE_CREATE))
	testTrue("Legolas access", acl.IsAllowedAction(legolas, weapons, PERMTYPE_READ))
	testTrue("Legolas access", acl.IsAllowedAction(legolas, weapons, PERMTYPE_UPDATE))
	testFalse("Legolas access", acl.IsAllowedAction(legolas, weapons, PERMTYPE_DELETE))
	//gimli
	testFalse("Gimli access", acl.IsAllowed(gimli, weapons))
	testTrue("Gimli access", acl.IsAllowedAction(gimli, weapons, PERMTYPE_CREATE))
	testTrue("Gimli access", acl.IsAllowedAction(gimli, weapons, PERMTYPE_READ))
	testTrue("Gimli access", acl.IsAllowedAction(gimli, weapons, PERMTYPE_UPDATE))
	testFalse("Gimli access", acl.IsAllowedAction(gimli, weapons, PERMTYPE_DELETE))
}

func printRegistries(a Acl) {
	fmt.Println(">>> RESOURCES")
	fmt.Println(a.VisualizeResources(&SimpleEntry{}))
	fmt.Println(">>> ROLES")
	fmt.Println(a.VisualizeRoles(&SimpleEntry{}))
	fmt.Println(">>> PERMISSIONS")
	fmt.Println(a.VisualizePermissions())
}

func TestCoveragePermTypes(t *testing.T) {
	three := 3
	zero := 0
	ten := 10
	var pt PermTypes

	pt = PermTypes(three)
	if pt.String() != "READ" {
		t.Errorf("expect PermType READ; got %v", pt.String())
	}
	pt = PermTypes(zero)
	if pt.String() != "ALL" {
		t.Errorf("expect PermType ALL; got %v", pt.String())
	}
	pt = PermTypes(ten)
	if pt.String() != "ALL" {
		t.Errorf("expect PermType ALL; got %v", pt.String())
	}
}

func TestCoverageRoot(t *testing.T) {
	re := NewRootEntry()
	if re.GetEntryDesc() != "ROOT" {
		t.Errorf("expect GetEntryDesc() to return ROOT for root entry; got %v", re.GetEntryDesc())
	}
	if re.RetrieveEntry("") != nil {
		t.Errorf("expect RetrieveEntry() to return nil for root entry; got %v", re.RetrieveEntry(""))
	}
}
