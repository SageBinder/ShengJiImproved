package com.sage.shengji.utils.shengji;

public enum Team {
    COLLECTORS,
    KEEPERS,
    NO_TEAM;

    @Override
    public String toString() {
        switch(this) {
        case COLLECTORS:
            return "Collectors";
        case KEEPERS:
            return "Keepers";
        case NO_TEAM:
            return "No team";
        default:
            return "";
        }
    }
}
