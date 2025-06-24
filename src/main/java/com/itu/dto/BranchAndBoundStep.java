package com.itu.dto;

import java.util.Map;

public class BranchAndBoundStep {
    private int nodeId;
    private String nodeType; // "BRANCHING", "INTEGER_SOLUTION", "PRUNED", "INFEASIBLE"
    private double objectiveValue;
    private String branchVariable;
    private double branchValue;
    private double leftChildBound;
    private double rightChildBound;
    private Map<String, Double> solution;
    private String message;

    // Constructors
    public BranchAndBoundStep() {}

    // Getters
    public int getNodeId() {
        return nodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public double getObjectiveValue() {
        return objectiveValue;
    }

    public String getBranchVariable() {
        return branchVariable;
    }

    public double getBranchValue() {
        return branchValue;
    }

    public double getLeftChildBound() {
        return leftChildBound;
    }

    public double getRightChildBound() {
        return rightChildBound;
    }

    public Map<String, Double> getSolution() {
        return solution;
    }

    public String getMessage() {
        return message;
    }

    // Setters
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public void setObjectiveValue(double objectiveValue) {
        this.objectiveValue = objectiveValue;
    }

    public void setBranchVariable(String branchVariable) {
        this.branchVariable = branchVariable;
    }

    public void setBranchValue(double branchValue) {
        this.branchValue = branchValue;
    }

    public void setLeftChildBound(double leftChildBound) {
        this.leftChildBound = leftChildBound;
    }

    public void setRightChildBound(double rightChildBound) {
        this.rightChildBound = rightChildBound;
    }

    public void setSolution(Map<String, Double> solution) {
        this.solution = solution;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}