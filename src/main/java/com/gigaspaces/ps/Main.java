package com.gigaspaces.ps;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Updated by yuvald on 5/28/17.
 */
public class Main {
    public static void main(String[] args) {
        try {
            HVObj.getAuth();
            HVObj.buildTeamMembersMap();
            HVObj.updateServiceOrder("20140101");
            //HVObj.getProject("14560484", "20140101");
        }catch(HVException e){
            e.printStackTrace();
        }
    }
}
