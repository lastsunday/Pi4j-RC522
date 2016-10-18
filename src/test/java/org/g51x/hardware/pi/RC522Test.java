package org.g51x.hardware.pi;

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
    }
}
