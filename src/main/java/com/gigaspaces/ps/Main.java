package com.gigaspaces.ps;

/**
 * Updated by yuvald on 5/28/17.
 */
public class Main {
    public static void main(String[] args) {
        try {
            Project project = HVObj.getProject("13993362","20170501", "20170630");
            SFObj.updateServiceOrder(project);
        }catch(HVException e){
            e.printStackTrace();
        }
    }
}
