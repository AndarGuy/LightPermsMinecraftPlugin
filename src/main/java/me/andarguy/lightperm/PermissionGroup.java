package me.andarguy.lightperm;

import java.util.List;

public class PermissionGroup {
    private String name;
    private List<String> permissions;
    private String inherit;

    public PermissionGroup(String name, List<String> permissions) {
        this.name = name;
        this.permissions = permissions;
    }


    public PermissionGroup(String name, List<String> permissions, String inherit) {
        this.name = name;
        this.permissions = permissions;
        this.inherit = inherit;
    }

    public String getName() {
        return name;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public String getInherit() {
        return inherit;
    }

    public boolean hasInherit() {
        return inherit != null;
    }
}
