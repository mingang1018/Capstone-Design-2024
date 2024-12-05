#include "HX711.h"
#include <Wire.h>
#include <SPI.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1305.h>
#include <LiquidCrystal_I2C.h>
#include "bitmap.h" // 비트맵 데이터 포함
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h> 
#include <BLE2902.h>  //ble 관련 라이브러리들 헤더
#include <HardwareSerial.h> // Hardware serial


// HX711 circuit wiring
#define LOADCELL_DOUT_PIN 16
#define LOADCELL_SCK_PIN 17

#define TARE_BTN_PIN 2

// Define SPI pins for ESP32 HSPI
#define OLED_SCLK 18 // SCK BLUE
#define OLED_MOSI 23 // MOSI GREEN
#define OLED_CS 4 // CS YELLOW
#define OLED_RESET 33 // RST 주황
#define OLED_DC 14 // DC WHITE

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define SSD1305_WHITE 1

// Hardware serial Pins
#define RX1 26
#define TX1 27

const int CS = 5;

const int movingAverageWindowSize = 10;
float batteryVoltages[movingAverageWindowSize];
int voltageIndex = 0;
float voltageSum = 0.0;

// BLE 전송을 위한 배열 크기 설정
const int txDataArraySize = 6; // 필요한 크기로 설정 (6개 요소)
int txDataArray[txDataArraySize];


// Define the ADC pin connected to the battery voltage divider
const int batteryPin = 34; // GPIO34 (ADC1_6)

// Define the reference voltage (3.3V) and the ADC resolution (12-bit for ESP32)
const float referenceVoltage = 3.3;
const int adcResolution = 4096;

bool loopOnOff = false;

// Define the resistors used in the voltage divider
const float resistor1 = 14760.0; // 10kΩ resistor (connected to battery positive terminal)
const float resistor2 = 14470.0; // 10kΩ resistor (connected to ground)

// Define LED pins

#define LED_PIN 32

float setup_weight = 0;
float percent_weight = 0;
float scale_value = 0;
float terra_button = 0;

bool setReset = false;

int success_count = 0;
int data_success_count = 0;

int recipeCount = 0;
bool ble_recipeStep = false;
bool loopSetScale = false;

// set the LCD number of columns and rows
int lcdColumns = 16;
int lcdRows = 2;

int minus_scale_count = 0;

// set LCD address, number of columns and rows
// if you don't know your display address, run an I2C scanner sketch
LiquidCrystal_I2C lcd(0x27, lcdColumns, lcdRows);  

// scale - 10Kg loadcell : 226 / 5kg loadcell : 372
// ADC 모듈에서 측정된 결과값을 (loadcellValue)값 당 1g으로 변환해 줌
float loadcellValue = 216; // 로드셀 캘리브레이션을 위한 
int display_mode = 0; //0: mless, 1: less, 2: equal , 3: more, 4: mmore
int current_mode = -1;
int dataIndex = 0;
int dataArray[20];
int dataCount = 0;

bool success_ble = false;
HX711 scale;

int battery_test = 84;

// hardware SPI - use 4Mhz (4000000UL) or lower because the screen is rated for 4MHz, or it will remain blank!
Adafruit_SSD1305 display(128, 64, &SPI, OLED_DC, OLED_RESET, OLED_CS, 4000000UL);

// BLE 관련
BLEServer *pServer = NULL;             // ESP32의 서버(Peripheral, 주변기기) 클래스
BLECharacteristic *pTxCharacteristic;  // ESP32에서 데이터를 전송하기 위한 캐릭터리스틱
bool deviceConnected = false;          // BLE 연결 상태 저장
bool oldDeviceConnected = false;       // BLE 이전 연결 상태 저장
uint8_t txValue = 0;
int recipeDataValue = 0; //on: 1 off: 0


// 서비스 UUID 및 캐릭터리스틱 UUID 설정
#define SERVICE_UUID           "ca42a720-458f-4d55-b6bd-8df890f64ea0" // 서비스 UUID
#define CHARACTERISTIC_UUID_RX "ca42a721-458f-4d55-b6bd-8df890f64ea0" // RX 캐릭터리스틱 UUID (앱이 데이터를 보냄)
#define CHARACTERISTIC_UUID_TX "ca42a722-458f-4d55-b6bd-8df890f64ea0" // TX 캐릭터리스틱 UUID (ESP32가 데이터를 보냄)

// UART1 초기화
HardwareSerial MySerial(1);


