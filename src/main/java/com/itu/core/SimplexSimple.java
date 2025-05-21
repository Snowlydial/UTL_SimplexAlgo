package com.itu.core;

import java.util.ArrayList;
import java.util.List;

import com.itu.dto.SimplexStep;

public class SimplexSimple {
    private int numberOfSlackvars = 0;
    private int numOriginalVars;
    protected List<String> variableNames = new ArrayList<>();

    private List<SimplexStep> iterationSteps = new ArrayList<>();

    //?--------Constructors
    public SimplexSimple() {}

    //*---Getters
    public int getSlackVar() {
        return numberOfSlackvars;
    }

    public List<String> getVariableNames() {
        return variableNames;
    }

    public List<SimplexStep> getIterationSteps() {
        return iterationSteps;
    }

    //*---Setters
    public void setNumberOfSlackVar(int nblack) {
        this.numberOfSlackvars = nblack;
    }

    //?--------MAIN METHODS
    //*-------Canonical to standard:
    protected Fraction[][] standardize(double[][] canonicalForm, String[] constraints) {
        if (canonicalForm.length != constraints.length) {
            throw new IllegalArgumentException("Mismatched array lengths");
        }

        int numRows = canonicalForm.length;
        numOriginalVars = canonicalForm[0].length;
        int numSlackVars = 0;

        for (String constraint : constraints) {
            if (!constraint.equals("=")) numSlackVars++;
        }
        setNumberOfSlackVar(numSlackVars);

        variableNames.clear();
        for(int i=0; i<numOriginalVars; i++) {
            variableNames.add("x"+(i+1));
        }
        for(int i=0; i<numSlackVars; i++) {
            variableNames.add("s"+(i+1));
        }

        Fraction[][] standardLHS = new Fraction[numRows][numOriginalVars + numSlackVars];
        int slackVarIndex = 0;

        for (int i = 0; i < numRows; i++) {
            // Initialize all elements first
            for (int j = 0; j < standardLHS[i].length; j++) {
                standardLHS[i][j] = new Fraction(0);
            }
            
            // Copy original variables
            for (int j = 0; j < numOriginalVars; j++) {
                standardLHS[i][j] = new Fraction((int) canonicalForm[i][j], 1);
            }

            // Handle slack variables
            String constraint = constraints[i];
            if (constraint.equals("<=")) {
                standardLHS[i][numOriginalVars + slackVarIndex] = new Fraction(1);
                slackVarIndex++;
            } else if (constraint.equals(">=")) {
                standardLHS[i][numOriginalVars + slackVarIndex] = new Fraction(-1);
                slackVarIndex++;
            }
        }
        return standardLHS;
    }

    public Fraction[][] standardize_then_fuse_rightSide(double[][] canonicalForm, String[] constraints, double[] rightSide) {
        Fraction[][] standardized = standardize(canonicalForm, constraints);
        Fraction[][] result = new Fraction[standardized.length][standardized[0].length + 1];

        for (int i = 0; i < standardized.length; i++) {
            // Copy existing values
            for (int j = 0; j < standardized[i].length; j++) {
                result[i][j] = standardized[i][j] != null ? standardized[i][j] : new Fraction(0);
            }
            // Add RHS (initialize if null)
            result[i][result[i].length - 1] = new Fraction((int) rightSide[i], 1);
        }
        return result;
    }

    //*-------CORE:
    // Pivolt column search
    public static int selectEnteringBasis(Fraction[][] tableau, boolean isMaximization, boolean isPhase1) {
        int objectiveRow = tableau.length - 1;
        int enteringCol = -1;

        if (isPhase1) {
            Fraction mostNegative = new Fraction(Integer.MAX_VALUE);
            for (int j = 0; j < tableau[0].length - 1; j++) {
                if (tableau[objectiveRow][j].compareTo(mostNegative) < 0) {
                    mostNegative = tableau[objectiveRow][j];
                    enteringCol = j;
                }
            }
        } else if (isMaximization) {
            Fraction mostPositive = new Fraction(Integer.MIN_VALUE);
            for (int j = 0; j < tableau[0].length - 1; j++) {
                if (tableau[objectiveRow][j].compareTo(mostPositive) > 0) {
                    mostPositive = tableau[objectiveRow][j];
                    enteringCol = j;
                }
            }
        }
        return enteringCol;
    }
    
