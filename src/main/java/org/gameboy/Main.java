package org.gameboy;

public class Main {
    public static void main(String[] args) {
        System.out.printf("%d\n", ((byte) 0xFF >>> (byte) 1));
        System.out.println((short) (Short.MAX_VALUE + 1));
        System.out.println(Short.MIN_VALUE);
    }
}