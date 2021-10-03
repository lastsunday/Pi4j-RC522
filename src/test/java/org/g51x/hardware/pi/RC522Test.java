package org.g51x.hardware.pi;

import java.util.Arrays;

public class RC522Test {

    public static void main(String[] args) {
        RC522 rc522 = new RC522Impl();
        int[] serialNumber = rc522.request((byte) RC522.PICC_REQIDL);
        System.out.println("Detecte card:" + serialNumber[0]);
        //防冲撞
        byte[] tagid = rc522.anticoll();
        //显示序列号
        String strUID = Convert.bytesToHex(tagid);
        System.out.println("Card Read UID:" + strUID.substring(0, 2) + ","
                + strUID.substring(2, 4) + ","
                + strUID.substring(4, 6) + ","
                + strUID.substring(6, 8));
        //选择卡片
        int size = rc522.selectTag(tagid);
        System.out.println("Size=" + size);

        //byte sector=15,block=3;
        byte sector = 15, block = 3;
        byte[] defaultkey = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        //byte[] defaultkey=new byte[]{(byte)0x03,(byte)0x03,(byte)0x00,(byte)0x01,(byte)0x02,(byte)0x03};
        // byte[] defaultkey=new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};
        rc522.auth((byte) RC522Impl.PICC_AUTHENT_1A, sector, block, defaultkey, tagid);

        //卡各扇区初始控制字FF078069，15扇区改为08778F69,A密钥改为330123
        byte data[] = new byte[16];
        // byte controlbytes[]=new byte[]{(byte)0x08,(byte)0x77,(byte)0x8f,(byte) 0x69};
        byte controlbytes[] = new byte[]{(byte) 0xFF, (byte) 0x07, (byte) 0x80, (byte) 0x69};
        System.arraycopy(controlbytes, 0, data, 6, 4);
//        byte[] keyA=new byte[]{(byte)0x03,(byte)0x03,(byte)0x00,(byte)0x01,(byte)0x02,(byte)0x03};
        byte[] keyA = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        byte[] keyB = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        System.arraycopy(keyA, 0, data, 0, 6);
        System.arraycopy(keyB, 0, data, 10, 6);
        try {
            byte[] returnValue = new byte[16];
            rc522.read(sector, block, returnValue);
            System.out.println(Arrays.toString(returnValue));
            rc522.write(sector, block, data);
            System.out.println("Write data finished");
        } catch (ErrorMessageException e) {
            System.out.println("write data error:" + e.getMessage());
        }
    }
}
