package com.matrizos.matrizos_simple_api.security;

import java.security.Permission;

public final class BluetoothPermission extends Permission {
    public static final BluetoothPermission GET_BATTERY_PERCENT = new BluetoothPermission("Get Battery Percent");
    public static final BluetoothPermission UNKNOWN_MESSAGE = new BluetoothPermission("Unknown Message");
    public static final BluetoothPermission ALT_DEVICES_LIMIT = new BluetoothPermission("Unknown Message");

    private BluetoothPermission(String name) {
        super(name);
    }

    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getActions() {
        return "";
    }
}
