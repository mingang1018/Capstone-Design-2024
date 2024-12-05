package com.example.ble_permisison.BLE

import java.util.UUID

object BleConstants {
    val SERVICE_UUID: UUID = UUID.fromString("ca42a720-458f-4d55-b6bd-8df890f64ea0")
    val CHARACTERISTIC_UUID_RX: UUID = UUID.fromString("ca42a721-458f-4d55-b6bd-8df890f64ea0")
    val CHARACTERISTIC_UUID_TX: UUID = UUID.fromString("ca42a722-458f-4d55-b6bd-8df890f64ea0")
    val setDeviceName = "CapstonDevice"
}
