package com.gigaspaces.ps;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by yuval on 5/7/17.
 */
public class HVObj {

    private static final Logger logger = Logger.getLogger("com.gigaspaces.ps.HVObj");

    static Properties properties;


    public static void updateServiceOrder(String dateFrom, String dateTo) throws HVException {

        properties = SFObj.readProperties();

        GetMethod get = new GetMethod("https://gigaspaces.harvestapp.com/projects");
        get.addRequestHeader("Accept", "application/json");
        get.addRequestHeader("Content-Type", "application/json");
        get.addRequestHeader("Authorization", "Basic " + properties.getProperty("Authorization"));

        //Project project = null;
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
                    JSONArray jsonArray = new JSONArray(response);

                    for (int i=0; i<jsonArray.length(); i++){
                        JSONObject jsonProject = jsonArray.optJSONObject(i);
                        JSONObject json = (JSONObject)jsonProject.get("project");
                        Integer id = (Integer) json.get("id");

                        Project project = getProject(id.toString(), dateFrom, dateTo);
                    }
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
    }

    public static Project getProject(String id, String dateFrom, String dateTo) throws HVException {

        properties = SFObj.readProperties();

        GetMethod get = new GetMethod("https://gigaspaces.harvestapp.com/projects/" + id);
        get.addRequestHeader("Accept", "application/json");
        get.addRequestHeader("Content-Type", "application/json");
        get.addRequestHeader("Authorization", "Basic " + properties.getProperty("Authorization"));

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

                    if (HVCode.equals("null") || budget.equals("null")){
                        return null;
                    }

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

        if (project != null) {
            SFObj.updateServiceOrder(project);
        }

        return project;
    }

    public static void getHoures(Project project, String dateFrom, String dateTo) throws HVException {

        if (project == null || dateFrom == null || dateTo == null) {
            return;
        }

        // Get the entries details from Harvest
        GetMethod get = new GetMethod("https://gigaspaces.harvestapp.com/projects/"+project.getId()+"/entries?from="+dateFrom+"&to="+dateTo);
        get.addRequestHeader("Accept", "application/json");
        get.addRequestHeader("Content-Type", "application/json");
        get.addRequestHeader("Authorization", "Basic " + properties.getProperty("Authorization"));

        HttpClient httpClient = new HttpClient();

        Map<String, String> tasksCategory = buildBillableNonBuillsbleTasksMap(project);

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

                        if (tasksCategory.get(taskId).equals("true")){
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

    public static Map<String, String> buildBillableNonBuillsbleTasksMap(Project project) throws HVException {

        GetMethod get = new GetMethod("https://gigaspaces.harvestapp.com/projects/"+ project.getId() + "/task_assignments");
        get.addRequestHeader("Accept", "application/json");
        get.addRequestHeader("Content-Type", "application/json");
        get.addRequestHeader("Authorization", "Basic " + properties.getProperty("Authorization"));

        HttpClient httpClient = new HttpClient();

        Map<String, String> tasksCategory = new Hashtable<String, String>();

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
                        JSONObject taskAssignment = new JSONObject(jsonObject.getString("task_assignment"));
                        String taskId = taskAssignment.getString("task_id");
                        String isBillable = taskAssignment.getString("billable");
                        tasksCategory.put(taskId, isBillable);
                    }
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

        return tasksCategory;
    }

    private static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    private static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
}
