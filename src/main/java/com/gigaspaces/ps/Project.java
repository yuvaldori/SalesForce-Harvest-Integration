package com.gigaspaces.ps;

import java.util.Hashtable;
import java.util.Map;

/**
 * Created by yuval on 5/22/17.
 */
public class Project {

    private int id; // HV: equals to id field
    private String name; // SF: equals to Service Order Number field in Service Order object. HV: part of code field
    private float budget; // HV: equals to project budget field
    private float billableHoursThisMonth;
    private float totalBillableHours; // HV: equals to project Billable Hours field
    private float nonBillableHours; // HV: equals to project Non-Billable Hours field
    private float budgetRemaining; // HV: equals to project budget Remaining field

    private Map<String, String> teamsMembersMap;

    public Project(int id, String name, float budget) {
        this.id = id;
        this.name = name;
        this.budget = budget;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setNumber(String number) {
        this.name = number;
    }

    public float getNonBillableHours() {
        return nonBillableHours;
    }

    public void addNonBillableHours(Float nonBillableHours) {
        this.nonBillableHours +=nonBillableHours;
    }

    public float getBudget() {
        return budget;
    }

    public void setBudget(float budget) {
        this.budget = budget;
    }

    public float getTotalBillableHours() {
        return totalBillableHours;
    }

    public void addBillableHours(float billableHours) {
        this.totalBillableHours +=billableHours;
    }

    public void addMonthlyBillableHours(float billableHours) {
        this.billableHoursThisMonth +=billableHours;
    }

    public float getBudgetRemaining() {
        return budgetRemaining;
    }

    public void setBudgetRemaining(float budgetRemaining) {
        this.budgetRemaining = budgetRemaining;
    }
    public float getBillableHoursThisMonth() {return billableHoursThisMonth; }

    public void setBillableHoursThisMonth(float billableHoursThisMonth) {
        this.billableHoursThisMonth = billableHoursThisMonth;
    }

    public Map<String, String> getTeamsMembersMap() {
        return teamsMembersMap;
    }

    public void setTeamsMembersMap(Map<String, String> teamsMembersMap) {
        this.teamsMembersMap = teamsMembersMap;
    }

    public String toString(){
        return "id = " + id + "\n" +
                "name = " + name + "\n" +
                "budget = " + budget + "\n" +
                "totalBillableHours = " + totalBillableHours + "\n" +
                "billableHoursThisMonth = " + billableHoursThisMonth + "\n" +
                "nonBillableHours = " + nonBillableHours + "\n" +
                "budgetRemaining = " + budgetRemaining + "\n" +
                "TeamMembersReport = " + teamsMembersMap;
    }
}
