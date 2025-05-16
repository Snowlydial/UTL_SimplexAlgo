import java.util.Arrays;

public class SimplexSimple {
    /* Supposons le probleme de maximisation suivant:
     * Max(12x + 16y)
     * 10x + 20y <= 120
     * 8x + 8y <= 80
     * x,y >= 0
     * 
     * Ceci est la forme canonique, nous devons la mettre sous forme standard:
     * Max(12x + 16y)
     * 10x + 20y + s1 = 120
     * 8x + 8y + s2 = 80
     * x, y, s1, s2 >= 0
    */

    private int numberOfSlackvars = 0;

    //?--------Constructors and Get/Set
    public SimplexSimple() {}

    public int getSlackVar() {
        return numberOfSlackvars;
    }

    public void setNumberOfSlackVar(int nblack) {
        this.numberOfSlackvars = nblack;
    }

    //?--------MAIN METHODS
    //*-------Canonical to standard:
    protected double[][] standardize(double[][] canonicalForm, String[] constraints) {
        if (canonicalForm.length != constraints.length) {
            throw new IllegalArgumentException("Mismatched array lengths, tried to do in standardize()");
        }
    
        int numRows = canonicalForm.length;
        int numOriginalVars = canonicalForm[0].length;
        int numSlackVars = 0;
    
        for (String constraint : constraints) {
            if (!constraint.equals("=")) {
                numSlackVars++;
            }
        }
    
        setNumberOfSlackVar(numSlackVars);

        double[][] standardLHS = new double[numRows][numOriginalVars + numSlackVars];
    
        int slackVarIndex = 0; // Tracks where to add the next slack variable (to avoid overlap)
        for (int i = 0; i < numRows; i++) {
            System.arraycopy(canonicalForm[i], 0, standardLHS[i], 0, numOriginalVars);
            String constraint = constraints[i];
            if (constraint.equals("<=") || constraint.equals("<")) {
                standardLHS[i][numOriginalVars + slackVarIndex] = 1;
                slackVarIndex++;
            } else if (constraint.equals(">=")) { // Not used in regular simplex
                standardLHS[i][numOriginalVars + slackVarIndex] = -1;
                slackVarIndex++;
            }
        }
        return standardLHS;
    }

    public double[][] standardize_then_fuse_rightSide(double[][] canonicalForm, String[] constraints, double[] rightSide) {
        if (canonicalForm.length != constraints.length || canonicalForm.length != rightSide.length) {
            throw new IllegalArgumentException("Mismatched array lengths, tried to do in standardize_then_fuse_rightside()");
        }

        double[][] standardized = standardize(canonicalForm, constraints);

        double[][] result = new double[standardized.length][standardized[0].length + 1];
        for (int i = 0; i < standardized.length; i++) {
            System.arraycopy(standardized[i], 0, result[i], 0, standardized[i].length);
            result[i][result[i].length - 1] = rightSide[i];
        }
        return result;
    }

    //*-------CORE:
    public static int selectEnteringBasis(double[][] tableau, boolean isMaximization, boolean isPhase1) {
        int enteringCol = -1;
        int objectiveRow = tableau.length - 1;

        // Phase 1: Prioritize columns with artificial variables first
        if (isPhase1) { // Look for the most negative coefficient in W-row (since we maximize -W)
            double mostNegative = Double.POSITIVE_INFINITY;
            for (int j = 0; j < tableau[0].length - 1; j++) {
                double coeff = tableau[objectiveRow][j];
                if (coeff < mostNegative) {
                    mostNegative = coeff;
                    enteringCol = j;
                }
            }
        } else if (isMaximization) { // Phase 2: Standard simplex rule (most positive for maximization)
            double mostPositive = Double.NEGATIVE_INFINITY;
            for (int j = 0; j < tableau[0].length - 1; j++) {
                double coeff = tableau[objectiveRow][j];
                if (coeff > mostPositive) {
                    mostPositive = coeff;
                    enteringCol = j;
                }
            }
        } 
        // Minimization (if ever needed, though we usually convert to maximization)
        else {
            double mostNegative = Double.POSITIVE_INFINITY;
            for (int j = 0; j < tableau[0].length - 1; j++) {
                double coeff = tableau[objectiveRow][j];
                if (coeff < mostNegative) {
                    mostNegative = coeff;
                    enteringCol = j;
                }
            }
        }

        return enteringCol;
    }
    
    public static int selectLeavingBasis(double[][] tableau, int enteringBasisCol) {
        int leavingRow = -1;
        double minRatio = Double.POSITIVE_INFINITY;
    
        for (int i = 0; i < tableau.length - 1; i++) {
            if (tableau[i][enteringBasisCol] > 0) { // Avoid division by zero
                double ratio = tableau[i][tableau[i].length - 1] / tableau[i][enteringBasisCol];
                if (ratio < minRatio) {
                    minRatio = ratio;
                    leavingRow = i;
                }
            }
        }
        return leavingRow;
    }

