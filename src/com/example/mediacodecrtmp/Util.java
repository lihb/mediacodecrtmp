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
        byte[] buf = {70, 0, 54, 14, 25, 00};
        printBuf(buf);
    }

}
