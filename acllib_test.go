package archly

import (
	"testing"

	"google.golang.org/appengine/aetest"

	"golang.org/x/net/context"
)

type Case struct {
	title  string
	size   int
	err    error
	prereq func(context.Context, *Registry) error
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
		//TODO run Display
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
}
