package com.itu.dto;

import java.util.List;

public class SimplexStep {
    private List<String> basisVariables;
    private List<List<String>> tableauRows;
    private String objectiveValue;

    //?------All getters
    public List<String> getBasisVariables() {
        return basisVariables;
    }
    public List<List<String>> getTableauRows() {
        return tableauRows;
    }
    public String getObjectiveValue() {
        return objectiveValue;
    }

    //?------All setters
    public void setBasisVariables(List<String> basisVariables) {
        this.basisVariables = basisVariables;
    }
    public void setTableauRows(List<List<String>> tableauRows) {
        this.tableauRows = tableauRows;
    }
    public void setObjectiveValue(String objectiveValue) {
        this.objectiveValue = objectiveValue;
    }
}