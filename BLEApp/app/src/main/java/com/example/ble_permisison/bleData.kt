package com.example.ble_permisison

object bleData{
    var batteryData: Int = 0 //배터리 값(배열 0번)
    var scaleData: Int = 0 //저울 값(배열 1번)
    var setScaleData: Int = 0 //설정 된 무게 값(배열 2번)
    var settingSuccessData: Int =0 //설정 된 무게 성공 유무(배열 3번) 1 : 성공 , 0 : pass
    var recipeNextData: Int = 0//레시피 다음 단계 유무 (배열 4번) 1 : 성공 , 0 : pass
    var setloopData: Int = 0//설정 무게값 반복 유무(배열 5번) 1 : 반복 , 0: 단일
}