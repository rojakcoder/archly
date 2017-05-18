// Package archly (v0.6.0) is the Go implementation of the hierarchy-based
// access control list (ACL).
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

	ErrNilEntry = errors.New("nil entry")

	ErrNonEmpty = errors.New("non-empty registry")

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

// Acl is the public API for managing permissions, roles and resources.
type Acl struct {
	Perms *Permission
	Rres  *Registry
	Rrole *Registry
}

// NewAcl creates a new instance of the ACL.
//
// This is equivalent to `makeInstance()` in Java.
func NewAcl() *Acl {
	rol := NewRegistry()
	res := NewRegistry()
	perm := NewPermission(true)
	return &Acl{perm, res, rol}
}

// AddResource adds a resource to the registry.
//
// If the resource is already in the registry, a `ErrDupEntry` error is
// returned.
func (this *Acl) AddResource(e Entry) error {
	return this.Rres.Add(e.GetID())
}

// AddResourceParent performs the same function as AddResource with the
// difference that the resource is added under a parent resource.
//
// e is the child resource, p is the parent resource.
//
// If the parent resource is not in the registry, a `ErrEntryNotFound` error
// is returned.
func (this *Acl) AddResourceParent(e, p Entry) error {
	return this.Rres.AddChild(e.GetID(), p.GetID())
}

// AddRole adds a role to the registry.
//
// If the role is already in the registry, a `ErrDupEntry` error is
// returned.
func (this *Acl) AddRole(e Entry) error {
	return this.Rrole.Add(e.GetID())
}

// AddRoleParent performs the same function as AddRole with the difference
// that the role is added under a parent role.
//
// e is the child role, p is the parent role.
//
// If the parent role is not in the registry, a `ErrEntryNotFound` error
// is returned.
func (this *Acl) AddRoleParent(e, p Entry) error {
	return this.Rrole.AddChild(e.GetID(), p.GetID())
}

// AllowAllResource grants permission on all resources to the role.
func (this *Acl) AllowAllResource(role Entry) {
	this.Rrole.Add(role.GetID()) //ignore duplicate error
	this.Perms.Allow(role.GetID(), NewRootEntry().GetID())
}

// AllowAllRole grants permission on the resource to all roles.
func (this *Acl) AllowAllRole(res Entry) {
	this.Rres.Add(res.GetID())
	this.Perms.Allow(NewRootEntry().GetID(), res.GetID())
}

// Allow grants permission on the resource to the role.
func (this *Acl) Allow(role, res Entry) {
	this.Rrole.Add(role.GetID()) //ignore duplicate error
	this.Rres.Add(res.GetID())   //ignore duplicate error
	this.Perms.Allow(role.GetID(), res.GetID())
}

// AllowAction grants specific action permission on the resource to the role.
func (this *Acl) AllowAction(role, res Entry, action PermTypes) {
	permType := PermTypes(action)
	this.Rrole.Add(role.GetID()) //ignore duplicate error
	this.Rres.Add(res.GetID())   //ignore duplicate error
	this.Perms.AllowAction(role.GetID(), res.GetID(), permType)
}

// Clear resets all the registries to an empty state.
//
// Note that this also removes the default permission. If required, either
// MakeDefaultAllow or MakeDefaultDeny should be called after this method is
// invoked.
func (this *Acl) Clear() {
	this.Perms.Clear()
	this.Rres.Clear()
	this.Rrole.Clear()
}

// DenyAllResource denies permission on all resources to the role.
func (this *Acl) DenyAllResource(role Entry) {
	this.Rrole.Add(role.GetID()) //ignore duplicate error
	this.Perms.Deny(role.GetID(), NewRootEntry().GetID())
}

// DenyAllRole denies permission on the resource to all roles.
func (this *Acl) DenyAllRole(res Entry) {
	this.Rres.Add(res.GetID()) //ignore duplicate error
	this.Perms.Deny(NewRootEntry().GetID(), res.GetID())
}

// Deny denies permission on the resource to the role.
func (this *Acl) Deny(role, res Entry) {
	this.Rrole.Add(role.GetID()) //ignore duplicate error
	this.Rres.Add(res.GetID())   //ignore duplicate error
	this.Perms.Deny(role.GetID(), res.GetID())
}

