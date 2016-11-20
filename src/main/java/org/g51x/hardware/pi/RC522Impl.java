package org.g51x.hardware.pi;

import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.Spi;

public class RC522Impl implements RC522 {

    public static int SPI_CHANNEL_0 = 0;
//    public static int SPI_CHANNEL_1 = 0;
    private int pinReset;
    private int speed;

    public RC522Impl() {
        this(22, 500000);
    }

    public RC522Impl(int pinReset, int speed) {
        this.pinReset = pinReset;
        this.speed = speed;
        if (speed < 500000 || speed > 32000000) {
            throw new IllegalArgumentException("Speed out of range:" + speed);
        } else {
            init();
        }
    }

    private void init() {
        Gpio.wiringPiSetup();           //Enable wiringPi pin schema
        int resultSetup = Spi.wiringPiSPISetup(SPI_CHANNEL_0, speed);
        if (resultSetup == -1) {
            throw new ErrorMessageException("Failed to set up  SPI communication:" + resultSetup);
        } else {
            Gpio.pinMode(pinReset, Gpio.OUTPUT);
            Gpio.digitalWrite(pinReset, Gpio.HIGH);
            reset();
            write(REG_T_MODE, (byte) 0x8D);
            write(REG_T_PRESCALER, (byte) 0x3E);
            write(REG_L_T_RELOAD, (byte) 30);
            write(REG_H_T_RELOAD, (byte) 0);
            write(REG_TX_AUTO, (byte) 0x40);
            write(REG_MODE, (byte) 0x3D);
            antennaOn();
        }
    }

    @Override
    public void reset() {
        write(REG_COMMAND, (byte) PCD_RESETPHASE);
    }

    private void write(byte address, byte value) {
        byte data[] = new byte[2];
        data[0] = (byte) ((address << 1) & 0x7E);
        data[1] = value;
        int result = Spi.wiringPiSPIDataRW(SPI_CHANNEL_0, data);
        if (result == -1) {
            throw new ErrorMessageException("Device write  error,address=" + address + ",value=" + value);
        } else {
            //success,not things to do
        }
    }

    private byte read(byte address) {
        byte data[] = new byte[2];
        data[0] = (byte) (((address << 1) & 0x7E) | 0x80);
        data[1] = 0;
        Spi.wiringPiSPIDataRW(SPI_CHANNEL_0, data);
        return data[1];
    }

    //Reads data from block. You should be authenticated before calling read.
    //Returns tuple of (result state, read data).
    //block_address
    //back_data-data to be read,16 bytes
    public void read(byte blockAddress, byte[] backData) {
        byte data[] = new byte[4];
        int backBits[] = new int[1];
        int backLen[] = new int[1];
        data[0] = PICC_READ;
        data[1] = blockAddress;
        calculateCRC(data);
        write((byte) PCD_TRANSCEIVE, data, data.length, backData, backBits, backLen);
        if (backLen[0] != 16) {
            throw new ErrorMessageException();
        }
    }

    public void read(byte sector, byte block, byte[] backData) {
        read(sector2BlockAddress(sector, block), backData);
    }

    private void setBitmask(byte address, byte mask) {
        write(address, (byte) (read(address) | mask));
    }

    private void clearBitmask(byte address, byte mask) {
        write(address, (byte) (read(address) & (~mask)));
    }

    @Override
    public void antennaOn() {
        byte value = read(REG_TX_CONTROL);
        //   if((value & 0x03) != 0x03)
        setBitmask(REG_TX_CONTROL, (byte) 0x03);
    }

    @Override
    public void antennaOff() {
        clearBitmask(REG_TX_CONTROL, (byte) 0x03);
    }

