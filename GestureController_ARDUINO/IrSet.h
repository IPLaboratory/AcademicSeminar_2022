// set buff length for ir receiver
#if RAMEND <= 0x4FF || (defined(RAMSIZE) && RAMSIZE < 0x4FF)
#define RAW_BUFFER_LENGTH  180  // 750 (600 if we have only 2k RAM) is the value for air condition remotes. Default is 112 if DECODE_MAGIQUEST is enabled, otherwise 100.
#elif RAMEND <= 0x8FF || (defined(RAMSIZE) && RAMSIZE < 0x8FF)
#define RAW_BUFFER_LENGTH  600  // 750 (600 if we have only 2k RAM) is the value for air condition remotes. Default is 112 if DECODE_MAGIQUEST is enabled, otherwise 100.
#else
#define RAW_BUFFER_LENGTH  750  // 750 (600 if we have only 2k RAM) is the value for air condition remotes. Default is 112 if DECODE_MAGIQUEST is enabled, otherwise 100.
#endif
#define MARK_EXCESS_MICROS 20 // recommended for the cheap VS1838 modules
#define RECORD_GAP_MICROS 12000 // Activate it for some LG air conditioner protocols

// usable pin -> D1, D2, D5, D6, D7
#define IR_RECEIVE_PIN D5
#define IR_SEND_PIN D6
#define R_LED D1
#define G_LED D2
#define B_LED D7
