package com.gigaspaces.ps;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by yuval on 5/7/17.
 */
public class HVObj {
    private final static Logger LOGGER = Logger.getLogger(HVObj.class.getName());

    static String auth;
    static Map<String, String> teamsMembersMap;


    public static void getAuth() {
        Properties properties = SFObj.readProperties();
        auth = properties.getProperty("Authorization");
    }

    public static List<Project> updateServiceOrder(String dateFrom) throws HVException {

        Calendar startCal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println(sdf.format(startCal.getTime()) + " Starting reading from HV");
        LOGGER.info("Starting reading from HV");

        GetMethod get = new GetMethod("https://gigaspaces.harvestapp.com/projects");
        get.addRequestHeader("Accept", "application/json");
        get.addRequestHeader("Content-Type", "application/json");
        get.addRequestHeader("Authorization", "Basic " + auth);


        //Project project = null;
        HttpClient httpClient = new HttpClient();

        List<Project> projectsList = new ArrayList<Project>();

        try {
            httpClient.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (get.getStatusCode() != 200 && get.getStatusCode() != 201) {
                System.out.println("ERROR: " + response);
                LOGGER.severe(response);
            } else {
                try {
                    JSONArray jsonArray = new JSONArray(response);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonProject = jsonArray.optJSONObject(i);
                        JSONObject json = (JSONObject) jsonProject.get("project");
                        Integer id = (Integer) json.get("id");
                        Project project = getProject(id.toString(), dateFrom);
                        projectsList.add(project);
                    }
                    System.out.println("Finished reading from HV");

                } catch (JSONException e) {
                    e.printStackTrace();
                    String message = "Harvest failed building jsonObject";
                    LOGGER.log(Level.SEVERE, message, e);
                    throw new HVException(message, e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Harvest getEntries projects has been failed";
            LOGGER.log(Level.SEVERE, message, e);
            throw new HVException(message, e);
        }

        return projectsList;
    }

    public static Project getProject(String id, String dateFrom) throws HVException {

        GetMethod get = new GetMethod("https://gigaspaces.harvestapp.com/projects/" + id);
        get.addRequestHeader("Accept", "application/json");
        get.addRequestHeader("Content-Type", "application/json");
        get.addRequestHeader("Authorization", "Basic " + auth);

        Project project = null;
        HttpClient httpClient = new HttpClient();

        try {
            httpClient.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (get.getStatusCode() != 200 && get.getStatusCode() != 201) {
                System.out.println("ERROR: " + response);
                LOGGER.severe(response);
            } else {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject jsonProject = new JSONObject(jsonObject.getString("project"));
                    String HVCode = jsonProject.getString("code");
                    String budget = jsonProject.getString("budget");

                    if (HVCode.equals("null") || budget.equals("null")) {
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

                    // calculate the first day of this month
                    Calendar c1 = Calendar.getInstance();   // this takes current date
                    c1.set(Calendar.DAY_OF_MONTH, 1);
                    SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMdd");
                    String firstDayInThisMonth = sdf.format(c1.getTime());

                    // calculate the last Saturday date
                    Calendar c2 = Calendar.getInstance();
                    c2.add(Calendar.DAY_OF_WEEK, -(c2.get(Calendar.DAY_OF_WEEK)));
                    String lastSaturday = sdf.format(c2.getTime());

                    // get billable hours from day one till last Saturday
                    getHoures(project, dateFrom, lastSaturday, true);

                    // get billable hours from the first day of this month to last Saturday
                    getHoures(project, firstDayInThisMonth, lastSaturday, false);

                    System.out.println("HV info: " + project);
                    LOGGER.info("HV info: " + project);

                } catch (JSONException e) {
                    e.printStackTrace();
                    String message = "Harvest failed building jsonObject";
                    LOGGER.severe(message);
                    throw new HVException(message, e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Harvest getEntries projects has been failed";
            LOGGER.severe(message);
            throw new HVException(message, e);
        }

        if (project != null) {
            // TODO: 6/22/17  
            SFObj.updateServiceOrder(project);
        }

        return project;
    }

    public static void getHoures(Project project, String dateFrom, String dateTo, boolean fromDayOne) throws HVException {

        if (project == null || dateFrom == null || dateTo == null) {
            return;
        }

        // Get the entries details from Harvest
        GetMethod get = new GetMethod("https://gigaspaces.harvestapp.com/projects/" + project.getId() + "/entries?from=" + dateFrom + "&to=" + dateTo);
        get.addRequestHeader("Accept", "application/json");
        get.addRequestHeader("Content-Type", "application/json");
        get.addRequestHeader("Authorization", "Basic " + auth);

        HttpClient httpClient = new HttpClient();

        Map<String, String> tasksCategory = buildBillableNonBuillsbleTasksMap(project);
        Map<String, String> tmpProjectTeamMembersMap = new Hashtable<String, String>();
        Map<String, String> projectTeamMembersMap = new Hashtable<String, String>();

        try {
            httpClient.executeMethod(get);
            String response = get.getResponseBodyAsString();

            //System.out.println(response);

            if (get.getStatusCode() != 200 && get.getStatusCode() != 201) {
                System.out.println("ERROR: " + response);
                LOGGER.severe(response);
            } else {
                // write Harvest projects to console
//                System.out.println("SUCCESS: " + response);
//                LOGGER.info(response);

                // calculate the billable hours
                try {
                    JSONArray jsonArray = new JSONArray(response);

                    for (int index = 0; index < jsonArray.length(); index++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(index);
                        JSONObject dayEntryJson = new JSONObject(jsonObject.getString("day_entry"));
                        String hoursStr = dayEntryJson.getString("hours");
                        String taskId = dayEntryJson.getString("task_id");
                        String userId = dayEntryJson.getString("user_id");
                        String spentAt = dayEntryJson.getString("spent_at");
                        tmpProjectTeamMembersMap.put(userId, spentAt);

                        if (tasksCategory.get(taskId).equals("true") && fromDayOne) {
                            project.addBillableHours(new Float(hoursStr));
                        } else if (tasksCategory.get(taskId).equals("false") && fromDayOne) {
                            project.addNonBillableHours(new Float(hoursStr));
                        } else if (tasksCategory.get(taskId).equals("true") && !fromDayOne) {
                            project.addMonthlyBillableHours(new Float(hoursStr));
                        }
                    }

                    Iterator it = tmpProjectTeamMembersMap.entrySet().iterator();

                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry)it.next();
                        String name = teamsMembersMap.get(pair.getKey());
                        projectTeamMembersMap.put((String)pair.getValue(), name);
                    }

                    if (fromDayOne) {
                        project.setTeamsMembersMap(projectTeamMembersMap);
                    }

                    if (fromDayOne) {
                        project.setBudgetRemaining(((project.getBudget() - project.getTotalBillableHours()) / project.getBudget()) * 100);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    String message = "Harvest failed building jsonObject";
                    LOGGER.severe(message);
                    throw new HVException(message, e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Harvest getEntries projects has been failed";
            LOGGER.severe(message);
            throw new HVException(message, e);
        }
    }

    public static Map<String, String> buildBillableNonBuillsbleTasksMap(Project project) throws HVException {

        GetMethod get = new GetMethod("https://gigaspaces.harvestapp.com/projects/" + project.getId() + "/task_assignments");
        get.addRequestHeader("Accept", "application/json");
        get.addRequestHeader("Content-Type", "application/json");
        get.addRequestHeader("Authorization", "Basic " + auth);

        HttpClient httpClient = new HttpClient();

        Map<String, String> tasksCategory = new Hashtable<String, String>();

        try {
            httpClient.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (get.getStatusCode() != 200 && get.getStatusCode() != 201) {
                System.out.println("ERROR: " + response);
                LOGGER.severe(response);
            } else {
                try {
                    JSONArray jsonArray = new JSONArray(response);

                    for (int index = 0; index < jsonArray.length(); index++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(index);
                        JSONObject taskAssignment = new JSONObject(jsonObject.getString("task_assignment"));
                        String taskId = taskAssignment.getString("task_id");
                        String isBillable = taskAssignment.getString("billable");
                        tasksCategory.put(taskId, isBillable);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    String message = "Harvest failed building jsonObject";
                    LOGGER.severe(message);
                    throw new HVException(message, e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Harvest getEntries projects has been failed";
            LOGGER.severe(message);
            throw new HVException(message, e);
        }

        return tasksCategory;
    }

    public static void buildTeamMembersMap() throws HVException {
        GetMethod get = new GetMethod("https://gigaspaces.harvestapp.com/people");
        get.addRequestHeader("Accept", "application/json");
        get.addRequestHeader("Content-Type", "application/json");
        get.addRequestHeader("Authorization", "Basic " + auth);

        HttpClient httpClient = new HttpClient();
        teamsMembersMap = new Hashtable<String, String>();

        try {
            httpClient.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (get.getStatusCode() != 200 && get.getStatusCode() != 201) {
                System.out.println("ERROR: " + response);
                LOGGER.severe(response);
            } else {
                try {
                    JSONArray jsonArray = new JSONArray(response);

                    for (int index = 0; index < jsonArray.length(); index++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(index);
                        JSONObject taskAssignment = new JSONObject(jsonObject.getString("user"));
                        String userId = taskAssignment.getString("id");
                        String firstName = taskAssignment.getString("first_name");
                        teamsMembersMap.put(userId, firstName);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    String message = "Harvest failed building jsonObject";
                    LOGGER.severe(message);
                    throw new HVException(message, e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Harvest getEntries projects has been failed";
            LOGGER.severe(message);
            throw new HVException(message, e);
        }
    }

    private static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    private static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
}