    // Pivolt row search by doing ratio btw enterCol/RHS
    public static int selectLeavingBasis(Fraction[][] tableau, int enteringCol) {
        int leavingRow = -1;
        Fraction minRatio = new Fraction(Integer.MAX_VALUE);

        for (int i = 0; i < tableau.length - 1; i++) {
            if (tableau[i][enteringCol].compareTo(new Fraction(0)) > 0) {
                Fraction ratio = tableau[i][tableau[i].length - 1].divide(tableau[i][enteringCol]);
                if (ratio.compareTo(minRatio) < 0) {
                    minRatio = ratio;
                    leavingRow = i;
                }
            }
        }
        return leavingRow;
    }

    private void executeSolvingProcess(Fraction[][] tableau, boolean isMaximization, boolean isPhase1) {
        int enteringCol = selectEnteringBasis(tableau, isMaximization, isPhase1);
        if (enteringCol == -1) return;

        int pivotRow = selectLeavingBasis(tableau, enteringCol);
        if (pivotRow == -1) throw new ArithmeticException("Unbounded problem");

        // Normalize pivot row
        Fraction pivotValue = tableau[pivotRow][enteringCol];
        for (int j = 0; j < tableau[pivotRow].length; j++) {
            tableau[pivotRow][j] = tableau[pivotRow][j].divide(pivotValue);
        }

        // Eliminate other rows
        for (int i = 0; i < tableau.length; i++) {
            if (i != pivotRow) {
                Fraction factor = tableau[i][enteringCol];
                for (int j = 0; j < tableau[i].length; j++) {
                    Fraction product = factor.multiply(tableau[pivotRow][j]);
                    tableau[i][j] = tableau[i][j].subtract(product);
                }
            }
        }
    }

    public Fraction[][] processSimple(Fraction[][] tableau, boolean isMaximization, boolean isAuxiliaryProblem, boolean isTwoPhases, List<SimplexStep> stepRecorder) {
        int maxIterations = 1000;
        int iterations = 0;
        
        iterationSteps.clear(); // Reset steps for new calculation
        while (true) {
            captureStep(tableau, stepRecorder);
            if (iterations++ > maxIterations) throw new ArithmeticException("Maximum iterations exceeded");
            
            // Check optimality using fraction comparisons
            boolean isOptimal;
            if (isMaximization) {
                isOptimal = !hasPositiveCoefficients(tableau);
            } else { 
                isOptimal = !hasNegativeCoefficients(tableau);
            }

            if (isOptimal) break;
            if(isTwoPhases && getFinalProcessValue(tableau).isZero()) break;

            executeSolvingProcess(tableau, isMaximization, isTwoPhases);
            System.out.println("----------Looping Processing----------");
            printTableau(tableau, variableNames);
        }
        return tableau;
    }

