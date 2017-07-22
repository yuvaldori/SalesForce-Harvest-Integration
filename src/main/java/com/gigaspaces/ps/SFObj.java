package com.gigaspaces.ps;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by yuval on 5/21/17.
 */
public class SFObj {
    private final static Logger LOGGER = Logger.getLogger(SFObj.class.getName());

    private static final Logger logger = Logger.getLogger("com.gigaspaces.ps.SFObj");
    private static final String PROPERTIES_FILE = System.getProperty("propertiesFile", System.getProperty("user.home") + "/salesforce.properties");
    private static final String DEFAULT_PROPERTIES_FILE = "salesforce-defaults.properties";
    private static String salesforceInstance = null;
    private static String salesforceServicesDataVersion = null;

    public static Properties readProperties() {
        InputStream inputStream = null;
        String propertiesFile = null;
        //Try to load properties file from user.home/
        File propertiesFileUnderUserHome = new File(PROPERTIES_FILE);
        if (propertiesFileUnderUserHome.exists()) {
            try {
                inputStream = new FileInputStream(new File(PROPERTIES_FILE));
                propertiesFile = PROPERTIES_FILE;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                String message = "Failed to load properties from file [" + propertiesFile + "]";
                logger.log(Level.SEVERE, message, e);
            }
        }

        //If input stream of file PROPERTIES_FILE is null then try to reload as resource from classpath
        if (inputStream == null) {
            logger.warning("Properties file [" + PROPERTIES_FILE + "] was not found. " +
                    "Using the defaults instead");
            inputStream = SFObj.class.getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE);
            propertiesFile = DEFAULT_PROPERTIES_FILE;
        }

        if (inputStream == null) {
            String message = "Properties file [" + PROPERTIES_FILE + "] was not found, " +
                    "and default properties file [" + DEFAULT_PROPERTIES_FILE + "] was not found as well";
            logger.severe(message);
            throw new RuntimeException(message);
        }

        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Failed to load properties from file [" + propertiesFile + "]";
            logger.log(Level.SEVERE, message, e);

            throw new RuntimeException(message, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return properties;
    }

    private static String salesforceLogin() throws SFException {

        String accessToken = null;
        Properties properties = readProperties();
        salesforceInstance = properties.getProperty("instance");
        salesforceServicesDataVersion = properties.getProperty("services_data_version");
        PostMethod post = new PostMethod("https://na6.salesforce.com/services/oauth2/token");
        post.addParameter("grant_type", "password");
        post.addParameter("client_id", properties.getProperty("client_id")); // see here: https://feedback.uservoice.com/knowledgebase/articles/235661-get-your-key-and-secret-from-salesforce
        post.addParameter("client_secret", properties.getProperty("client_secret")); // see here: https://feedback.uservoice.com/knowledgebase/articles/235661-get-your-key-and-secret-from-salesforce
        post.addParameter("username", properties.getProperty("username"));
        post.addParameter("password", properties.getProperty("password"));
        post.addParameter("redirect_uri", "https://localhost:8443/RestTest/oauth/_callback");
        HttpClient httpClient = new HttpClient();
        String responseBody = null;
        try{
            httpClient.executeMethod(post);
            responseBody = post.getResponseBodyAsString();
        }catch(IOException e){
            e.printStackTrace();
            String message = "Salesforce login failed";
            logger.log(Level.SEVERE, message, e);
            throw new SFException(message, e);
        }
        try {
            JSONObject json = new JSONObject(responseBody);
            accessToken = json.getString("access_token");
        }catch(JSONException e){
            e.printStackTrace();
            String message = "Salesforce failed receiving Access Token";
            logger.log(Level.SEVERE, message, e);
            throw new SFException(message, e);
        }

        return accessToken;
    }

