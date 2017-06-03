package com.gigaspaces.ps;

/**
 * Updated by yuvald on 5/28/17.
 */
public class Main {
    public static void main(String[] args) {
        try {
            HVObj.getProjects("20160501", "20170630");
            //Project project = HVObj.getProject("12831491","20160501", "20170630");
            //SFObj.updateServiceOrder(project);
        }catch(HVException e){
            e.printStackTrace();
        }
    }
}
