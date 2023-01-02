#include <Arduino.h>

// for ESP8266 and socketIO
#include <ESP8266WiFi.h>
#include <ESP8266WiFiMulti.h>
#include <ArduinoJson.h>
#include <WebSocketsClient.h>
#include <SocketIOclient.h>
#include <Hash.h>

// for IR
#include "PinDefinitionsAndMore.h" // Define macros for input and output pin etc.
#include <IRremote.hpp>
#include "IrSet.h"

// for socketIO
ESP8266WiFiMulti WiFiMulti;
SocketIOclient socketIO;

// json doc size
#define DOC_SIZE 2048


void socketIOEvent(socketIOmessageType_t type, uint8_t * payload, size_t length) {
    switch (type) {
        case sIOtype_DISCONNECT:
            Serial.printf("[IOc] Disconnected!\n\n");
            ledHigh(R_LED);
            break;
        case sIOtype_CONNECT:
            Serial.printf("[IOc] Connected to url: %s\n\n", payload);
            // 소켓서버 조인
            socketIO.send(sIOtype_CONNECT, "/");  // 기본 네임스페이스 접속            
            break;

        case sIOtype_EVENT: // 아두이노가 메세지를 수신하는 지점
            {

                // payload를 String 형식으로 변경
                String msg = (char*)payload;

                // string을 json으로 변경
                DynamicJsonDocument doc(DOC_SIZE);
                DeserializationError error = deserializeJson(doc, msg);
                if (error) { // json array 해석 실패 예외처리
                    Serial.printf("deserializeJson() failed: %s", error.c_str());
                    break;
                }
                JsonArray array = doc.as<JsonArray>();
                JsonVariant eventName = array[0];
                JsonVariant data = array[1];

                Serial.printf("[IOc] get event: %s\n", eventName.as<const char*>());
                Serial.println(F("======================================"));
                
                if (eventName.as<String>() == "request rawData") {  // 서버가 rawData를 요청하는 이벤트
                    int did = data["did"];
                    int gid = data["gid"];
                    const char* message = data["message"].as<const char*>();

                    /*
                        ir 수신기를 on해서 입력값 받기
                    */
                    ledHigh(B_LED);
                    IrReceiver.start(30000);
                    if (receiveRawData()) { // 신호 수신 성공 -> 서버로 전송
                        uint16_t dsize = (IrReceiver.decodedIRData.rawDataPtr->rawlen) - 1;
                        uint16_t rawData[dsize] = {};
                        cvtUint32to16(rawData);

                        int rawDataLen = sizeof(rawData) / sizeof(uint16_t);
                        Serial.printf("[INFO_IRSIGNAL] data size:%d, type size:%d, length:%d\n", sizeof(rawData), sizeof(uint16_t), rawDataLen);

                        //입력값 전송
                        String strRawData = rawData2Str(rawData, rawDataLen);
                        String keyValue[] = {"did", String(did), "gid", String(gid), "length", String(rawDataLen), "rawData", strRawData};
                        String json = makeJson("get rawData", keyValue, 4);
                        socketIO.sendEVENT(json);

                        ledHigh(G_LED);
                    } else {  // 신호 수신 실패(타임아웃)
                        Serial.println(F("[FAIL_IR_RECV] timeout"));
                        // 오류 led조작
                        ledLow();
                        ledNotice(G_LED, R_LED);                        
                    }
                    IrReceiver.stop();
                    IrReceiver.resume();


                } else if (eventName.as<String>() == "send rawData") {  // 서버로부터 rawData를 받아 ir송신기로 전송

                     String sRawData = data["rawData"].as<String>();    // JsonVariant as String
                     const int strLen = sRawData.length();  // 문자열의 길이(문자개수)
                     const int rawDataLen = data["length"].as<const int>();  // rawData의 개수(배열 인덱스 수)

                     char cRawData[strLen] = {0};   // strtok을 사용하기 위해 String을 char배열에 옮김
                     sRawData.toCharArray(cRawData,strLen+1);                     
                     
                     uint16_t iRawData[rawDataLen]; // atoi를 사용하여 uint16_t rawData를 저장할 배열
                     
                     int ipos = 0;
                     char *tok = strtok(cRawData, ","); // 문자열을 ','기준으로 자른다
                     while(tok){
                        if (ipos < rawDataLen){
                            iRawData[ipos++] = (uint16_t)(atoi(tok));   //잘린 문자열을 정수로 바꾸어 배열에 저장
                        }
                        tok = strtok(NULL, ",");    //다음 문장을 자른다.
                     }
                     IrSender.sendRaw(iRawData, sizeof(iRawData)/sizeof(iRawData[0]), 38);
                     Serial.printf("[INFO] length of data for send: %d\n", rawDataLen);
                     Serial.println(F("[SUCCESS_IR_SEND] sended ir signal"));
                     
                     // 정상 led조작
                     ledNotice(G_LED, B_LED);

                } else if (eventName.as<String>() == "request name") {  // 서버에 클라이언트 등록
                    String data[] = {"device", "ir"};
                    String json = makeJson("client registration", data, 1);
                    socketIO.sendEVENT(json);
                    
                    // 접속 완료 led변경
                    ledHigh(G_LED);
                }

                Serial.println(F("======================================"));
                Serial.println();
                break;
            }
        case sIOtype_ACK:
            Serial.printf("[IOc] get ack: %u\n\n", length);
            hexdump(payload, length);
            break;
        case sIOtype_ERROR:
            Serial.printf("[IOc] get error: %u\n\n", length);
            hexdump(payload, length);
            break;
        case sIOtype_BINARY_EVENT:
            Serial.printf("[IOc] get binary: %u\n\n", length);
            hexdump(payload, length);
            break;
        case sIOtype_BINARY_ACK:
            Serial.printf("[IOc] get binary ack: %u\n\n", length);
            hexdump(payload, length);
            break;
    }
}