// ESP32 연결 상태 콜백함수
class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
      // onConnect 연결 되는 시점에 호출 됨
      Serial.println("Connected");
      deviceConnected = true;
  };

  void onDisconnect(BLEServer* pServer) {
      // onDisconnect 연결이 해제되는 시점에 호출 됨
      Serial.println("Disconnected");
      deviceConnected = false;
  }
};

class MyCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
      // 외부에서 데이터를 보내오면 호출됨 
      // 보내온 데이터를 변수에 데이터 저장
      String rxValue = pCharacteristic->getValue().c_str(); 

      // 데이터가 있다면..
      if (rxValue.length() > 0) {
          Serial.println("*********");
          Serial.print("Received Value: ");
          Serial.println(rxValue);

          // "RecipeData"와 일치하는지 확인
          if (rxValue == "RecipeData") {
              // 데이터가 "RecipeData"이면 배열 관련 변수 초기화
              Serial.println("Recipadata!! Initializing array...");
              recipeDataValue = 1; // 다음에 올 데이터가 레시피 데이터임을 표시
              
              // 배열 관련 변수 초기화
              dataIndex = 0;
              dataCount = 0;
              memset(dataArray, 0, sizeof(dataArray)); // 배열 초기화
              
              Serial.println("Data array initialized.");
          } else if (recipeDataValue == 1) {
              // 레시피 데이터를 수신 중이면 데이터를 배열로 처리
              dataIndex = 0; // 배열 인덱스를 초기화
              char* token = strtok((char*)rxValue.c_str(), ",");
              while (token != nullptr && dataIndex < 20) { // 최대 20개의 숫자를 처리
                  dataArray[dataIndex++] = atof(token); // 문자열을 float로 변환하여 배열에 저장
                  token = strtok(nullptr, ","); // 다음 토큰으로 이동
              }

              // 배열의 데이터를 출력
              if (dataIndex > 0) {
                  Serial.println("Parsed Values:");
                  for (int i = 0; i < dataIndex; i++) {
                      Serial.println(dataArray[i]); // 각 값을 시리얼 모니터에 출력
                  }
              } else {

              }

              // 데이터를 처리한 후 플래그 초기화
              recipeDataValue = 0;
          } else if (rxValue == "Terra") {
              // 데이터가 "Terra"이면 특정 동작 수행
              terra_button = 1;
          } else if( rxValue == "LoopOn"){
            Serial.println("BLE Loop On");
            loopOnOff = true;
          } else if ( rxValue == "LoopOff"){
            Serial.println("BLE Loop Off");
            loopOnOff = false;
          }
          else {
              // 다른 데이터를 수신했을 경우 해당 데이터를 float로 변환하여 처리
              setup_weight = rxValue.toFloat(); // 문자열을 float로 변환
              Serial.print("Setup Weight: ");
              Serial.println(setup_weight);
          }

          Serial.println("*********");
      }
  }
};




