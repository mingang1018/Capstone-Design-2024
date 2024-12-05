package com.example.ble_permisison.BLE

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.ble_permisison.MainActivity

object BLEPermission {
    // 필요한 권한 배열
    val PERMISSIONS = if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.RECORD_AUDIO,
        )
    }

    // 권한 요청 코드
    const val REQUEST_CODE_PERMISSIONS = 100

    // 권한이 있는지 확인하는 메서드
    fun hasPermissions(context: Context, permissions: Array<String> = PERMISSIONS): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    // 권한 요청 결과 처리
    fun handlePermissionResult(
        context: Context,
        requestCode: Int,
        grantResults: IntArray,
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: () -> Unit
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(context, "권한 승인", Toast.LENGTH_SHORT).show()
                onPermissionsGranted()
            } else {
                Toast.makeText(context, "권한 거부", Toast.LENGTH_SHORT).show()
                onPermissionsDenied()
            }
        }
    }
    fun requestPermissions(activity: AppCompatActivity) {
        ActivityCompat.requestPermissions(activity, PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }
}
