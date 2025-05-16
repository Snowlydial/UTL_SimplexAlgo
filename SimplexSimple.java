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
    protected Fraction[][] standardize(double[][] canonicalForm, String[] constraints) {
        if (canonicalForm.length != constraints.length) {
            throw new IllegalArgumentException("Mismatched array lengths");
        }

        int numRows = canonicalForm.length;
        int numOriginalVars = canonicalForm[0].length;
        int numSlackVars = 0;

        for (String constraint : constraints) {
            if (!constraint.equals("=")) numSlackVars++;
        }
        setNumberOfSlackVar(numSlackVars);

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

    public Fraction[][] processSimple(Fraction[][] tableau, boolean isMaximization, boolean isAuxiliaryProblem, boolean isTwoPhases) {
        int maxIterations = 1000;
        int iterations = 0;
        
        while (true) {
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
            printTableau(tableau);
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

    public static void printTableau(Fraction[][] tableau) {
        for (int i = 0; i < tableau.length; i++) {
            System.out.print("Row" + i + ": ");
            for (int j = 0; j < tableau[i].length; j++) {
                Fraction val = tableau[i][j];
                System.out.printf("%12s", val != null ? val.toString() : "null");
            }
            System.out.println();
        }
    }

    public static void printFinalSolution(Fraction[][] tableau, double[] originalObjective) {
        int numCols = tableau[0].length - 1;
        Fraction[] solution = new Fraction[numCols];
        Arrays.fill(solution, new Fraction(0));

        for (int i = 0; i < tableau.length - 1; i++) {
            for (int j = 0; j < numCols; j++) {
                if (tableau[i][j].equals(new Fraction(1))) {
                    solution[j] = tableau[i][numCols];
                    break;
                }
            }
        }

        System.out.println("[============ Optimal Solution ============]");
        for (int j = 0; j < originalObjective.length; j++) {
            System.out.printf("x%d = %s\n", j+1, solution[j].toString());
        }
        System.out.printf("Objective Value: %s\n",  Math.abs(Integer.parseInt(tableau[tableau.length-1][numCols].toString())));
    }
}