// DenyAction denies specific action permission on the resource to the role.
func (this *Acl) DenyAction(role, res Entry, action PermTypes) {
	permType := PermTypes(action)
	this.Rrole.Add(role.GetID()) //ignore duplicate error
	this.Rres.Add(res.GetID())   //ignore duplicate error
	this.Perms.DenyAction(role.GetID(), res.GetID(), permType)
}

// ExportPermissions exports a snapshot of the permissions map.
//
// Returns a string-string-bool map for persistent storage.
func (this *Acl) ExportPermissions() map[string]map[string]bool {
	return this.Perms.Export()
}

// ExportResources exports a snapshot of the resources registry.
//
// Returns a string-string map for persistent storage.
func (this *Acl) ExportResources() map[string]string {
	return this.Rres.Export()
}

// ExportRoles exports a snapshot of the roles registry.
//
// Returns a string-string map for persistent storage.
func (this *Acl) ExportRoles() map[string]string {
	return this.Rrole.Export()
}

// ImportPermissions imports a new set of permissions.
//
// If the existing permissions map is not empty (including the default
// permission), the ErrNonEmpty error is returned.
func (this *Acl) ImportPermissions(p map[string]map[string]bool) error {
	if this.Perms.Size() != 0 {
		return ErrNonEmpty
	}
	this.Perms.ImportMap(p)
	return nil
}

// ImportResources imports a new hierarchy of resources.
//
// If the existing resource registry is not empty, the ErrNonEmpty error is
// returned.
func (this *Acl) ImportResources(r map[string]string) error {
	if this.Rres.Size() != 0 {
		return ErrNonEmpty
	}
	this.Rres.ImportRegistry(r)
	return nil
}

// ImportRoles imports a new hierarchy of roles.
//
// If the existing role registry is not empty, the ErrNonEmpty error is
// returned.
func (this *Acl) ImportRoles(r map[string]string) error {
	if this.Rrole.Size() != 0 {
		return ErrNonEmpty
	}
	this.Rrole.ImportRegistry(r)
	return nil
}

// IsAllowed determines if the role has access to the resource.
//
// Both role and res can be specified as `nil` to check for the default access.
//
// Returns true if the role has access on the resource, and false otherwise.
func (this *Acl) IsAllowed(role, res Entry) bool {
	var re, ro string

	if role != nil {
		ro = role.GetID()
	}
	if res != nil {
		re = res.GetID()
	}
	//get the traversal path for role
	rolePath := this.Rrole.TraverseRoot(ro)
	//get the traversal path for resource
	resPath := this.Rres.TraverseRoot(re)
	//check role-resource
	for _, aro := range rolePath {
		for _, aco := range resPath {
			expYes, expNo := this.Perms.IsAllowed(aro, aco)
			if expYes && !expNo {
				return true
			}
			if !expYes && expNo {
				return false
			}
			//else not specified, continue
		}
	}

	return false
}

// IsAllowedAction performs the same function as IsAllowed with the difference
// that a type may be specified to check for a specific type of access.
//
// `action` is a number ranging from 1 to 5. See PermTypes.
func (this *Acl) IsAllowedAction(role, res Entry, action PermTypes) bool {
	var re, ro string

	if role != nil {
		ro = role.GetID()
	}
	if res != nil {
		re = res.GetID()
	}
	//get the traversal path for role
	rolePath := this.Rrole.TraverseRoot(ro)
	//get the traversal path for resource
	resPath := this.Rres.TraverseRoot(re)
	permType := PermTypes(action)
	//check role-resource
	for _, aro := range rolePath {
		for _, aco := range resPath {
			expYes, expNo := this.Perms.IsAllowedAction(aro, aco, permType)
			if expYes && !expNo {
				return true
			}
			if !expYes && expNo {
				return false
			}
			//else not specified, continue
		}
	}

	return false
}

// IsDenied determines if the role is denied access to the resource.
//
// Both role and res can be specified as `nil` to check for the default access.
//
// Returns true if the role has access on the resource, and false otherwise.
func (this *Acl) IsDenied(role, res Entry) bool {
	var re, ro string

	if role != nil {
		ro = role.GetID()
	}
	if res != nil {
		re = res.GetID()
	}
	//get the traversal path for role
	rolePath := this.Rrole.TraverseRoot(ro)
	//get the traversal path for resource
	resPath := this.Rres.TraverseRoot(re)
	//check role-resource
	for _, aro := range rolePath {
		for _, aco := range resPath {
			expYes, expNo := this.Perms.IsDenied(aro, aco)
			if expYes && !expNo {
				return true
			}
			if !expYes && expNo {
				return false
			}
			//else not specified, continue
		}
	}

	return false
}