    private void write(byte command, byte[] data, int dataLen, byte[] backData, int[] backBits, int[] backLen) {
        byte irq = 0, irq_wait = 0, lastBits = 0;
        int n = 0, i = 0;
        backLen[0] = 0;
        if (command == PCD_AUTHENT) {
            irq = 0x12;
            irq_wait = 0x10;
        } else if (command == PCD_TRANSCEIVE) {
            irq = 0x77;
            irq_wait = 0x30;
        }
        write(REG_COMM_IEN, (byte) (irq | 0x80));
        clearBitmask(REG_COMM_IRQ, (byte) 0x80);
        setBitmask(REG_FIFO_LEVEL, (byte) 0x80);

        write(REG_COMMAND, (byte) PCD_IDLE);

        for (i = 0; i < dataLen; i++) {
            write(REG_FIFO_DATA, data[i]);
        }

        write(REG_COMMAND, command);
        if (command == PCD_TRANSCEIVE) {
            setBitmask(REG_BIT_FRAMING, (byte) 0x80);
        }

        i = 2000;
        while (true) {
            n = read(REG_COMM_IRQ);
            i--;
            if ((i == 0) || (n & 0x01) > 0 || (n & irq_wait) > 0) {
                break;
            }
        }
        clearBitmask(REG_BIT_FRAMING, (byte) 0x80);

        if (i != 0) {
            if ((read(REG_ERROR) & 0x1B) == 0x00) {
                if ((n & irq & 0x01) > 0) {
                    throw new ErrorMessageException("MI_NOTAGERR");
                }
                if (command == PCD_TRANSCEIVE) {
                    n = read(REG_FIFO_LEVEL);
                    lastBits = (byte) (read(REG_CONTROL) & 0x07);
                    if (lastBits != 0) {
                        backBits[0] = (n - 1) * 8 + lastBits;
                    } else {
                        backBits[0] = n * 8;
                    }

                    if (n == 0) {
                        n = 1;
                    }
                    if (n > MAX_LEN) {
                        n = MAX_LEN;
                    }
                    backLen[0] = n;
                    for (i = 0; i < n; i++) {
                        backData[i] = read(REG_FIFO_DATA);
                    }
                }
            } else {
                throw new ErrorMessageException("MI_ERR");
            }
        }
    }

    @Override
    public int[] request(byte reqMode) {
        byte tagType[] = new byte[1];
        int[] backBits = new int[1];
        byte dataBack[] = new byte[16];
        int backLen[] = new int[1];
        tagType[0] = reqMode;
        backBits[0] = 0;
        write(REG_BIT_FRAMING, (byte) 0x07);
        write((byte) PCD_TRANSCEIVE, tagType, 1, dataBack, backBits, backLen);
        if (backBits[0] != 0x10) {
            throw new ErrorMessageException("Request error");
        }
        return backBits;
    }

    @Override
    public byte[] anticoll() {
        byte tagid[] = new byte[5];
        byte[] serialNumber = new byte[2];   //2字节命令
        int serialNumberCheck = 0;
        int backLen[] = new int[1];
        int backBits[] = new int[1];
        write(REG_BIT_FRAMING, (byte) 0x00);
        serialNumber[0] = (byte) PICC_ANTICOLL;
        serialNumber[1] = 0x20;
        write((byte) PCD_TRANSCEIVE, serialNumber, 2, tagid, backBits, backLen);
        if (backLen[0] == 5) {
            for (int i = 0; i < 4; i++) {
                serialNumberCheck ^= tagid[i];
            }
            if (serialNumberCheck != tagid[4]) {
                throw new ErrorMessageException("check error");
            }
        } else {
            //nothings to do
        }
        return tagid;
    }

    //Writes data to block. You should be authenticated before calling write.
    //Returns error state.
    //data-16 bytes
    public void write(byte blockAddress, byte[] data) {
        byte buff[] = new byte[4];
        byte buffWrite[] = new byte[data.length + 2];
        byte backData[] = new byte[MAX_LEN];
        int backBits[] = new int[1];
        int backLen[] = new int[1];
        buff[0] = (byte) PICC_WRITE;
        buff[1] = blockAddress;
        calculateCRC(buff);
        write((byte) PCD_TRANSCEIVE, buff, buff.length, backData, backBits, backLen);
        if (backBits[0] != 4 || (backData[0] & 0x0F) != 0x0A) {
            throw new ErrorMessageException("MI_ERR");
        }
        for (int i = 0; i < data.length; i++) {
            buffWrite[i] = data[i];
        }
        calculateCRC(buffWrite);
        write((byte) PCD_TRANSCEIVE, buffWrite, buffWrite.length, backData, backBits, backLen);
        if (backBits[0] != 4 || (backData[0] & 0x0F) != 0x0A) {
            throw new ErrorMessageException("Error while writing");
        }
    }