void setup() {
    Serial.begin(115200);
    

    MySerial.begin(9600, SERIAL_8N1, RX1, TX1); // RX(GPIO26), TX(GPIO27)
    

    // put your setup code here, to run once:
    pinMode(batteryPin, INPUT); // battery adc pin
    pinMode(TARE_BTN_PIN, INPUT_PULLUP); // 내부 풀업 저항 사용
    //LED 
    pinMode(LED_PIN, OUTPUT);
    // initialize LCD
    lcd.init();
    // turn on LCD backlight                      
    lcd.backlight();

    for (int i = 0; i < movingAverageWindowSize; i++) {
        batteryVoltages[i] = readBatteryVoltage();
        voltageSum += batteryVoltages[i];
    }

    while (!Serial) delay(100);
    Serial.println("SSD1305 OLED test");

    if (!display.begin(0x2)) {
        Serial.println("Unable to initialize OLED");
        while (1) yield();
    }

    Serial.println("Display initialized successfully!");


    

    display.clearDisplay();   // clears the screen and buffer
    display.setTextSize(1);
    display.setTextColor(WHITE);
    display.setCursor(0, 0);
    display.println("Initializing...");
    display.display();
    display_mode = 0;
    current_mode = -1;
    // 로드셀 HX711 보드 pin 설정
    scale.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);

    // 부팅 후 잠시 대기 (2초)
    delay(2000);

    // 측정값 1회 읽어오기
    Serial.print("read: \t\t\t");
    Serial.println(scale.read());

    delay(1000);

    // 스케일 설정
    scale.set_scale(loadcellValue);
    
    // 오프셋 설정(10회 측정 후 평균값 적용) - 저울 위에 아무것도 없는 상태를 0g으로 정하는 기준점 설정(저울 위에 아무것도 올려두지 않은 상태여야 합니다.)   
    scale.tare(20);    

    // 설정된 오프셋 및 스케일 값 확인
    Serial.print("Offset value :\t\t");
    Serial.println(scale.get_offset());
    Serial.print("Scale value :\t\t");
    Serial.println(scale.get_scale());

    // (read - offset) 값 확인 (scale 미적용)
    Serial.print("(read - offset) value: \t");  
    Serial.println(scale.get_value());
    delay(2000);

    // BLE 초기화 및 설정
    BLEDevice::init("CapstonDevice");

    // 서버(Peripheral, 주변기기) 생성
    pServer = BLEDevice::createServer();
    // 연결 상태(연결/해제) 콜백함수 등록
    pServer->setCallbacks(new MyServerCallbacks());

    // 서비스 UUID 등록 
    BLEService *pService = pServer->createService(SERVICE_UUID);

    // ESP32에서 외부로 데이터 보낼 캐릭터리스틱 생성 (Tx)
    // 캐릭터리스틱의 속성은 Notify만 가능하게 함
    pTxCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID_TX,
        BLECharacteristic::PROPERTY_NOTIFY
    );
    // Client(Central, 모바일 기기 등)에서 pTxCharacteristic 속성을 읽거나 설정할 수 있게 UUID 2902를 등록
    // Client가 ESP32에서 보내는 데이터를 받기 위해 해당 설정이 필요함.
    pTxCharacteristic->addDescriptor(new BLE2902());

    // Client가 ESP32로 보내는 캐릭터리스틱 생성 (Rx)
    // write 속성 활성
    BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID_RX,
        BLECharacteristic::PROPERTY_WRITE
    );
    // pRxCharacteristic에 client가 보낸 데이터를 처리할 콜백 함수 등록
    pRxCharacteristic->setCallbacks(new MyCallbacks());

    // 서비스 시작
    // 아직 외부에 표시가 안됨
    pService->start();

    // 어드버타이징 시작
    // 이때 모바일에서 스캔하면 표시됨.
    pServer->getAdvertising()->start();
    Serial.println("Waiting for a client connection to notify...");
}

