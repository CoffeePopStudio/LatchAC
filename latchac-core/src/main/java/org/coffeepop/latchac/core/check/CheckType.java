package org.coffeepop.latchac.core.check;

public enum CheckType {
    COMBAT("战斗"),
    MOVEMENT("移动"),
    PLAYER("玩家"),
    WORLD("世界"),
    INVENTORY("物品栏"),
    EXPLOIT("漏洞利用");

    private final String displayName;
    CheckType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