// IsDeniedAction performs the same function as IsDenied with the difference
// that a type may be specified to check for a specific type of access.
//
// `action` is a number ranging from 1 to 5. See PermTypes.
func (this *Acl) IsDeniedAction(role, res Entry, action PermTypes) bool {
	var re, ro string

	if role != nil {
		ro = role.GetID()
	}
	if res != nil {
		re = res.GetID()
	}
	//get the traversal path for role
	rolePath := this.Rrole.TraverseRoot(ro)
	//get the traversal path for resource
	resPath := this.Rres.TraverseRoot(re)
	permType := PermTypes(action)
	//check role-resource
	for _, aro := range rolePath {
		for _, aco := range resPath {
			expYes, expNo := this.Perms.IsDeniedAction(aro, aco, permType)
			if expYes && !expNo {
				return true
			}
			if !expYes && expNo {
				return false
			}
			//else not specified, continue
		}
	}

	return false
}

// MakeDefaultAllow makes the default permission allow, making it a blacklist.
func (this *Acl) MakeDefaultAllow() {
	this.Perms.MakeDefaultAllow()
}

// MakeDefaultDeny makes the default permission deny, making it a whitelist.
func (this *Acl) MakeDefaultDeny() {
	this.Perms.MakeDefaultDeny()
}

// Remove removes the permissions on the resource from the role.
func (this *Acl) Remove(role, res Entry) error {
	var ro, re string

	if role != nil {
		ro = role.GetID()
	}
	if res != nil {
		re = res.GetID()
	}
	return this.Perms.Remove(ro, re)
}

// RemoveAction removes the specific permission on the resource from the role.
func (this *Acl) RemoveAction(role, res Entry, action PermTypes) error {
	var ro, re string

	if role != nil {
		ro = role.GetID()
	}
	if res != nil {
		re = res.GetID()
	}
	permType := PermTypes(action)
	return this.Perms.RemoveAction(ro, re, permType)
}

// RemoveResource removes a resource and its permissions.
//
// Any permissions applicable to the resource are also removed. If the
// descendants are to be removed as well, the corresponding permissions are
// removed too.
//
// If `desc` is true, all descendant resources of this resource are also
// removed.
func (this *Acl) RemoveResource(resource Entry, desc bool) error {
	if resource == nil {
		return ErrNilEntry
	}
	res, e := this.Rres.Remove(resource.GetID(), desc)
	if e != nil {
		return e
	}
	for _, r := range res {
		this.Perms.RemoveByResource(r)
	}
	return nil
}

// RemoveRole removes a role and its permissions.
//
// Any permissions applicable to the role are also removed. If the descendants
// are to be removed as well, the corresponding permissions are removed
// too.
//
// If `desc` is true, all descendant roles of this role are also removed.
func (this *Acl) RemoveRole(role Entry, desc bool) error {
	if role == nil {
		return ErrNilEntry
	}
	rol, e := this.Rrole.Remove(role.GetID(), desc)
	if e != nil {
		return e
	}
	for _, r := range rol {
		this.Perms.RemoveByRole(r)
	}
	return nil
}

func (this *Acl) Visualize() string {
	var buf bytes.Buffer

	buf.WriteString(this.Rrole.String())
	buf.WriteString("\n")
	buf.WriteString(this.Rres.String())
	buf.WriteString("\n")
	buf.WriteString(this.Perms.String())
	buf.WriteString("\n")

	return buf.String()
}

func (this *Acl) VisualizePermissions() string {
	return this.Perms.String()
}

func (this *Acl) VisualizeResources(loader Entry) string {
	return this.Rres.Display(loader, "", "")
}

func (this *Acl) VisualizeRoles(loader Entry) string {
	return this.Rrole.Display(loader, "", "")
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

// NewRootEntry creates a simple Entry for the purpose of handling catch-all role/resource.
func NewRootEntry() Entry {
	return &rootEntry{}
}

type rootEntry struct {
}

func (this *rootEntry) GetID() string {
	return "*"
}

func (this *rootEntry) GetEntryDesc() string {
	return "ROOT"
}

func (this *rootEntry) RetrieveEntry(resID string) Entry {
	return nil
}
