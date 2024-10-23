package com.matrizos.matrizos_simple_api.bluetooth;

import com.matrizos.matrizos_simple_api.security.BluetoothPermission;
import lombok.Getter;

import java.lang.annotation.Native;

public final class Bluetooth {
    private static synchronized native void searchDevices();
    private static synchronized native void connectDevice();
    private static synchronized native void directConnection(String macAddress);

    @Native @Getter private static int limitDevices = 5;

    @SuppressWarnings("removal")
    public void setLimitDevices(int limitDevices) throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null)
            sm.checkPermission(BluetoothPermission.ALT_DEVICES_LIMIT);
        Bluetooth.limitDevices = limitDevices;
    }
}
