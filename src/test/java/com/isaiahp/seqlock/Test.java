package com.isaiahp.seqlock;

public class Test {

     public static void main(String[] args) {
         byte[] result = new byte[16];
         long d = -1;
         long d1 = -1;
         for (int i = 0; i < 8; i ++) {
             int shift = i * 8;
             byte b = (byte) ((d >> shift) & 0xFF );
             result[i] = b;
         }
         for (int i = 8; i < 16; i ++) {
             long shift = (i - 8) * 8;
             byte b = (byte) ((d1 >> shift) & 0xFF);
             result[i] = b;
         }

         for (int i = 0; i < result.length; i++) {
             System.out.print(result[i]);
             System.out.print(";");
         }
         System.out.println();
         long res1 = 0;
         for (int i = 0; i < 8; i++) {

             int shift = i * 8;
             res1 |= (result[i]  << shift);
         }
         long res2 = 0;
         for (int i = 8; i < 16; i++) {
             int shift = (i - 8) * 8;
             res2 |= (result[i]  << shift);
         }

         System.out.println(res1);
         System.out.println(res2);
    }
}
