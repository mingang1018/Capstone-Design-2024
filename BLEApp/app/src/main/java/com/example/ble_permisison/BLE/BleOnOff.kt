package com.example.ble_permisison.BLE

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity

object BleOnOff{
    private const val REQUEST_ENABLE_BT = 2 // 블루투스를 켜기 위한 요청 코드

    fun checkAndEnableBluetooth(activity: AppCompatActivity, onBluetoothEnabled: () -> Unit) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "Bluetooth 스캔 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(activity, "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show()
            activity.finish()
        } else {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                onBluetoothEnabled()
            }
        }
    }

    fun handleBluetoothActivityResult(
        requestCode: Int,
        resultCode: Int,
        activity: AppCompatActivity,
        onBluetoothEnabled: () -> Unit
    ) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                Toast.makeText(activity, "블루투스가 켜졌습니다.", Toast.LENGTH_SHORT).show()
                onBluetoothEnabled()
            } else {
                Toast.makeText(activity, "블루투스가 꺼졌습니다.", Toast.LENGTH_SHORT).show()
                activity.finish()
            }
        }
    }
}