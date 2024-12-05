package com.example.ble_permisison.BLE

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import com.example.ble_permisison.MainActivity
import com.example.ble_permisison.BLE.BleConstants.setDeviceName
import com.example.ble_permisison.HomeFragment
import com.example.ble_permisison.bleData
import java.util.*



object BleScan {
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    var receivedData: String? = null
    var recipeBoolen: Boolean = false

    var bleConnect: Boolean = false


    // 주어진 BluetoothAdapter를 사용하여 Bluetooth 스캐너를 초기화합니다.
    fun initializeBluetooth(bluetoothAdapter: BluetoothAdapter) {
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    }

    // BLE 장치 검색을 시작합니다.
    fun startBleScan(activity: MainActivity) {
        // 필요한 BLE 권한을 확인합니다.
        if (!BLEPermission.hasPermissions(activity)) {
            BLEPermission.requestPermissions(activity)
            return
        }

        try {
            // BLE 스캔을 시작하고 스캔 시작을 알리는 토스트 메시지를 표시합니다.
            bluetoothLeScanner.startScan(scanCallback(activity))
            Toast.makeText(activity, "BLE 스캔 시작", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            // 권한이 없어 스캔을 시작할 수 없는 경우 처리합니다.
            Log.e("BLE_SCAN", "권한이 없어 스캔을 시작할 수 없습니다.", e)
            Toast.makeText(activity, "권한이 없어 스캔을 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 스캔 중 발견된 장치를 처리하는 ScanCallback을 생성합니다.
    private fun scanCallback(activity: MainActivity) = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            // Android 12+에서 Bluetooth 연결 권한 문제를 처리합니다.
            if (Build.VERSION.SDK_INT >= 31 &&
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(activity, "Bluetooth 연결 권한이 필요합니다.", Toast.LENGTH_SHORT).apply {
                    setGravity(Gravity.CENTER, 0, 0)
                    show()
                }
                return
            }

            // 발견된 Bluetooth 장치를 가져옵니다.
            val device = result.device
            val deviceName = device.name ?: "Unknown"
            Log.d("BLE_SCAN", "장치 발견: $deviceName - ${device.address}")

            // 발견된 장치가 원하는 장치인지 확인합니다.
            if (deviceName == setDeviceName) {
                // 원하는 장치를 찾으면 스캔을 중지합니다.
                bluetoothLeScanner.stopScan(this)
                connectToDevice(device, activity)
                activity.saveConnectedDeviceAddress(device.address)
            }
        }

        // 스캔 실패를 처리합니다.
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("BLE_SCAN", "스캔 실패: $errorCode")
        }
    }

    // 특정 Bluetooth 장치에 연결합니다.
    private fun connectToDevice(device: BluetoothDevice, activity: MainActivity) {
        // 필요한 BLE 권한을 확인합니다.
        if (!BLEPermission.hasPermissions(activity)) {
            BLEPermission.requestPermissions(activity)
            return
        }

        // 지정된 장치에 연결을 시도합니다.
        Toast.makeText(activity, "${setDeviceName} 장치와 연결 시도 중...", Toast.LENGTH_SHORT).show()
        try {
            // GATT 연결을 설정합니다.
            bluetoothGatt = device.connectGatt(activity, false, object : BluetoothGattCallback() {
                // 연결 상태 변경을 처리합니다.
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d("BLE_CONNECT", "GATT 서버에 연결되었습니다.")
                        bleConnect = true
                        activity.runOnUiThread {
                            Toast.makeText(activity, "${setDeviceName} 장치와 연결되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                        gatt.discoverServices() // 연결 후 서비스 검색
                        BleDisconnect.initializeGatt(gatt) // 연결 해제 관리를 위해 GATT 저장
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        bleConnect = false
                        Log.d("BLE_CONNECT", "GATT 서버와의 연결이 끊어졌습니다.")
                        activity.runOnUiThread {
                            Toast.makeText(activity, "${setDeviceName} 장치와의 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // 서비스 검색 결과를 처리합니다.
                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d("BLE_SERVICES", "서비스가 발견되었습니다: ${gatt.services}")
                        enableNotifications(gatt, activity) // 특성에 대한 알림 활성화
                    } else {
                        Log.e("BLE_SERVICES", "서비스 검색 실패: $status")
                    }
                }

                // BLE 장치로부터 들어오는 데이터를 처리합니다.
                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {

                    receivedData = characteristic.value.toString(Charsets.UTF_8)
                    Log.d("BLE_DATA_RECEIVED", "데이터 수신됨: $receivedData")

                    receivedData?.let { data->
                        if(data.isNotBlank()){
                            processReceivedData(data)
                        }
                    }
                }
            })
        } catch (e: SecurityException) {
            Log.e("BLE_CONNECT", "권한이 없어 연결할 수 없습니다.", e)
        }
    }
    // 받은 데이터를 처리하는 메서드
    private fun processReceivedData(data: String) {
        // 여기에서 받은 데이터를 처리하는 로직을 추가하세요. 예: 서버 전송, 로컬 저장 등.
        Log.d("BLE_PROCESS", "처리된 데이터: $data")
        val dataArray = data.split(",").map { it.trim().toInt() }
        if(dataArray.size == 6){
            bleData.batteryData = dataArray[0]
            bleData.scaleData = dataArray[1]
            bleData.setScaleData = dataArray[2]
            bleData.settingSuccessData = dataArray[3]
            bleData.recipeNextData = dataArray[4]
            bleData.setloopData = dataArray[5]
        } else{
            null
        }

    }

    // 특정 특성에 대한 알림을 활성화합니다.
    private fun enableNotifications(gatt: BluetoothGatt, activity: MainActivity) {
        // Android 12+에서 Bluetooth 연결 권한 문제를 처리합니다.
        if (Build.VERSION.SDK_INT >= 31 &&
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(activity, "Bluetooth 연결 권한이 필요합니다.", Toast.LENGTH_SHORT).apply {
                setGravity(Gravity.CENTER, 0, 0)
                show()
            }
            return
        }

        // 알림이 활성화된 특성을 가져옵니다.
        val characteristic = gatt.getService(BleConstants.SERVICE_UUID)?.getCharacteristic(
            BleConstants.CHARACTERISTIC_UUID_TX)
        if (characteristic != null) {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor) // 알림을 활성화하기 위해 디스크립터를 씁니다.
        }
    }

    // 이전에 연결된 장치의 주소를 사용하여 장치에 다시 연결합니다.
    fun reconnectToDevice(activity: MainActivity, deviceAddress: String) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (device != null) {
            connectToDevice(device, activity)
        } else {
            Toast.makeText(activity, "이전 연결된 장치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            startBleScan(activity) // 이전 장치를 찾지 못한 경우 새 스캔 시작
        }
    }

    // 특정 특성을 사용하여 BLE 장치로 데이터를 전송합니다.
    fun sendDataToBleDevice(activity: AppCompatActivity, data: String) {
        // 필요한 BLE 권한을 확인합니다.
        if (!BLEPermission.hasPermissions(activity)) {
            BLEPermission.requestPermissions(activity)
            return
        }

        // 데이터를 전송하기 위한 특성을 가져옵니다.
        val characteristic = bluetoothGatt?.getService(BleConstants.SERVICE_UUID)?.getCharacteristic(
            BleConstants.CHARACTERISTIC_UUID_RX)
        if (characteristic != null) {
            characteristic.value = data.toByteArray(Charsets.UTF_8)
            try {
                bluetoothGatt?.writeCharacteristic(characteristic) // 특성에 데이터를 씁니다.
                Log.d("BLE_WRITE", "데이터 전송됨: $data")
            } catch (e: SecurityException) {
                Log.e("BLE_WRITE", "권한이 없어 데이터를 전송할 수 없습니다.", e)
            }
        } else {
            Log.e("BLE_WRITE", "특성을 찾을 수 없거나 GATT가 연결되지 않음")
        }
    }


}
