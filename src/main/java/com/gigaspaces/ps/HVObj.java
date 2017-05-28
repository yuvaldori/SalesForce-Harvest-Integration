package com.gigaspaces.ps;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by yuval on 5/7/17.
 */
public class HVObj {

    private static final Logger logger = Logger.getLogger("com.gigaspaces.ps.HVObj");


    public static Project getProject(String id, String dateFrom, String dateTo) throws HVException {

        GetMethod get = new GetMethod("https://gigaspaces.harvestapp.com/projects/" + id);
        get.addRequestHeader("Accept", "application/json");
        get.addRequestHeader("Content-Type", "application/json");
        get.addRequestHeader("Authorization", "Basic eXV2YWxkQGdpZ2FzcGFjZXMuY29tOll1RDAyMDYy");
        Project project = null;
        HttpClient httpClient = new HttpClient();

        try {
            httpClient.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (get.getStatusCode() != 200 && get.getStatusCode() != 201) {
                System.out.println("ERROR: " + response);
            } else {
                // write Harvest projects to console
                System.out.println("SUCCESS: " + response);

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject jsonProject = new JSONObject(jsonObject.getString("project"));
                    String HVCode = jsonProject.getString("code");
                    String budget = jsonProject.getString("budget");

                    StringTokenizer st = new StringTokenizer(HVCode);
                    while (st.hasMoreElements()) {
                        String part = (String) st.nextElement();
                        if (part.startsWith("PRJ")) {
                            project = new Project(Integer.parseInt(id), part, Float.parseFloat(budget));

                            break;
                        }
                    }

                    getHoures(project, dateFrom, dateTo);
                } catch (JSONException e) {
                    e.printStackTrace();
                    String message = "Harvest failed building jsonObject";
                    logger.log(Level.SEVERE, message, e);
                    throw new HVException(message, e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Harvest getEntries projects has been failed";
            logger.log(Level.SEVERE, message, e);
            throw new HVException(message, e);
        }

        return project;
    }

    private static boolean getIsBillable(String taskId) throws HVException {
        // Get the task details from Harvest
        GetMethod get = new GetMethod("https://gigaspaces.harvestapp.com/tasks/" + taskId);
        get.addRequestHeader("Accept", "application/json");
        get.addRequestHeader("Content-Type", "application/json");
        get.addRequestHeader("Authorization", "Basic eXV2YWxkQGdpZ2FzcGFjZXMuY29tOll1RDAyMDYy");

        HttpClient httpClient = new HttpClient();

        boolean isBillable = false;

        try {
            httpClient.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (get.getStatusCode() != 200 && get.getStatusCode() != 201) {
                System.out.println("ERROR: " + response);
            } else {
                // write Harvest projects to console
                System.out.println("SUCCESS: " + response);

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject jsonTask = new JSONObject(jsonObject.getString("task"));
                    String isBillableStr = jsonTask.getString("billable_by_default");
                    isBillable = Boolean.valueOf(isBillableStr);

                } catch (JSONException e) {
                    e.printStackTrace();
                    String message = "Harvest failed building jsonObject";
                    logger.log(Level.SEVERE, message, e);
                    throw new HVException(message, e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Harvest getEntries projects has been failed";
            logger.log(Level.SEVERE, message, e);
            throw new HVException(message, e);
        }

        return isBillable;
    }

    public static void getHoures(Project project, String dateFrom, String dateTo) throws HVException {

        if (project == null || dateFrom == null || dateTo == null) {
            return;
        }

        // Get the entries details from Harvest
        GetMethod get = new GetMethod("https://gigaspaces.harvestapp.com/projects/"+project.getId()+"/entries?from="+dateFrom+"&to="+dateTo);
        get.addRequestHeader("Accept", "application/json");
        get.addRequestHeader("Content-Type", "application/json");
        get.addRequestHeader("Authorization", "Basic eXV2YWxkQGdpZ2FzcGFjZXMuY29tOll1RDAyMDYy");

        HttpClient httpClient = new HttpClient();

        try{
            httpClient.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (get.getStatusCode() != 200 && get.getStatusCode() != 201) {
                System.out.println("ERROR: " + response);
            }else{
                // write Harvest projects to console
                System.out.println("SUCCESS: " + response);

                // calculate the billable hours
                try {
                    JSONArray jsonArray = new JSONArray(response);

                    for (int index=0; index<jsonArray.length(); index++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(index);
                        JSONObject dayEntryJson = new JSONObject(jsonObject.getString("day_entry"));
                        String hoursStr = dayEntryJson.getString("hours");
                        String taskId = dayEntryJson.getString("task_id");

                        if (getIsBillable(taskId) == true){
                            project.addBillableHours(new Float(hoursStr));
                        }else{
                            project.addNonBillableHours(new Float(hoursStr));
                        }
                    }

                    project.setBudgetRemaining(((project.getBudget() - project.getBillableHours()) / project.getBudget()) * 100);

                    System.out.println(project.toString());

                }catch (JSONException e){
                    e.printStackTrace();
                    String message = "Harvest failed building jsonObject";
                    logger.log(Level.SEVERE, message, e);
                    throw new HVException(message, e);
                }
            }
        }catch(IOException e){
            e.printStackTrace();
            String message = "Harvest getEntries projects has been failed";
            logger.log(Level.SEVERE, message, e);
            throw new HVException(message, e);
        }
    }
}
