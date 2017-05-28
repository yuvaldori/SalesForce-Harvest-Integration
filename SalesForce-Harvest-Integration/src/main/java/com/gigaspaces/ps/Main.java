package com.gigaspaces.ps;

/**
 * Created by yuval on 5/24/17.
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