void loop() {
    // put your main code here, to run repeatedly:
    float batteryVoltage = getSmoothedBatteryVoltage();
    int batteryPercentage = calculateBatteryPercentage(batteryVoltage);
    Serial.print("Battery percentage: ");
    Serial.println(batteryPercentage);


    SwitchCheck(); // tare 버튼이 눌러지는지 확인하는 함수
    // set cursor to first column, first row
    scale_value = scale.get_units(10); //평균값 계산 코드(HX7a11 제공) //10회 측정 평균값
    scale_value = round(scale_value*10)/10;
    // 오프셋 및 스케일이 적용된 측정값 출력 (5회 측정 평균값) 
    if (abs(scale_value) < 0.5) scale_value = 0; 

    if(deviceConnected == false){
      digitalWrite(LED_PIN, LOW);
    }

    else if(deviceConnected == true){
      digitalWrite(LED_PIN, HIGH);
    }

    //UART1 
    MySerial.println(scale_value,0);
    // 시리얼 출력
    Serial.print("value :");
    Serial.print(scale_value, 0);    // 10회 측정 평균값, 소수점 아래 0자리 출력
    Serial.println("g");
    lcd.setCursor(0, 0);  

    if(scale_value< 0){
      minus_scale_count++;
    } else{
      minus_scale_count = 0;
    }

    lcd.print("                ");
    if(dataIndex == 0){ //일반 설정
      if(setup_weight != 0){
        lcd.setCursor(0, 0);
        lcd.print(scale_value, 0);
        lcd.print("g   /   ");
        lcd.print(setup_weight, 0);
        lcd.print("g");

      } else{
        lcd.setCursor(0, 0);
        lcd.print(scale_value, 0);
        lcd.print("g");
      }
    } 
    
    if (success_count > 10) {
      if(loopOnOff == false){
        setup_weight = 0;
        success_count = 0; // Reset success_count after success
        display_mode = -1;
        // BLE transmission logic
        if (deviceConnected) {
          success_ble = true; //만약 값이 성공하면 활성화
        }
      } else{
        terra_button = 1;
        SwitchCheck();
        success_count = 0;
      }
        delay(500);
      }
    
    if(dataIndex == 0){
      if (setup_weight != 0) {
          Serial.print(setup_weight, 0);
          oledDisplay(setup_weight, scale_value);
      } else {
          notSetOLED(scale_value, 5000);
          current_mode = -1;
      }
    } else{
        
        if(setReset == false){
          setup_weight = 0;
          setReset = true;
        }

        // dataArray에 값이 있고 아직 처리 중인 경우
        if (dataIndex > 0 && setup_weight == 0 && dataCount < dataIndex) {
            // 배열의 다음 요소를 setup_weight에 할당
            setup_weight = dataArray[dataCount];
            Serial.print("New Setup Weight: ");
            Serial.println(setup_weight);
            
        }

        // setup_weight가 설정된 상태에서 디스플레이 로직 처리
        if (setup_weight != 0) {  
            oledDisplay(setup_weight, scale_value);
            Serial.print("Setup Weight: ");
            Serial.println(setup_weight);
            lcd.setCursor(0, 0);
            lcd.print(scale_value, 0);
            lcd.print("g   /   ");
            lcd.print(setup_weight, 0);
            lcd.print("g");
            lcd.setCursor(0,1);
            lcd.print(dataCount);
            lcd.print(" / ");
            lcd.print(dataIndex);
            // 특정 조건에 도달하면 다음 요소로 넘어가거나 초기화
            if (0.98 <= percent_weight && percent_weight <= 1.02) {
                // 배열의 다음 값을 처리하기 위해 setup_weight를 0으로
                data_success_count++;
                if(data_success_count > 10){
                  Serial.println("Data processed");
                  dataCount++;
                  setup_weight = 0;
                  ble_recipeStep = true;

                // 만약 모든 데이터를 다 처리했으면 초기화
                  if (dataCount >= dataIndex) {
                      dataIndex = 0; // 배열의 인덱스 초기화
                      setReset = false;
                      dataCount = 0; // 처리된 카운트 초기화
                      setup_weight = 0; // setup_weight 초기화
                      memset(dataArray, 0, sizeof(dataArray)); // 배열 초기화
                      lcd.clear();
                      Serial.println("All data processed, resetting values.");
                    }
                  data_success_count = 0;
                }
            } 
          }
    }
  

    // BLE가 연결되었으면 scale_value를 전송
    if (deviceConnected) {
        // 첫 번째 요소에 항상 battery_test를 저장
        txDataArray[0] = batteryPercentage;

        // 두 번째 요소부터 필요한 데이터를 추가
        txDataArray[1] = static_cast<int>((scale_value+0.5)); // 무게 값 예시
        txDataArray[2] = setup_weight; // setup_weight 예시
        if(success_ble == true){ //설정된 무게값 성공시 전달
          txDataArray[3] = 1;
          success_ble = false;
        }else{
          txDataArray[3] = 0;
        }
        if(ble_recipeStep == true){ //레시피 단계 다음 1 , 충족 아닐경우 0
          txDataArray[4] = 1;
          ble_recipeStep = false;
        }else{
          txDataArray[4] = 0;
        }if(loopOnOff == true){ //설정이 반복되면 1, 아니면 0
          txDataArray[5] = 1;
          Serial.println("Loop on");
        }else{
          txDataArray[5] = 0;
          Serial.println("Loop off");
        }
        
        // 배열을 문자열 형태로 변환하여 BLE로 전송
        String txDataString = String(txDataArray[0]);
        for (int i = 1; i < txDataArraySize; i++) {
            txDataString += "," + String(txDataArray[i]);
        }

        // BLE 전송
        pTxCharacteristic->setValue(txDataString.c_str()); // 배열을 문자열로 전송
        pTxCharacteristic->notify(); // notify를 통해 값 전송

        delay(500); // 전송 주기 조절
    }

    // 데이터가 있고, 배열에 값이 저장된 경우만 처리
    if (dataIndex > 0) {
        Serial.println("Parsed Values:");
        Serial.println("***************");
        for (int i = 0; i < dataIndex; i++) {
          Serial.println(dataArray[i]); // 각 값을 시리얼 모니터에 출력
        }
        Serial.println("***************");
    } else {
        Serial.println("No data received in the array.");
    }
    Serial.print("Data Index: ");
    Serial.println(dataIndex);

    // 연결상태가 변경되었는데 연결 해제된 경우
    if (!deviceConnected && oldDeviceConnected) {
        delay(500); // 연결이 끊어지고 잠시 대기
        // BLE가 연결되면 어드버타이징이 정지되기 때문에 연결이 해제되면 다시 어드버타이징을 시작시킴
        pServer->startAdvertising(); // 어드버타이징을 다시 시작시킴        
        Serial.println("Start advertising");
        // 이전 상태를 갱신함
        oldDeviceConnected = deviceConnected;
    }
    // 연결상태가 변경되었는데 연결된 경우
    if (deviceConnected && !oldDeviceConnected) {
        // 이전 상태를 갱신함
        oldDeviceConnected = deviceConnected;
    }

    delay(50); // 1초 대기
}

