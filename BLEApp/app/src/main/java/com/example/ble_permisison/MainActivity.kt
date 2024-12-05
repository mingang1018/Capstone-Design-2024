package com.example.ble_permisison

import ai.picovoice.porcupine.PorcupineManager
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.ble_permisison.BLE.BLEPermission
import com.example.ble_permisison.BLE.BleDisconnect
import com.example.ble_permisison.BLE.BleOnOff
import com.example.ble_permisison.BLE.BleScan
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private var connectedDeviceAddress: String? = null
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var porcupineManager: PorcupineManager

    lateinit var ttsIntent: TTSIntent
    lateinit var sttIntent: STTIntent

    var keywordDetectedListener: OnKeywordDetectedListener? = null


    private lateinit var modelPath: String // 초기화를 나중에 수행
    private lateinit var keywordPath: String // 초기화를 나중에 수행

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // modelPath와 keywordPath를 onCreate 내부에서 초기화
        modelPath = getAssetFilePath("porcupine_params_ko.pv")
        keywordPath = getAssetFilePath("scale_ko_android_v3_0_0.ppn")

        ttsIntent = TTSIntent(this)
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)


        // 현재 기기의 가로/세로 방향 구분
        val configuration: Configuration = resources.configuration

        // 기기의 기본 높이와 너비를 계산
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val display = getWindowManager().defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = resources.displayMetrics.density
        val dpHeight = outMetrics.heightPixels / density
        val dpWidth = outMetrics.widthPixels / density


        //dp별 layout 별도 적용
        Log.d("Device dp", "dpHeight : : $dpHeight  dpWidth : $dpWidth  density : $density")



        // 화면이 가로모드인지 확인
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 가로모드에서는 너비와 높이를 역전시킴
            setSizeForPortraitMode(width, height)
        } else {
            // 세로모드일 때는 그대로
            setSizeForPortraitMode(width, height)
        }

        setContentView(R.layout.activity_main)

        // Toolbar 설정
        val toolbar = findViewById<Toolbar>(R.id.toolBar)
        setSupportActionBar(toolbar)
        layourView.batteryImage = findViewById(R.id.battery_icon)
        layourView.batteryText = findViewById(R.id.battery_text)
        layourView.percent_text = findViewById(R.id.percentText)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_home,
            R.id.nav_weight,
            R.id.nav_recipe,
            R.id.nav_setting
        ))
        setupActionBarWithNavController(navController, appBarConfiguration)
        findViewById<BottomNavigationView>(R.id.bottom_nav_view).setupWithNavController(navController)

        // BLE 권한 확인
        if (!BLEPermission.hasPermissions(this)) {
            requestPermissions(BLEPermission.PERMISSIONS, BLEPermission.REQUEST_CODE_PERMISSIONS)
        } else {
            BleOnOff.checkAndEnableBluetooth(this) {
                BleScan.initializeBluetooth(BluetoothAdapter.getDefaultAdapter())
                startAutoConnect()
            }
        }

        // Porcupine 초기화
        initializePorcupine()
    }

    private fun initializePorcupine() {
        Log.d("PorcupineInit", "Initializing Porcupine with modelPath: $modelPath and keywordPath: $keywordPath")
        try {
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey("295yBhlD3t2XaDKUPzUlJTG/2Y7iB/BNzjuI5JrYhflrQ7sL0Uyoug==") // Access Key 추가
                .setModelPath(modelPath)
                .setKeywordPath(keywordPath)
                .setSensitivity(0.7f)
                .build(this) { keywordIndex ->
                    Log.d("Porcupine", "Keyword detected: 저울아")
//                    Toast.makeText(this, "\"저울아\" 키워드가 감지되었습니다!", Toast.LENGTH_SHORT).show()
                    startSpeechRecognition() //STT시작
                }
            Log.d("PorcupineStart","Porcupine Started")
            porcupineManager.start()
        } catch (e: Exception) {
            Log.e("PorcupineError", "Error initializing Porcupine: ${e.message}")
            Toast.makeText(this, "Porcupine 초기화 오류 발생", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        BleOnOff.checkAndEnableBluetooth(this) {
            BleScan.initializeBluetooth(BluetoothAdapter.getDefaultAdapter())
            startAutoConnect()
        }
        return item.onNavDestinationSelected(findNavController(R.id.nav_host_fragment))
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun setSizeForPortraitMode(width: Int, height: Int) {
        // 세로모드 기준에서의 크기 계산
        val deviceWidth = min(width, height)
        val deviceHeight = max(width, height)

        // 여기서 deviceWidth와 deviceHeight를 사용하여 dp 계산
        val density = resources.displayMetrics.density
        val widthInDp = deviceWidth / density

        // 600dp 기준으로 가로 모드 또는 세로 모드를 설정
        val thresholdDp = 600

        if (widthInDp >= thresholdDp) {
            // 가로 크기가 600dp 이상이면 가로 모드로 고정
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            // 가로 크기가 600dp 미만이면 세로 모드로 고정
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun startAutoConnect() {
        if (connectedDeviceAddress == null) {
            BleScan.startBleScan(this)
        } else {
            BleScan.reconnectToDevice(this, connectedDeviceAddress!!)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        BLEPermission.handlePermissionResult(
            this,
            requestCode,
            grantResults,
            onPermissionsGranted = {
                BleOnOff.checkAndEnableBluetooth(this) {
                    BleScan.initializeBluetooth(BluetoothAdapter.getDefaultAdapter())
                    startAutoConnect()
                }
            },
            onPermissionsDenied = {
                finish()
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        BleOnOff.handleBluetoothActivityResult(requestCode, resultCode, this) {
            BleScan.initializeBluetooth(BluetoothAdapter.getDefaultAdapter())
            startAutoConnect()
        }
        if (requestCode == REQUEST_CODE_STT && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = results?.get(0) ?: ""
            STTText.sttText = recognizedText
// 키워드 조건에 따라 Fragment로 이동
            when {
                STTText.sttText.contains("홈") || STTText.sttText.contains("메인 화면") -> {
                    findNavController(R.id.nav_host_fragment).navigate(R.id.nav_home)
                }
                STTText.sttText.contains("무게설정") || STTText.sttText.contains("무게 설정") -> {
                    // 무게 설정 Fragment로 이동
                    findNavController(R.id.nav_host_fragment).navigate(R.id.nav_weight)
                }
                STTText.sttText.contains("레시피") || STTText.sttText.contains("Recipe") -> {
                    // 레시피 Fragment로 이동
                    findNavController(R.id.nav_host_fragment).navigate(R.id.nav_recipe)
                }
                STTText.sttText.contains("사용자 설정") || STTText.sttText.contains("사용자설정") -> {
                    // 설정 Fragment로 이동
                    findNavController(R.id.nav_host_fragment).navigate(R.id.nav_setting)
                }
                STTText.sttText.contains("블루투스 연결") || STTText.sttText.contains("Bluetooth") -> {
                    if (!BleScan.bleConnect) {
                        startAutoConnect()
                    } else {
                        Toast.makeText(this, "이미 블루투스에 연결되어있습니다.", Toast.LENGTH_SHORT).show()
                        ttsIntent.speakText("이미 블루투스에 연결되어있습니다.")
                    }
                }

                STTText.sttText.contains("영점") || STTText.sttText.contains("0점") || STTText.sttText.contains("초점") || STTText.sttText.contains("영 점") -> {
                    STTText.terraButton.performClick()
                }

                (STTText.sttText.contains("반복 버튼") || STTText.sttText.contains("반복버튼") || STTText.sttText.contains("반복")) -> {
                    val weightFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                        ?.childFragmentManager?.primaryNavigationFragment as? WeightFragment

                    weightFragment?.let {
                        // loopButton 클릭 효과 실행
                        it.loopButton.performClick()


                        if(it.loopValue == true){
                            Toast.makeText(this, "반복 버튼이 활성화 되었습니다.", Toast.LENGTH_SHORT).show()
                            ttsIntent.speakText("반복 버튼이 활성화 되었습니다.")
                        }else{
                            Toast.makeText(this, "반복 버튼이 비 활성화 되었습니다.", Toast.LENGTH_SHORT).show()
                            ttsIntent.speakText("반복 버튼이 비 활성화 되었습니다.")
                        }
                    } ?: run {
                        Toast.makeText(this, "WeightFragment를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                STTText.sttText.contains("무게 입력") || STTText.sttText.contains("무게입력") ->{
                    val weightFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                        ?.childFragmentManager?.primaryNavigationFragment as? WeightFragment

                    weightFragment?.let {
                        it.micButton.performClick()
                    }
                }

                else -> {
                    Log.d("STTResult", "인식된 텍스트와 일치하는 키워드가 없습니다.")
                }
            }
            Log.d("STTResult", "Recognized text: $recognizedText")
            getCurrentFragment()?.onKeywordRecognized(recognizedText)
        }
    }

    private fun getCurrentFragment(): VoiceCommandReceiver? {
        return supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            ?.childFragmentManager
            ?.primaryNavigationFragment as? VoiceCommandReceiver
    }

    override fun onDestroy() {
        super.onDestroy()
        BleDisconnect.disconnectFromDevice(this)
        porcupineManager.stop()
        porcupineManager.delete()
    }

    private fun startSpeechRecognition() {
        Log.d("Keyword Detect","KeyWord Detect")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "명령을 말해주세요.")
        }
        startActivityForResult(intent, REQUEST_CODE_STT)
    }

    private fun getAssetFilePath(fileName: String): String {
        val file = File(cacheDir, fileName)
        if (!file.exists()) {
            try {
                assets.open(fileName).use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d("FileCopy", "File copied to cache directory: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e("FileCopyError", "Error copying asset file: ${e.message}")
            }
        } else {
            Log.d("FileCopy", "File already exists in cache: ${file.absolutePath}")
        }

        // 파일이 읽을 수 있는 상태인지 확인
        if (!file.canRead()) {
            Log.e("FilePermissionError", "Cannot read file: ${file.absolutePath}")
        }

        return file.absolutePath
    }


    fun saveConnectedDeviceAddress(address: String) {
        connectedDeviceAddress = address
    }


    companion object {
        private const val REQUEST_CODE_STT = 1
    }

    interface OnKeywordDetectedListener {
        fun onKeywordDetected()
    }
}
