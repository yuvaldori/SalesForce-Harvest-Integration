package com.gigaspaces.ps;

/**
 * Updated by yuvald on 5/28/17.
 */
public class Main {
    public static void main(String[] args) {
        try {
            //HVObj.updateServiceOrder("20140101", "20170630");
            HVObj.getProject("10513278", "20140101", "20170630");
        }catch(HVException e){
            e.printStackTrace();
        }
    }
}
