package com.itu.dto;

// This class is a DTO (Data Transfer Object)
public class SimplexRequest {
    private double[] objective; // the stuff to maximize
    private double[][] constraints; // the matrix
    private double[] rhs;
    private String[] constraintTypes; // the signs
    private boolean integerProgramming; // NEW: flag to indicate if this is an integer programming problem

    //?-------All getters
    public double[] getObjective() {
        return objective;
    }
    public double[][] getConstraints() {
        return constraints;
    }
    public double[] getRhs() {
        return rhs;
    }
    public String[] getConstraintTypes() {
        return constraintTypes;
    }
    public boolean isIntegerProgramming() {
        return integerProgramming;
    }

    //?---------USE THESE ONLY FOR THE SIMPLE version
    public double[][] getConstraintsWithObjective() {
        double[][] constraints = this.getConstraints();
        double[] objective = this.getObjective();
        double[][] constraintsWithObjective = new double[constraints.length + 1][constraints[0].length];
        System.arraycopy(constraints, 0, constraintsWithObjective, 0, constraints.length);
        for (int i = 0; i < constraints[0].length; i++) {
            constraintsWithObjective[constraints.length][i] = objective[i];
        }
        return constraintsWithObjective;
    }

    public double[] getRhsWithObjectiveDummy() {
        double[] rhs = this.getRhs();
        double[] rhsWithObjectiveDummy = new double[rhs.length + 1];
        System.arraycopy(rhs, 0, rhsWithObjectiveDummy, 0, rhs.length);
        rhsWithObjectiveDummy[rhs.length] = 0;
        return rhsWithObjectiveDummy;
    }

    public String[] getConstraintTypesWithObjectiveDummy() {
        String[] constraintTypes = this.getConstraintTypes();
        String[] constraintTypesWithObjectiveDummy = new String[constraintTypes.length + 1];
        System.arraycopy(constraintTypes, 0, constraintTypesWithObjectiveDummy, 0, constraintTypes.length);
        constraintTypesWithObjectiveDummy[constraintTypes.length] = "==";
        return constraintTypesWithObjectiveDummy;
    }

    //?---------USE THESE ONLY FOR THE TWO PHASE version
    public double[][] getOriginalConstraints() {
        return constraints;
    }

    public String[] getOriginalConstraintTypes() {
        return constraintTypes;
    }

    public double[] getOriginalRhs() {
        return rhs;
    }

    //?-------All setters
    public void setObjective(double[] objective) {
        this.objective = objective;
    }
    public void setConstraints(double[][] constraints) {
        this.constraints = constraints;
    }
    public void setRhs(double[] rhs) {
        this.rhs = rhs;
    }
    public void setConstraintTypes(String[] constraintTypes) {
        this.constraintTypes = constraintTypes;
    }
    public void setIntegerProgramming(boolean integerProgramming) {
        this.integerProgramming = integerProgramming;
    }
}