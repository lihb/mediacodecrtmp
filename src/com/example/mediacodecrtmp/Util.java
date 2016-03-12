package com.example.mediacodecrtmp;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Administrator on 2016/3/3.
 */
public class Util {

    public static void printBufToFile(byte[] buf, String fileName) {
        File file = new File(fileName);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(buf, 0, buf.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void printBuf(byte[] buf) {
        for (int i = 0; i < buf.length; i++) {
            System.out.print(buf[i]);
        }
    }


    public static void main(String[] args) {
        byte s = -80;
        byte t = 94;
        System.out.println(String.format("s = %x,    t = %x, %x, %x, %x,%x, %x, %x, %x,%x",
                (byte)-2, (byte)-1,(byte)-48,(byte)-79,(byte)-126,(byte)-92, (byte)-56,(byte)-109,(byte)108,(byte)-98));
        System.out.println(String.format("%d", (byte)0xaf));
//        byte[] fisrtData = new byte[2];
//        int profile = 2;  //AAC LC
//        //39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
//        int freqIdx = 4;  //44.1KHz
//        int chanCfg = 2;  //CPE
//        fisrtData[0] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
//        System.out.println(fisrtData[0]);
//        System.out.println(String.format("%x", fisrtData[0]));
    }

}
