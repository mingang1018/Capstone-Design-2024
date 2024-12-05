package com.example.ble_permisison.BLE

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.ble_permisison.BLE.BleConstants.setDeviceName
import com.example.ble_permisison.MainActivity

object BleDisconnect {
    private var bluetoothGatt: BluetoothGatt? = null

    // Initialize the BluetoothGatt instance in this object
    fun initializeGatt(gatt: BluetoothGatt?) {
        bluetoothGatt = gatt
    }

    // Disconnect from the connected BLE device
    fun disconnectFromDevice(activity: MainActivity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "Bluetooth 연결 권한이 필요합니다.", Toast.LENGTH_SHORT).apply {
                show()
            }
            return
        }

        // If connected, disconnect and clean up resources
        bluetoothGatt?.let {
            it.disconnect()  // Disconnect the GATT connection
            it.close()       // Close the GATT object to release resources
            bluetoothGatt = null  // Reset the GATT instance to null
            Toast.makeText(activity, "${setDeviceName} 장치와의 연결을 해제했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
