#include <Arduino.h>
#include <ArduinoJson.h>
#include <Hash.h>

// json maker
String makeJson(String event, String keyValue[], int valueCount) {
    DynamicJsonDocument doc(DOC_SIZE);
    JsonArray array = doc.to<JsonArray>();
    array.add(event);
    JsonObject param = array.createNestedObject();

    //key:value 매핑해서 json obj에 추가
    int i;
    for (i = 0; i < valueCount * 2; i = i + 2) {
        String key = keyValue[i];
        String value = keyValue[i + 1];
        param[key] = value;
    }

    // 만들어 놓은 구조를 바탕으로 JSON Seriallize(직렬화)
    String output;
    serializeJson(doc, output);

    //시리얼모니터에 테스트 메세지 출력(전송한 메세지 출력)
    Serial.print(F("[JSON_CREATED] event: "));
    Serial.println(event);
//    Serial.println(output);

    return output;
}


// 부트 시 대기함수
void setupWait() {
    for (uint8_t t = 1; t < 5; t++) {
        Serial.printf("[SETUP] BOOT WAIT %d/4...\n", t);
        Serial.flush();
        delay(1000);
    }
}
