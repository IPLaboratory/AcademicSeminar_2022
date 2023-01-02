boolean resume() {
    IrReceiver.resume();
    return false;
}

// 32비트 rawData를 16비트로 변환하는 함수
void cvtUint32to16(uint16_t *rawData) {
    // Store data, skip leading space
#if RAW_BUFFER_LENGTH <= 254        // saves around 75 bytes program memory and speeds up ISR
    uint_fast8_t i;
#else
    unsigned int  i;
#endif

    for (i = 1; i < IrReceiver.decodedIRData.rawDataPtr->rawlen; i++) {
        uint32_t tDuration = IrReceiver.decodedIRData.rawDataPtr->rawbuf[i] * MICROS_PER_TICK;
        if (i & 1) {
            tDuration -= MARK_EXCESS_MICROS;
        } else {
            tDuration += MARK_EXCESS_MICROS;
        }

        // uint16_t type의 배열에 rawData를 저장
        *rawData = (uint16_t)tDuration;
        rawData++;
    }
}

// ir 신호 수신하는 함수
boolean receiveRawData() {
    unsigned long startMillis = millis();
    while ((!IrReceiver.decode()) || (IrReceiver.decodedIRData.rawDataPtr->rawlen < 60 || resume())) {
        yield();

        if (millis() - startMillis >= 30000) // 30초동안 수신받지 못하면 종료
            return false;

        if (IrReceiver.decode()) {  // 적외선이 감지되면
            if (IrReceiver.decodedIRData.rawDataPtr->rawlen < 60) { // 손상된 데이터 수신
                Serial.print(F("[FAIL_IR_RECV] corrupted ir signal -> length: "));
                Serial.println(IrReceiver.decodedIRData.rawDataPtr->rawlen);
                IrReceiver.resume();
                continue;
            } else {  // 올바른 데이터 수신
                // Check if the buffer overflowed
                if (IrReceiver.decodedIRData.flags & IRDATA_FLAGS_WAS_OVERFLOW) { //오버플로우 예외처리
                    Serial.println(F("Overflow detected"));
                    Serial.println(F("Try to increase the \"RAW_BUFFER_LENGTH\" value of " STR(RAW_BUFFER_LENGTH) " in " __FILE__));
                    return false;
                } else {
                    Serial.println(F("[SUCCESS_IR_RECV] received ir signal"));
                    return true;
                }
            }
        }
    }
}

// 16비트 rawData를 문자열 형태로 변경 [0,1,2] => "{0,1,2}"
String rawData2Str(uint16_t rawData[], int cnt) {
    int i;
    String strRawData = "";

    strRawData += String(rawData[0]);
    for (i = 1; i < cnt; i++) {
        strRawData += ",";
        strRawData += String(rawData[i]);
    }
    return strRawData;
}


// led깜빡임으로 신호 전송 확인
void ledNotice(int mainLed, int noticeLed) {
    digitalWrite(mainLed, LOW);
    digitalWrite(noticeLed, HIGH);
    delay(50);
    digitalWrite(noticeLed, LOW); 
    digitalWrite(mainLed, HIGH);
}
void ledLow() {
    digitalWrite(R_LED, LOW);
    digitalWrite(G_LED, LOW);
    digitalWrite(B_LED, LOW);
}
void ledHigh(int led) {
    ledLow();
    digitalWrite(led, HIGH);
}