    public void write(byte sector, byte block, byte[] data) {
        write(sector2BlockAddress(sector, block), data);
    }

    //uid-5字节数组,存放序列号
    //返值是大小
    @Override
    public int selectTag(byte[] uid) {
        byte data[] = new byte[9];
        byte backData[] = new byte[MAX_LEN];
        int backBits[] = new int[1];
        int backLen[] = new int[1];
        int i, j;
        data[0] = (byte) PICC_SELECT_TAG;
        data[1] = 0x70;
        for (i = 0, j = 2; i < 5; i++, j++) {
            data[j] = uid[i];
        }
        calculateCRC(data);
        write((byte) PCD_TRANSCEIVE, data, 9, backData, backBits, backLen);
        if (backBits[0] == 0x18) {
            return backData[0];
        } else {
            return 0;
        }
    }

    //uid-5 bytes
    public void selectMirareOne(byte[] uid) {
        byte tagid[] = new byte[5];
        try {
            request((byte) PICC_REQIDL);
        } catch (ErrorMessageException e1) {
            try {
                anticoll();
            } catch (ErrorMessageException e2) {
                selectTag(tagid);
                System.arraycopy(tagid, 0, uid, 0, 5);
            }
        }
    }

    //Ends operations with Crypto1 usage.
    public void stopCrypto() {
        clearBitmask(REG_STATUS_2, (byte) 0x08);
    }

    //导出1K字节,64个扇区
    public byte[] dumpClassic1K(byte[] key, byte[] uid) {
        byte[] data = new byte[1024];
        byte[] buff = new byte[16];
        for (int i = 0; i < 64; i++) {
            RC522Impl.this.auth((byte) PICC_AUTHENT_1A, (byte) i, key, uid);
            read((byte) i, buff);
            System.arraycopy(buff, 0, data, i * 64, 16);
        }
        return data;
    }

    //Authenticates to use specified block address. Tag must be selected using select_tag(uid) before auth.
    //auth_mode-RFID.auth_a or RFID.auth_b
    //block_address- used to authenticate
    //key-list or tuple(数组) with six bytes key
    //uid-list or tuple with four bytes tag ID
    public void auth(byte authMode, byte blockAddress, byte[] key, byte[] uid) {
        byte data[] = new byte[12];
        byte backData[] = new byte[MAX_LEN];
        int backBits[] = new int[1];
        int backLen[] = new int[1];
        int i, j;
        data[0] = authMode;
        data[1] = blockAddress;
        for (i = 0, j = 2; i < 6; i++, j++) {
            data[j] = key[i];
        }
        for (i = 0, j = 8; i < 4; i++, j++) {
            data[j] = uid[i];
        }
        write((byte) PCD_AUTHENT, data, 12, backData, backBits, backLen);
        if ((read(REG_STATUS_2) & 0x08) == 0) {
            throw new ErrorMessageException("MI_ERR");
        }
    }

    public void auth(byte auth_mode, byte sector, byte block, byte[] key, byte[] uid) {
        auth(auth_mode, sector2BlockAddress(sector, block), key, uid);
    }

    //CRC值放在data[]最后两字节
    private void calculateCRC(byte[] data) {
        clearBitmask(REG_DIV_IRQ, (byte) 0x04);
        setBitmask(REG_FIFO_LEVEL, (byte) 0x80);

        for (int i = 0; i < data.length - 2; i++) {
            write(REG_FIFO_DATA, data[i]);
        }
        write(REG_COMMAND, (byte) PCD_CALCCRC);
        int i = 255;
        int n;
        while (true) {
            n = read(REG_DIV_IRQ);
            i--;
            if ((i == 0) || ((n & 0x04) > 0)) {
                break;
            }
        }
        data[data.length - 2] = read(REG_L_CRC_RESULT);
        data[data.length - 1] = read(REG_M_CRC_RESULT);
    }

    //Convert sector  to blockaddress
    //sector-0~15
    //block-0~3
    //return blockaddress
    private byte sector2BlockAddress(byte sector, byte block) {
        if (sector < 0 || sector > 15 || block < 0 || block > 3) {
            return -1;
        }
        return (byte) (sector * 4 + block);
    }
}