// 스위치 동작 확인&처리 함수
void SwitchCheck() {
    // 스위치 값 읽기
    int tare_btn = digitalRead(TARE_BTN_PIN);

    // 스위치 상태에 따라 각 동작 진행
    if (tare_btn == LOW || terra_button == 1 ||minus_scale_count == 50) { // 버튼이 눌리면 LOW
        minus_scale_count = 0;
        Serial.println("tare!!\n");
        lcd.init();
        lcd.setCursor(0, 0);
        lcd.print("Set Zero!!");
        scale.tare(20);
        terra_button = 0;
        delay(1000); // 버튼이 눌렸을 때의 딜레이
    }
    Serial.println(current_mode);
}

void OledDrawer(const uint8_t bitmap[]) {
    display.clearDisplay();
    display.drawBitmap(0, 0, bitmap, 128, 64, WHITE);
    display.display();
    delay(100);
}

void notSetOLED(float weightValue, float maxWeight){
  display.clearDisplay();
      // 무게 게이지바
    display.setTextSize(2);
    display.setTextColor(SSD1305_WHITE);
    display.setCursor(0, 0);
    display.print(" ");
    display.print(weightValue,0);
    display.print("g");
    // 게이지바 그리기 (0부터 최대 무게까지)
    display.drawRect(0, 30, 120, 20, SSD1305_WHITE);
    int barWidth = map(weightValue, 0, maxWeight, 0, 120);
    if (barWidth > 0) {
        display.fillRect(0, 30, barWidth, 20, SSD1305_WHITE);
    }

     for (int i = 500; i <= maxWeight; i += 500) {
        int tickPos = map(i, 0, maxWeight, 0, 120);
        display.drawLine(tickPos, 30, tickPos, 35, SSD1305_WHITE); // 게이지 바 내부에 눈금 표시
    }
    display.display();

}


void oledDisplay(float setup_weight, float scale_value) {
    // Set up the percentage calculation
    percent_weight = scale_value / setup_weight;
    if (percent_weight < 0.475) {
        display_mode = 0;
        if (current_mode != display_mode) {
            success_count = 0;
            display.clearDisplay();
            OledDrawer(mless_bitmap_data);
            current_mode = 0;
        }
    } else if (0.425 <= percent_weight && percent_weight < 0.95) {
        display_mode = 1;
        if (current_mode != display_mode) {
            success_count = 0;
            display.clearDisplay();
            OledDrawer(less_bitmap_data);
            current_mode = 1;
        }
    } else if (0.95 <= percent_weight && percent_weight <= 1.05) {
    display_mode = 2;
    if (current_mode != display_mode ) {
        display.clearDisplay();
        if(success_count < 5){
          OledDrawer(equal_bitmap_data);
        }
        current_mode = 2;  // Update current_mode to 2
    }
    success_count++;
    
  }
 else if (1.05 < percent_weight && percent_weight <= 1.525) {
        display_mode = 3;
        if (current_mode != display_mode) {
            success_count = 0;
            display.clearDisplay();
            OledDrawer(more_bitmap_data);
            current_mode = 3;
        }
    } else if (1.525 < percent_weight) {
        display_mode = 4;
        if (current_mode != display_mode) {
            success_count = 0;
            display.clearDisplay();
            OledDrawer(mmore_bitmap_data);
            current_mode = 4;
        }
    }
    if(3 <= success_count && success_count < 7){
      display.clearDisplay();
      OledDrawer(success_50_data);
    }else if(success_count >= 7){
      display.clearDisplay();
      OledDrawer(success_perfect_data);
    }
}

float getSmoothedBatteryVoltage() {
    float newVoltage = readBatteryVoltage();
    voltageSum -= batteryVoltages[voltageIndex];
    batteryVoltages[voltageIndex] = newVoltage;
    voltageSum += batteryVoltages[voltageIndex];
    
    voltageIndex = (voltageIndex + 1) % movingAverageWindowSize;
    return voltageSum / movingAverageWindowSize;
}


float readBatteryVoltage() {
    
    int adcValue = analogRead(batteryPin);
    float voltageAtPin = (adcValue / (float)adcResolution) * referenceVoltage;
    float batteryVoltage = voltageAtPin * ((resistor1 + resistor2) / resistor2);
    return batteryVoltage;
}

int calculateBatteryPercentage(float voltage) {
    const float maxVoltage = 4.2;
    const float minVoltage = 3.0;
    float percentage = ((voltage - minVoltage) / (maxVoltage - minVoltage)) * 100.0;
    if (percentage > 100.0) percentage = 100.0;
    if (percentage < 0.0) percentage = 0.0;
    return round(percentage);
}