void setup() {

    Serial.begin(115200);
#if defined(__AVR_ATmega32U4__) || defined(SERIAL_PORT_USBVIRTUAL) || defined(SERIAL_USB) /*stm32duino*/|| defined(USBCON) /*STM32_stm32*/|| defined(SERIALUSB_PID) || defined(ARDUINO_attiny3217)
    delay(4000); // To be able to connect Serial monitor after reset or power up and before first print out. Do not wait for an attached Serial Monitor!
#endif

    pinMode(LED_BUILTIN, OUTPUT);
    pinMode(R_LED, OUTPUT);
    pinMode(G_LED, OUTPUT);
    pinMode(B_LED, OUTPUT);

    digitalWrite(B_LED, HIGH);  //부팅 led

    /*
        IR sender setup
    */
    IrSender.begin(); // Start with IR_SEND_PIN as send pin and if NO_LED_FEEDBACK_CODE is NOT defined, enable feedback LED at default feedback LED pin
    Serial.print(F("Ready to send IR signals at pin "));
    Serial.println(IR_SEND_PIN);


    /*
        IR receiver setup
    */
    IrReceiver.begin(IR_RECEIVE_PIN, DISABLE_LED_FEEDBACK);
    Serial.print(F("Ready to receive IR signals at pin "));
    Serial.println(IR_RECEIVE_PIN);

    /*
        socketIO setup
    */
    pinMode(D1, OUTPUT);
    Serial.setDebugOutput(true);

    setupWait(); // wait 4sec

    if (WiFi.getMode() & WIFI_AP) {
        WiFi.softAPdisconnect(true);
    }

//    WiFiMulti.addAP("IPL", "ipl612112");
    WiFiMulti.addAP("cslab-pre", "cslabm606");

    //WiFi.disconnect();
    while (WiFiMulti.run() != WL_CONNECTED) {
        delay(100);
    }

    String ip = WiFi.localIP().toString();
    Serial.printf("[SETUP] WiFi Connected %s\n\n", ip.c_str());

//    socketIO.begin("220.69.208.237", 8080, "/socket.io/?EIO=4");
    socketIO.begin("192.168.0.7", 8080, "/socket.io/?EIO=4");

    socketIO.onEvent(socketIOEvent);

}

void loop() {
    socketIO.loop();
}
