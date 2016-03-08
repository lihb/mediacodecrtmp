package com.example.mediacodecrtmp;

/**
 * Created by Administrator on 2016/3/7.
 */
public class H264SPSPaser {
    private static final String TAG = "H264SPSPaser";
    private static int startBit = 0;
    /*
     * 从数据流data中第StartBit位开始读，读bitCnt位，以无符号整形返回
     */
    public static short u(int bitCnt, byte[] data,int StartBit){
        short ret = 0;
        int start = StartBit;
        for(int i = 0;i < bitCnt; i++){
            ret<<=1;
            if ((data[start / 8] & (0x80 >> (start % 8))) != 0) {
                ret += 1;
            }
            start++;
        }
        return ret;
    }
    /*
     * 无符号指数哥伦布编码
     * leadingZeroBits = ?1;
     * for( b = 0; !b; leadingZeroBits++ )
     *    b = read_bits( 1 )
     * 变量codeNum 按照如下方式赋值：
     * codeNum = 2^leadingZeroBits ? 1 + read_bits( leadingZeroBits )
     * 这里read_bits( leadingZeroBits )的返回值使用高位在先的二进制无符号整数表示。
     */
    public static short ue(byte[] data,int StartBit){
        short ret = 0;
        int leadingZeroBits = -1;
        int tempStartBit = (StartBit == -1)?startBit:StartBit;//如果传入-1，那么就用上次记录的静态变量
        for( int b = 0; b != 1; leadingZeroBits++ ){//读到第一个不为0的数，计算前面0的个数
            b = u(1, data,tempStartBit++);
        }
        System.out.println("ue leadingZeroBits = " + leadingZeroBits + ",Math.pow(2, leadingZeroBits) = " + Math.pow(2, leadingZeroBits) + ",tempStartBit = " + tempStartBit);
        ret = (short) (Math.pow(2, leadingZeroBits) - 1 + u(leadingZeroBits, data,tempStartBit));
        startBit = tempStartBit + leadingZeroBits;
        System.out.println("ue startBit = " + startBit);
        return ret;
    }
    /*
     * 有符号指数哥伦布编码
     * 9.1.1 有符号指数哥伦布编码的映射过程
     *按照9.1节规定，本过程的输入是codeNum。
     *本过程的输出是se(v)的值。
     *表9-3中给出了分配给codeNum的语法元素值的规则，语法元素值按照绝对值的升序排列，负值按照其绝对
     *值参与排列，但列在绝对值相等的正值之后。
     *表 9-3－有符号指数哥伦布编码语法元素se(v)值与codeNum的对应
     *codeNum 语法元素值
     *  0       0
     *  1       1
     *  2       ?1
     *  3       2
     *  4       ?2
     *  5       3
     *  6       ?3
     *  k       (?1)^(k+1) Ceil( k÷2 )
     */
    public static int se(byte[] data,int StartBit){
        int ret = 0;
        short codeNum = ue(data,StartBit);
        ret = (int) (Math.pow(-1, codeNum + 1)*Math.ceil(codeNum/2));
        return ret;
    }

    public static void main(String[] args) {
        byte[] buf = {70, 0, 54, 14, 25, 00};
//        printBuf(buf);
        byte[] header_sps = {0x67, 0x42, (byte)0xc0, 0x16, (byte)0x92, 0x54, 0x05, 0x01, (byte)0xed, 0x08, 0x00, 0x00, 0x03, 0x00, 0x08, 0x00, 0x00, 0x03,
                0x00,  (byte)0xf3, 0x00, 0x00, 0x04,  (byte)0xe7,  (byte)0xc0, 0x00, 0x4e, 0x5e, 0x5e,  (byte)0xf7, 0x00,  (byte)0xf1, 0x62,  (byte)0xea};
        byte[] header_pps = {0x68,  (byte)0xce, 0x32, 0x48};

//        int width = (H264SPSPaser.ue(header_sps,34) + 1)*16;
//        int height = (H264SPSPaser.ue(header_sps,-1) + 1)*16;

//        System.out.println("width = " + width);
//        System.out.println("height = " + height);

        System.out.println("test = " + H264SPSPaser.u(3, new byte[]{1,1,1},0));
    }

}
