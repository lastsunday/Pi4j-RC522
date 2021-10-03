package org.g51x.hardware.pi;

public interface RC522 {

    int MAX_LEN = 16;
    int PCD_IDLE = 0x00;
    int PCD_AUTHENT = 0x0E;
    int PCD_RECEIVE = 0x08;
    int PCD_TRANSMIT = 0x04;
    int PCD_TRANSCEIVE = 0x0C;
    int PCD_RESETPHASE = 0x0F;
    int PCD_CALCCRC = 0x03;

    int PICC_REQIDL = 0x26;
    int PICC_REQALL = 0x52;
    int PICC_ANTICOLL = 0x93;
    int PICC_SELECT_TAG = 0x93;
    int PICC_AUTHENT_1A = 0x60;
    int PICC_AUTHENT_1B = 0x61;
    int PICC_READ = 0x30;
    int PICC_WRITE = 0xA0;
    int PICC_DECREMENT = 0xC0;
    int PICC_INCREMENT = 0xC1;
    int PICC_RESTORE = 0xC2;
    int PICC_TRANSFER = 0xB0;
    int PICC_HALT = 0x50;

    int MI_OK = 0;
    int MI_NOTAGERR = 1;
    int MI_ERR = 2;

    byte RESERVED_00 = 0x00;
    byte REG_COMMAND = 0x01;
    byte REG_COMM_IEN = 0x02;
    byte REG_DIVL_EN = 0x03;
    byte REG_COMM_IRQ = 0x04;
    byte REG_DIV_IRQ = 0x05;
    byte REG_ERROR = 0x06;
    byte REG_STATUS_1 = 0x07;
    byte REG_STATUS_2 = 0x08;
    byte REG_FIFO_DATA = 0x09;
    byte REG_FIFO_LEVEL = 0x0A;
    byte REG_WATER_LEVEL = 0x0B;
    byte REG_CONTROL = 0x0C;
    byte REG_BIT_FRAMING = 0x0D;
    byte REG_COLL = 0x0E;
    byte RESERVED_01 = 0x0F;

    byte RESERVED_10 = 0x10;
    byte REG_MODE = 0x11;
    byte REG_TX_MODE = 0x12;
    byte REG_RX_MODE = 0x13;
    byte REG_TX_CONTROL = 0x14;
    byte REG_TX_AUTO = 0x15;
    byte REG_TX_SEL = 0x16;
    byte REG_RX_SEL = 0x17;
    byte REG_RX_THRESHOLD = 0x18;
    byte REG_DEMO = 0x19;
    byte RESERVED_11 = 0x1A;
    byte RESERVED_12 = 0x1B;
    byte REG_MIFARE = 0x1C;
    byte RESERVED_13 = 0x1D;
    byte RESERVED_14 = 0x1E;
    byte REG_SERIAL_SPEED = 0x1F;

    byte RESERVED_20 = 0x20;
    byte REG_M_CRC_RESULT = 0x21;
    byte REG_L_CRC_RESULT = 0x22;
    byte RESERVED_21 = 0x23;
    byte REG_MOD_WIDTH = 0x24;
    byte RESERVED_22 = 0x25;
    byte REG_RF_CFG = 0x26;
    byte REG_GS_N = 0x27;
    byte REG_CW_GS_P = 0x28;
    byte REG_MOD_GS_P = 0x29;
    byte REG_T_MODE = 0x2A;
    byte REG_T_PRESCALER = 0x2B;
    byte REG_H_T_RELOAD = 0x2C;
    byte REG_L_T_RELOAD = 0x2D;
    byte REG_H_T_COUNTER_VALUE = 0x2E;
    byte REG_L_T_COUNTER_VALUE = 0x2F;

    byte RESERVED_30 = 0x30;
    byte REG_TEST_SELL = 0x31;
    byte REG_TEST_SEL2 = 0x32;
    byte REG_TEST_PIN_EN = 0x33;
    byte REG_TEST_PIN_VALUE = 0x34;
    byte REG_TEST_BUS = 0x35;
    byte REG_AUTO_TEST = 0x36;
    byte REG_VERSION = 0x37;
    byte REG_ANALOG_TEST = 0x38;
    byte REG_TEST_DAC1 = 0x39;
    byte REG_TEST_DAC2 = 0x3A;
    byte REG_TEST_ADC = 0x3B;
    byte RESERVED_31 = 0x3C;
    byte RESERVED_32 = 0x3D;
    byte RESERVED_33 = 0x3E;
    byte RESERVED_34 = 0x3F;

    void reset();

    void antennaOn();

    void antennaOff();

    int[] request(byte reqMode);

    byte[] anticoll();

    int selectTag(byte[] uid);

    //Authenticates to use specified block address. Tag must be selected using select_tag(uid) before auth.
    //auth_mode-RFID.auth_a or RFID.auth_b
    //block_address- used to authenticate
    //key-list or tuple(数组) with six bytes key
    //uid-list or tuple with four bytes tag ID
    void auth(byte authMode, byte blockAddress, byte[] key, byte[] uid);

    void auth(byte auth_mode, byte sector, byte block, byte[] key, byte[] uid);

    //导出1K字节,64个扇区
    byte[] dumpClassic1K(byte[] key, byte[] uid);

    //Reads data from block. You should be authenticated before calling read.
    //Returns tuple of (result state, read data).
    //block_address
    //back_data-data to be read,16 bytes
    void read(byte blockAddress, byte[] backData);

    void read(byte sector, byte block, byte[] backData);

    //uid-5 bytes
    void selectMirareOne(byte[] uid);

    //Ends operations with Crypto1 usage.
    void stopCrypto();

    //Writes data to block. You should be authenticated before calling write.
    //Returns error state.
    //data-16 bytes
    void write(byte blockAddress, byte[] data);

    void write(byte sector, byte block, byte[] data);
}
