package com.mxn.plugin.terrier;


import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.android.sdk.AndroidSdkUtils;

import java.util.*;

public class DeviceService {
    private AndroidDebugBridge debugBridge;
    private final Project project;

    public DeviceService(Project project) {
        this.project = project;
        debugBridge = AndroidSdkUtils.getDebugBridge(project);
    }

    public void startService() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(() -> task());
            }
        }, 0, 1000));
    }

    private void task() {
        if (debugBridge != null) {
            IDevice[] devices = debugBridge.getDevices();
            if (devicesListener != null) {
                devicesListener.findDevices(devices);
            }
        } else {
            debugBridge = AndroidSdkUtils.getDebugBridge(project);
            if (devicesListener != null) {
                devicesListener.adbNotFind();
            }
        }
    }

    private DevicesListener devicesListener;

    public void setDevicesListener(DevicesListener devicesListener) {
        this.devicesListener = devicesListener;
    }

    public interface DevicesListener {
        void findDevices(IDevice[] devices);

        void adbNotFind();
    }
}