    private static JSONObject salesforceSendGetRequest(String url, String accessToken) throws SFException {

        JSONObject response = null;
        GetMethod get = new GetMethod(url);
        get.setRequestHeader("Authorization", "OAuth " + accessToken);

        try {
            HttpClient httpclient = new HttpClient();
            httpclient.executeMethod(get);

            if (get.getStatusCode() == HttpStatus.SC_OK) {
                try {
                    System.out.println(get.getResponseBodyAsStream());
                    LOGGER.info(get.getResponseBodyAsString());
                    response = new JSONObject(new JSONTokener(new InputStreamReader(get.getResponseBodyAsStream())));
                    String message = "Response for " + url + " is " + response.toString(2);
                    logger.log(Level.INFO, message);
                }catch(JSONException e){
                    e.printStackTrace();
                    String message = "Salesforce failed receiving Access Token";
                    logger.log(Level.SEVERE, message, e);
                    throw new SFException(message, e);
                }
            }
        }catch(IOException e){
            e.printStackTrace();
            String message = "Salesforce failed receiving Access Token";
            logger.log(Level.SEVERE, message, e);
            throw new SFException(message, e);
        } finally {
            get.releaseConnection();
        }

        return response;
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

    private static String salesforceGetServiceOrderByName(JSONObject response, String filedName) throws SFException {
        try {
            JSONArray jsonArray = (JSONArray) response.getJSONArray("records");
            JSONObject jsonObject = (JSONObject) jsonArray.get(0);
            Map map = toMap(jsonObject);

            return (String)map.get(filedName);
        } catch (JSONException e) {
            e.printStackTrace();
            String message = "Salesforce failed account Order id by name " + response;
            logger.log(Level.SEVERE, message, e);
            throw new SFException(message, e);
        }
    }

    private static PostMethod updateServiceOrder(String url, JSONObject jsonProject, String accessToken) throws IOException {
        PostMethod patch = new PostMethod(url)
        {
            @Override public String getName() { return "PATCH"; }
        };

        patch.setRequestHeader("Authorization", "OAuth " + accessToken);
        patch.setRequestEntity(new StringRequestEntity(jsonProject.toString(), "application/json", null));
        HttpClient httpclient = new HttpClient();
        httpclient.executeMethod(patch);

        return patch;
    }

    public static PostMethod createServiceOrder(String url, JSONObject jsonProject, String accessToken) throws IOException {
        PostMethod post = new PostMethod(url);
        post.setRequestHeader("Authorization", "OAuth " + accessToken);
        post.setRequestEntity(new StringRequestEntity(jsonProject.toString(), "application/json", null));
        HttpClient httpclient = new HttpClient();
        httpclient.executeMethod(post);

        return post;
    }

    private static StringBuffer printMap(Map<String, String> mapToPrint) {
        StringBuffer sb = new StringBuffer();
        SortedSet<String> keys = new TreeSet<String>(Collections.reverseOrder());
        keys.addAll(mapToPrint.keySet());

        for (String key : keys) {
            String value = mapToPrint.get(key);
            sb.append(key + "  -  " + value + "\n");
        }

        return sb;
    }

    public static Boolean updateServiceOrder(Project project) {
        String accessToken = null;
        JSONObject jsonProject = new JSONObject();
        String serviceOrderId = null;

        try {
            // Get Token to Salesforce
            accessToken = salesforceLogin();

            // Build the JSON record
            jsonProject.put("Budget__c", project.getBudget());
            jsonProject.put("Total_Billable_Hours__c", project.getTotalBillableHours());
            jsonProject.put("Billable_Hours_this_month__c", project.getBillableHoursThisMonth());
            jsonProject.put("Non_Billable_Hours__c", project.getNonBillableHours());
            jsonProject.put("Budget_Remaining__c", project.getBudgetRemaining());
            jsonProject.put("Last_Report_Date__c", printMap(project.getTeamsMembersMap()));

            JSONObject responseAccount = salesforceSendGetRequest(salesforceInstance + salesforceServicesDataVersion + "query?q=SELECT+Account__c+from+Service_Order__c+where+Name=+'" + project.getName() + "'", accessToken);
            JSONObject responseId = salesforceSendGetRequest(salesforceInstance + salesforceServicesDataVersion + "query?q=SELECT+Record_ID__c+from+Service_Order__c+where+Name=+'" + project.getName() + "'", accessToken);
            String accountId = salesforceGetServiceOrderByName(responseAccount, "Account__c");
            serviceOrderId = salesforceGetServiceOrderByName(responseId, "Record_ID__c");
            jsonProject.put("Account__c", accountId);

        } catch (JSONException e) {
            e.printStackTrace();
            String message = "Salesforce failed building Service Order JSON. " + project.toString() ;
            logger.log(Level.SEVERE, message, e);
        } catch (SFException e) {
            e.printStackTrace();
            String message = "Salesforce Error. " + project.toString();
            logger.log(Level.SEVERE, message, e);
        }

        PostMethod patch = null;

        try {
            // create a new Service order under the account
            //PostMethod post = updateServiceOrder(salesforceInstance + salesforceServicesDataVersion + "sobjects/Service_Order__c/", jsonProject, accessToken);

            // update existing Service Order
            patch = updateServiceOrder(salesforceInstance + salesforceServicesDataVersion + "sobjects/Service_Order__c/" + serviceOrderId, jsonProject, accessToken);

            try {
                if (patch.getStatusCode() != 204) {
                    System.out.println("ERROR: " + patch.getStatusLine() + " " + project.toString());
                    LOGGER.severe("ERROR: " + patch.getStatusLine() + " " + project.toString());

                    return false;
                } else {
                    System.out.println("Service Order " + serviceOrderId + " has been updated");
                    LOGGER.info("Service Order " + serviceOrderId + " has been updated");
                    System.out.println("Link :" + salesforceInstance + "/" + serviceOrderId);
                    LOGGER.info("Link :" + salesforceInstance + "/" + serviceOrderId);
                }
            } finally {
                patch.releaseConnection();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Salesforce Error. " + project.toString();
            logger.log(Level.SEVERE, message, e);
        }

        return true;
    }
}
