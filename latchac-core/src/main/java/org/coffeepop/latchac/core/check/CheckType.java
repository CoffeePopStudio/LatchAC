package org.coffeepop.latchac.core.check;

/** Check categories. Used to organize checks in the registry and log output. */
public enum CheckType {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    WORLD("World"),
    INVENTORY("Inventory"),
    EXPLOIT("Exploit"),
    MISC("Misc");

    private final String displayName;
    CheckType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
