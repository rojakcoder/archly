package archly

import (
	"bytes"
	"errors"
)

var (
	ErrDupEntry = errors.New("duplicate entry in registry")

	ErrEntryNotFound = errors.New("entry not found in registry")
)

type Entry interface {
	GetID() string
	GetEntryDesc() string
	RetrieveEntry(string) Entry
}

type SimpleEntry struct {
	ID string
}

func (this *SimpleEntry) GetID() string {
	return this.ID
}

func (this *SimpleEntry) GetEntryDesc() string {
	return ""
}

func (this *SimpleEntry) RetrieveEntry(resID string) *SimpleEntry {
	return &SimpleEntry{resID}
}

type Registry struct {
	Reg map[string]string
}

func NewRegistry() *Registry {
	reg := &Registry{}
	reg.Clear()
	return reg
}

func (this *Registry) Add(ent string) error {
	if _, dup := this.Reg[ent]; dup {
		return ErrDupEntry
	}
	this.Reg[ent] = ""
	return nil
}

func (this *Registry) AddChild(child, parent string) error {
	if _, dup := this.Reg[child]; dup {
		return ErrDupEntry
	}
	if _, has := this.Reg[parent]; !has {
		return ErrEntryNotFound
	}
	this.Reg[child] = parent
	return nil
}

func (this *Registry) Clear() {
	this.Reg = make(map[string]string)
}

func (this *Registry) Export() map[string]string {
	dup := make(map[string]string)
	for k, v := range this.Reg {
		dup[k] = v
	}
	return dup
}

func (this *Registry) Has(ent string) bool {
	_, has := this.Reg[ent]
	return has
}

func (this *Registry) HasChild(parent string) bool {
	for _, p := range this.Reg {
		if p == parent {
			return true
		}
	}
	return false
}

func (this *Registry) ImportRegistry(reg map[string]string) {
	this.Clear()
	for k, v := range reg {
		this.Reg[k] = v
	}
}

func (this *Registry) TraverseRoot(ent string) []string {
	path := make([]string, 0)

	if ent == "" {
		return append(path, "*")
	}

	e := ent
	for this.Has(e) {
		path = append(path, e)
		e = this.Reg[e]
	}
	return append(path, "*")
}

func (this *Registry) Display(loader Entry, leading, ent string) string {
	var buf bytes.Buffer

	children := this.findChildren(ent)
	for _, c := range children {
		entry := loader.RetrieveEntry(c)
		buf.WriteString(leading)
		buf.WriteString("- ")
		buf.WriteString(entry.GetEntryDesc())
		buf.WriteString("\n")
		buf.WriteString(this.Display(loader, " "+leading, c))
	}
	return buf.String()
}

func (this *Registry) Remove(ent string, desc bool) ([]string, error) {
	if !this.Has(ent) {
		return nil, ErrEntryNotFound
	}
	removed := make([]string, 0)

	if this.HasChild(ent) {
		parent := this.Reg[ent]
		children := this.findChildren(ent)
		if desc {
			removed = append(removed, this.removeDesc(children)...)
		} else {
			for _, c := range children {
				this.Reg[c] = parent
			}
		}
	}
	delete(this.Reg, ent)
	return append(removed, ent), nil
}

func (this *Registry) Size() int {
	return len(this.Reg)
}

func (this *Registry) String() string {
	var buf bytes.Buffer

	for k, v := range this.Reg {
		buf.WriteString("\t")
		buf.WriteString(k)
		if len(k) >= 8 {
			buf.WriteString("\t - \t")
		} else {
			buf.WriteString("\t\t - \t")
		}
		if v == "" {
			buf.WriteString("*")
		} else {
			buf.WriteString(v)
		}
		buf.WriteString("\n")
	}
	return buf.String()
}

func (this *Registry) findChildren(parent string) []string {
	children := make([]string, 0)
	for k, v := range this.Reg {
		if v == parent {
			children = append(children, k)
		}
	}
	return children
}

func (this *Registry) removeDesc(ents []string) []string {
	removed := make([]string, 0)

	for _, ent := range ents {
		delete(this.Reg, ent)
		removed = append(removed, ent)
		for this.HasChild(ent) {
			removed = append(removed, this.removeDesc(this.findChildren(ent))...)
		}
	}
	return removed
}

func RegPrintPath(path []string) string {
	var buf bytes.Buffer

	buf.WriteString("-")
	for _, p := range path {
		buf.WriteString(" -> ")
		buf.WriteString(p)
	}
	buf.WriteString(" <")
	return buf.String()
}
