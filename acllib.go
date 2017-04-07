package archly

import (
	"bytes"
	"errors"
	"strconv"
	"strings"
)

const (
	PERMTYPE_ALL PermTypes = 1 + iota
	PERMTYPE_CREATE
	PERMTYPE_READ
	PERMTYPE_UPDATE
	PERMTYPE_DELETE
	DEFAULT_KEY = "*::*"
	NOT_FOUND   = "Permission %v not found on %v for %v"
)

var (
	ErrDupEntry = errors.New("duplicate entry in registry")

	ErrEntryNotFound = errors.New("entry not found in registry")

	permtypes = [...]string{
		"ALL",
		"CREATE",
		"READ",
		"UPDATE",
		"DELETE",
	}
)

type PermTypes int

func (p PermTypes) String() string {
	if p > 5 || p < 1 {
		return permtypes[0]
	}
	return permtypes[p-1]
}

type Entry interface {
	GetID() string
	GetEntryDesc() string
	RetrieveEntry(string) Entry
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

type Permission struct {
	Perms map[string]map[string]bool
}

func (this *Permission) Allow(role, resource string) {
	key := this.makeKey(role, resource)
	perm := this.makePerm(PERMTYPE_ALL, true)
	this.Perms[key] = perm
}

func (this *Permission) AllowAction(role, resource string, action PermTypes) {
	key := this.makeKey(role, resource)

	if p, has := this.Perms[key]; has {
		p[action.String()] = true
		this.Perms[key] = p
	} else {
		this.Perms[key] = this.makePerm(action, true)
	}
}

func (this *Permission) Clear() {
	this.Perms = make(map[string]map[string]bool)
}

func (this *Permission) Deny(role, resource string) {
	key := this.makeKey(role, resource)
	perm := this.makePerm(PERMTYPE_ALL, false)
	this.Perms[key] = perm
}

func (this *Permission) DenyAction(role, resource string, action PermTypes) {
	key := this.makeKey(role, resource)

	if p, has := this.Perms[key]; has {
		p[action.String()] = false
		this.Perms[key] = p
	} else {
		this.Perms[key] = this.makePerm(action, false)
	}
}

func (this *Permission) Export() map[string]map[string]bool {
	dup := make(map[string]map[string]bool)
	for key, v := range this.Perms {
		m := make(map[string]bool)
		for perm, acc := range v {
			m[perm] = acc
		}
		dup[key] = m
	}
	return dup
}

func (this *Permission) Has(key string) bool {
	_, has := this.Perms[key]
	return has
}

func (this *Permission) ImportMap(p map[string]map[string]bool) {
	this.Perms = make(map[string]map[string]bool)
	for key, v := range p {
		m := make(map[string]bool)
		for perm, acc := range v {
			m[perm] = acc
		}
		this.Perms[key] = m
	}
}

func (this *Permission) IsAllowed(role, resource string) (bool, bool) {
	key := this.makeKey(role, resource)
	allSet := 0

	if !this.Has(key) {
		return false, false
	}
	perm := this.Perms[key]
	for action, access := range perm {
		//if any entry is false, resource is NOT allowed
		if access == false {
			return false, true
		}
		if action != PERMTYPE_ALL.String() {
			allSet++
		}
	}
	if _, has := perm[PERMTYPE_ALL.String()]; has {
		return true, false //positive because FALSE would be caught in the loop
	}
	if allSet == 4 {
		return true, false
	}
	return false, false
}

func (this *Permission) IsAllowedAction(role, resource string, action PermTypes) (bool, bool) {
	key := this.makeKey(role, resource)

	if !this.Has(key) {
		return false, false
	}
	perm := this.Perms[key]
	if p, has := perm[action.String()]; !has {
		//if specific action is not present, check for ALL
		if allp, hasAll := perm[PERMTYPE_ALL.String()]; !hasAll {
			return false, false
		} else {
			if allp {
				return true, false
			} else {
				return false, true
			}
		}
	} else { //else specific action present
		if p {
			return true, false
		} else {
			return false, true
		}
	}
}

// Returns `true, false` only if the role has been explicitly denied access to
// all actions on the resource. Returns `false, true` if the role has been
// explicitly granted access. Return `false, false` otherwise.
func (this *Permission) IsDenied(role, resource string) (bool, bool) {
	key := this.makeKey(role, resource)
	allSet := 0

	if !this.Has(key) {
		return false, false
	}
	perm := this.Perms[key]
	for action, access := range perm {
		//if any entry is true, resource is NOT denied
		if access {
			return false, true
		}
		if action != PERMTYPE_ALL.String() {
			allSet++
		}
	}
	if _, has := perm[PERMTYPE_ALL.String()]; has {
		return true, false //denied because TRUE would be caught in the loop
	}
	if allSet == 4 {
		return true, false
	}
	return false, false
}

func (this *Permission) IsDeniedAction(role, resource string, action PermTypes) (bool, bool) {
	key := this.makeKey(role, resource)

	if !this.Has(key) {
		return false, false
	}
	perm := this.Perms[key]
	if p, has := perm[action.String()]; !has {
		//if specific action is not present, check for ALL
		if allp, hasAll := perm[PERMTYPE_ALL.String()]; !hasAll {
			return false, false
		} else {
			if allp {
				return false, true
			} else {
				return true, false
			}
		}
	} else { //else specific action present
		if p {
			return false, true
		} else {
			return true, false
		}
	}
}

func (this *Permission) MakeDefaultAllow() {
	this.Perms[DEFAULT_KEY] = this.makePerm(PERMTYPE_ALL, true)
}

func (this *Permission) MakeDefaultDeny() {
	this.Perms[DEFAULT_KEY] = this.makePerm(PERMTYPE_ALL, false)
}

func (this *Permission) Remove(role, resource string) error {
	key := this.makeKey(role, resource)

	if !this.Has(key) {
		return ErrEntryNotFound
	}
	delete(this.Perms, key)
	return nil
}

func (this *Permission) RemoveAction(role, resource string, action PermTypes) error {
	key := this.makeKey(role, resource)

	if !this.Has(key) {
		return ErrEntryNotFound
	}
	perm := this.Perms[key]
	if _, has := perm[action.String()]; has {
		delete(perm, action.String())
	} else if _, hasAll := perm[PERMTYPE_ALL.String()]; hasAll {
		origVal := perm[PERMTYPE_ALL.String()]
		//has ALL - remove and put in the others
		delete(perm, PERMTYPE_ALL.String())
		for t := 2; t <= len(permtypes); t++ {
			if t != int(action) {
				perm[PermTypes(t).String()] = origVal
			}
		}
	} else {
		return ErrEntryNotFound
	}

	if len(perm) == 0 {
		delete(this.Perms, key)
	} else {
		this.Perms[key] = perm
	}
	return nil
}

func (this *Permission) RemoveByResource(resource string) int {
	toRemove := make(map[string]bool)

	resource = "::" + resource
	for key, _ := range this.Perms {
		if strings.HasSuffix(key, resource) {
			toRemove[key] = true
		}
	}
	return this.del(toRemove)
}

func (this *Permission) RemoveByRole(role string) int {
	toRemove := make(map[string]bool)

	role += "::"
	for key, _ := range this.Perms {
		if strings.HasPrefix(key, role) {
			toRemove[key] = true
		}
	}
	return this.del(toRemove)
}

func (this *Permission) Size() int {
	return len(this.Perms)
}

func (this *Permission) String() string {
	var buf bytes.Buffer

	buf.WriteString(strconv.Itoa(this.Size()))
	buf.WriteString("\n-------\n")

	i := 0
	for k, v := range this.Perms {
		i++
		buf.WriteString(strconv.Itoa(i))
		buf.WriteString("- ")
		buf.WriteString(k)
		buf.WriteString("\n")
		for key, value := range v {
			buf.WriteString("\t")
			buf.WriteString(key)
			buf.WriteString("\t")
			buf.WriteString(strconv.FormatBool(value))
			buf.WriteString("\n")
		}
	}

	return buf.String()
}

func (this *Permission) del(keys map[string]bool) int {
	removed := 0

	for key, _ := range keys {
		if _, has := this.Perms[key]; has {
			removed++
		}
		delete(this.Perms, key)
	}
	return removed
}

func (this *Permission) makeKey(aro, aco string) string {
	if aro == "" {
		aro = "*"
	}
	if aco == "" {
		aco = "*"
	}
	return aro + "::" + aco
}

func (this *Permission) makePerm(action PermTypes, allow bool) map[string]bool {
	p := make(map[string]bool)
	p[action.String()] = allow
	return p
}

func NewPermission(asWhitelist bool) *Permission {
	p := Permission{}
	p.Perms = make(map[string]map[string]bool)
	if asWhitelist {
		p.MakeDefaultDeny()
	} else {
		p.MakeDefaultAllow()
	}
	return &p
}