    //*-------VERIFICATIONS on the Objective Row
    private boolean hasPositiveCoefficients(Fraction[][] tableau) {
        int objectiveRow = tableau.length - 1;
        for (int j = 0; j < tableau[0].length - 1; j++) {
            if (tableau[objectiveRow][j].compareTo(new Fraction(0)) > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNegativeCoefficients(Fraction[][] tableau) {
        int objectiveRow = tableau.length - 1;
        for (int j = 0; j < tableau[0].length - 1; j++) {
            if (tableau[objectiveRow][j].compareTo(new Fraction(0)) < 0) {
                return true;
            }
        }
        return false;
    }

    //?-------Unused (Now using the two aboves for verifications)
    public int checkHowManyNotZeroYetInTheObjective(double[][] tableau) {
        int objectiveRow = tableau.length - 1;
        int numOriginalVars = tableau[0].length - getSlackVar() - 1;
        int count = 0;
        for (int j = 0; j < numOriginalVars; j++) {
            if (tableau[objectiveRow][j] != 0) {
                count++;
            }
        }
        return count;
    }

    //?======= FINAL PRINTS
    public Fraction getFinalProcessValue(Fraction[][] afterProcess) {
        return afterProcess[afterProcess.length - 1][afterProcess[0].length - 1];
    }

    public static void printTableau(Fraction[][] tableau, List<String> varNames) {
        List<String> basis = new ArrayList<>();
        
        // Identify basis variables
        for(int i=0; i<tableau.length-1; i++) {
            for(int j=0; j<tableau[i].length-1; j++) {
                if(tableau[i][j].equals(new Fraction(1))) {
                    boolean isBasic = true;
                    for(int k=0; k<tableau.length-1; k++) {
                        if(k != i && !tableau[k][j].isZero()) {
                            isBasic = false;
                            break;
                        }
                    }
                    if(isBasic) basis.add(varNames.get(j));
                }
            }
        }

        // Print tableau with improved absolute value formatting
        for(int i=0; i<tableau.length; i++) {
            String basisVar = (i < basis.size()) ? basis.get(i) : "OBJ";
            System.out.printf("%-4s:", basisVar);
            
            for(int j=0; j<tableau[i].length; j++) {
                Fraction currentFrac = tableau[i][j];
                String valueStr = currentFrac.toString();
                
                if(i == tableau.length-1 && j == tableau[i].length-1) {
                    int totalWidth = 10;
                    System.out.printf("%"+totalWidth + "s" + "|" + "%s" + "|", "", valueStr, "");
                } else {
                    System.out.printf("%12s", valueStr);
                }
            }
            System.out.println();
        }
    }

    public static void printFinalSolution(Fraction[][] tableau, List<String> varNames) {
        System.out.println("[============ Optimal Solution ============]");
        
        for(int j=0; j<varNames.size(); j++) {
            boolean isBasic = false;
            Fraction value = new Fraction(0);
            
            for(int i=0; i<tableau.length-1; i++) {
                if(tableau[i][j].equals(new Fraction(1))) {
                    boolean unique = true;
                    for(int k=0; k<tableau.length-1; k++) {
                        if(k != i && !tableau[k][j].isZero()) {
                            unique = false;
                            break;
                        }
                    }
                    if(unique) {
                        isBasic = true;
                        value = tableau[i][tableau[i].length-1];
                    }
                }
            }
            
            if(isBasic) {
                System.out.printf("%s = %s (%.2f)%n", varNames.get(j), value, value.toDouble());
            }
        }
        
        Fraction objValue = tableau[tableau.length-1][tableau[0].length-1];
        double objValueDouble = objValue.toDouble();
        if(objValueDouble < 0) {
            objValueDouble *= -1;
        }
        System.out.printf("Objective Value: |%s| (%.2f)%n", objValue.toString(), objValueDouble);
    }

    
    //?------NEW HELPER METHODS
    private void captureStep(Fraction[][] tableau, List<SimplexStep> stepRecorder) {
        SimplexStep step = new SimplexStep();
        step.setBasisVariables(getCurrentBasis(tableau));
        step.setTableauRows(convertTableauToStringArray(tableau));
        step.setObjectiveValue(getCurrentObjectiveValue(tableau));
        stepRecorder.add(step);
    }

    private List<String> getCurrentBasis(Fraction[][] tableau) {
        List<String> basis = new ArrayList<>();
        for(int i=0; i<tableau.length-1; i++) {
            for(int j=0; j<tableau[i].length-1; j++) {
                if(tableau[i][j].equals(new Fraction(1))) {
                    boolean isBasic = true;
                    for(int k=0; k<tableau.length-1; k++) {
                        if(k != i && !tableau[k][j].isZero()) {
                            isBasic = false;
                            break;
                        }
                    }
                    if(isBasic) basis.add(variableNames.get(j));
                }
            }
        }        
        return basis;
    }
     
    private List<List<String>> convertTableauToStringArray(Fraction[][] tableau) {
        List<List<String>> rows = new ArrayList<>();
        for(int i = 0; i< tableau.length; i++) {
            List<String> stringRow = new ArrayList<>();
            Fraction[] row = tableau[i];
            for(int j=0; j< row.length; j++) {
                String toAdd = row[j].toString();
                if(i == tableau.length-1 && j == row.length-1) {
                    toAdd = "| "+ toAdd + " |";
                }
                stringRow.add(toAdd);
            }
            rows.add(stringRow);
        }
        return rows;
    }

    private String getCurrentObjectiveValue(Fraction[][] tableau) {
        Fraction objValue = tableau[tableau.length-1][tableau[0].length-1];
        return String.format("|%s|", objValue.toString());
    }
}
