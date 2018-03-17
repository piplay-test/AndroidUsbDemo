package com.piplay.myapplication;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private static final String _ACTION_USB_PERMISSION = "com.piplay.myapplication.USB_PERMISSION";
    private final BroadcastReceiver _BROADCAST_RECEIVER = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (usbDevice != null) {
                synchronized (this) {
                    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                        onUsbAttached(usbDevice, intent);
                    } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                        onUsbDetached(usbDevice, intent);
                    } else if (_ACTION_USB_PERMISSION.equals(action)) {
                        onUsbPermission(usbDevice, intent);
                    }
                }
            }
        }
    };

    private UsbManager _usbManager;
    private TextView _textViewOne;
    private TextView _textViewSecond;
    private UsbDevice _usbDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        _textViewOne = findViewById(R.id.text_one);
        _textViewSecond = findViewById(R.id.text_second);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(_ACTION_USB_PERMISSION);
        registerReceiver(_BROADCAST_RECEIVER, intentFilter);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    void updateTextSecond(String text) {
        _textViewSecond.setText(text);
        _textViewSecond.invalidate();
        Log.d("[USB]", "* " + text);
    }

    void onClickSearchUsb(final View view) {
        synchronized (this) {
            HashMap<String, UsbDevice> deviceList = _usbManager.getDeviceList();
            String msg = deviceList.size() + " USB device(s) found";
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
            while (deviceIterator.hasNext()) {
                msg += "\n\t";
                UsbDevice device = deviceIterator.next();
                _usbDevice = device;
                if (_usbManager.hasPermission(device)) {
                    msg += "+HasPermission - ";
                } else {
                    msg += "+NoPermission - ";
                }
                msg += device;
            }
            _textViewOne.setText(msg);
            _textViewOne.invalidate();
            Log.d("[USB]", "* " + msg);
        }
    }

    void onClickRequestUsbPermission(final View view) {
        synchronized (this) {
            if (_usbDevice != null) {
                updateTextSecond("RequestPermission-" + _usbDevice);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent(_ACTION_USB_PERMISSION), PendingIntent.FLAG_ONE_SHOT);
                _usbManager.requestPermission(_usbDevice, pendingIntent);
            }
        }
    }

    void onClickOpenDevice(final View view) {
        if (_usbDevice != null) {
            UsbDeviceConnection conn = _usbManager.openDevice(_usbDevice);
            if (conn != null) {
                updateTextSecond("OpenDevice-Good-" + _usbDevice);
                conn.close();
            } else {
                updateTextSecond("OpenDevice-Wrong-" + _usbDevice);
            }
        } else {
            updateTextSecond("OpenDevice-NoDevice Yet to Open!!");
        }
    }

    void onUsbAttached(UsbDevice usbDevice, Intent intent) {
        _usbDevice = usbDevice;
        if (_usbManager.hasPermission(usbDevice)) {
            updateTextSecond("ATTACHED+HasPermission-" + usbDevice);
        } else {
            updateTextSecond("ATTACHED+NoPermission-" + usbDevice);
        }
    }

    void onUsbDetached(UsbDevice usbDevice, Intent intent) {
        updateTextSecond("DEATTCHED-" + usbDevice);
        if (_usbDevice == usbDevice) _usbDevice = null;
    }

    void onUsbPermission(UsbDevice usbDevice, Intent intent) {
        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            updateTextSecond("PERMISSION+Granted-" + usbDevice);
        } else {
            updateTextSecond("PERMISSION+NotGranted-" + usbDevice);
        }
    }
}