    private void executeSolvingProcess(double[][] tableau, boolean isMaximization, boolean isPhase1) {
        int enteringBasisCol = selectEnteringBasis(tableau, isMaximization, isPhase1);
        if (enteringBasisCol == -1) return; // No positive coefficients (optimal)

        int pivotRow = selectLeavingBasis(tableau, enteringBasisCol);
        if (pivotRow == -1) throw new ArithmeticException("Problem is not solvable");

        double pivotValue = tableau[pivotRow][enteringBasisCol];
        for (int j = 0; j < tableau[pivotRow].length; j++) {
            tableau[pivotRow][j] /= pivotValue;
        }

        for (int i = 0; i < tableau.length; i++) {
            if (i != pivotRow) {
                double factor = tableau[i][enteringBasisCol];
                for (int j = 0; j < tableau[i].length; j++) {
                    tableau[i][j] -= factor * tableau[pivotRow][j];
                }
            }
            // System.out.print("Row" + i + ": ");
            // for (int j = 0; j < tableau[i].length; j++) {
            //     System.out.printf("%10.3f ", tableau[i][j]);
            // }
            // System.out.println();
        }
    }

    public double[][] processSimple(double[][] tableau, boolean isMaximization, boolean isAuxiliaryProblem, boolean isTwoPhases) {
        int maxIterations = 1000;
        int iterations = 0;
        
        while (true) {
            if (iterations++ > maxIterations) throw new ArithmeticException("Maximum iterations exceeded");
            
            // Check optimality based on objective row coefficients
            boolean isOptimal;
            if (isMaximization) {
                isOptimal = !hasPositiveCoefficients(tableau);
            } else { // should not be used anymore
                isOptimal = !hasNegativeCoefficients(tableau);
            }
    
            if (isOptimal) break;
            if(isTwoPhases) if(getFinalProcessValue(tableau) == 0) break;

            executeSolvingProcess(tableau, isMaximization, isTwoPhases);

            System.out.println("----------Looping Processing----------");
            printTableau(tableau);
        }
        return tableau;
    }

    //*-------VERIFICATIONS on the Objective Row
    private boolean hasPositiveCoefficients(double[][] tableau) {
        int objectiveRow = tableau.length - 1;
        for (int j = 0; j < tableau[0].length - 1; j++) {
            if (tableau[objectiveRow][j] > 0) return true;
        }
        return false;
    }

    private boolean hasNegativeCoefficients(double[][] tableau) {
        int objectiveRow = tableau.length - 1;
        for (int j = 0; j < tableau[0].length - 1; j++) {
            if (tableau[objectiveRow][j] < 0) return true;
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
    public double getFinalProcessValue(double[][] afterProcess) {
        double finalValue = afterProcess[afterProcess.length - 1][afterProcess[0].length - 1];
        return Math.abs(finalValue);
    }

    public static void printFinalSolution(double[][] tableau, double[] originalObjective) {
        int numRows = tableau.length - 1;  // Exclude objective row
        int numCols = tableau[0].length - 1;  // Exclude RHS column
        double[] solution = new double[numCols];  // Stores variable values (x1, x2, ..., slack vars)
        Arrays.fill(solution, 0.0);

        // Identify basic variables and their RHS values
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                if (tableau[i][j] == 1.0) {
                    boolean isBasic = true;
                    for (int k = 0; k < numRows; k++) {
                        if (k != i && Math.abs(tableau[k][j]) > 1e-6) {
                            isBasic = false;
                            break;
                        }
                    }
                    if (isBasic) {
                        solution[j] = tableau[i][numCols];
                        break;
                    }
                }
            }
        }

        // Print variable solutions
        System.out.println("[==================Optimal Solution==================]");
        for (int j = 0; j < numCols; j++) {
            if (j < originalObjective.length) {
                System.out.printf("x%d = %.2f\n", j + 1, solution[j]);
            } else {
                System.out.printf("s%d = %.2f\n", j - originalObjective.length + 1, solution[j]);
            }
        }

        // Print objective value
        double objectiveValue = Math.abs(tableau[tableau.length - 1][numCols]);
        System.out.printf("Objective Value = %.2f\n", objectiveValue);
    }

    public static void printTableau(double[][] tableau) {
        for (int i = 0; i < tableau.length; i++) {
            System.out.print("Row" + i + ": ");
            for (int j = 0; j < tableau[i].length; j++) {
                System.out.printf("%10.3f ", tableau[i][j]);
            }
            System.out.println();
        }
    }
}
