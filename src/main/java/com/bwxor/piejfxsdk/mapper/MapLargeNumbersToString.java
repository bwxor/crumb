package com.bwxor.piejfxsdk.mapper;

public class MapLargeNumbersToString {
    public static String map(long input) {
        if (input < 1000){
            return String.valueOf(input);
        }
        if (input < 10_000) {
            long integerPart = input/1000;
            long decimalPart = input/100 % 10;
            return integerPart + "." + decimalPart + "K";
        }
        else if (input < 100_000){
            long integerPart = input/1000;
            long decimalPart = input/100 % 10;
            return integerPart + "." + decimalPart + "K";
        }
        else if (input < 1_000_000) {
            long integerPart = input/1000;
            long decimalPart = input/100 % 10;
            return integerPart + "." + decimalPart + "K";
        }
        else if (input < 1_000_000_000) {
            long integerPart = input/1_000_000;
            long decimalPart = input/100_000 % 10;
            return integerPart + "." + decimalPart + "M";
        } else {
            long integerPart = input / 1_000_000_000;
            long decimalPart = input/100_000_000 % 10;
            return integerPart + "." + decimalPart + "B";
        }
    }
}
