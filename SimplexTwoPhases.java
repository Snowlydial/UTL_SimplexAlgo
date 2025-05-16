import java.util.ArrayList;
import java.util.Arrays;

public class SimplexTwoPhases extends SimplexSimple {
    private int numArtificial = 0;
    private ArrayList<Integer> artificialVariableColumns = new ArrayList<>();
    private double[] originalObjective; //Unused

    //?----------Constructor, Get and Set
    public SimplexTwoPhases() {
    }

    //*-------Setters
    public void setOriginalObjective(double[] objective) {
        this.originalObjective = objective;
    }
    public void setNumArtificial(int _numArtificial) {
        this.numArtificial = _numArtificial;
    }

    //*-------Getters
    public int getNumArtificial() {
        return this.numArtificial;
    }
    public double[] getOriginalObjective() {
        return this.originalObjective;
    }
    public ArrayList<Integer> getIndexOfArtificialVariables() {
        return this.artificialVariableColumns;
    }

    //?---------MAIN METHODS
    public double[][] phaseOneProcess(double[][] canonicalForm, double[] rhs, String[] constraints) {
        //?----Step 1: Add artificial variables
        double[][] standardizedWithArtificials = addArtificialVariables(canonicalForm, constraints);
        double[][] standardizedWithRHS = fuseWithRightSide(standardizedWithArtificials, rhs);
        
        //?----Step 2: Create W row (sum of artificial rows)
        double[] wRow = constructPhaseOneObjective(standardizedWithRHS);

        //?----Step 3: Create Phase 1 tableau
        double[][] phaseOneTableau = new double[standardizedWithRHS.length + 1][];
        System.arraycopy(standardizedWithRHS, 0, phaseOneTableau, 0, standardizedWithRHS.length);
        phaseOneTableau[phaseOneTableau.length - 1] = wRow;
        System.out.println("Initial tableau: ");
        printTableau(phaseOneTableau);

        //?----Step 4: Solve Phase 1
        processSimple(phaseOneTableau, true, true, true);

        // Check if problem is feasible (W = 0)
        double wValue = phaseOneTableau[phaseOneTableau.length - 1][phaseOneTableau[0].length - 1];
        if (Math.abs(wValue) > 1e-6) {
            throw new ArithmeticException("Problem is infeasible");
        }

        // Remove artificial columns and W row
        double[][] cleanedFromArtifAndObj = cleanPhase1Tableau(phaseOneTableau);

        // System.out.println("Cleaned tableau: ");printTableau(cleanedFromArtifAndObj);
        return cleanedFromArtifAndObj;
    }

    public double[][] phaseTwoProcess(double[][] phaseOneCleaned) {
        // Add original objective row
        double[][] phaseTwoTableau = new double[phaseOneCleaned.length + 1][];
        System.arraycopy(phaseOneCleaned, 0, phaseTwoTableau, 0, phaseOneCleaned.length);

        // Initialize objective row with original coefficients
        double[] adjustedObjRow = new double[phaseOneCleaned[0].length];
        Arrays.fill(adjustedObjRow, 0);
        for (int i = 0; i < originalObjective.length; i++) {
            adjustedObjRow[i] = originalObjective[i];
        }

        // Adjust objective row based on basis variables
        adjustObjectiveRow(phaseTwoTableau, adjustedObjRow);

        // Set the adjusted objective row
        phaseTwoTableau[phaseTwoTableau.length - 1] = adjustedObjRow;

        System.out.println("Adjusted Phase 2 Tableau:");
        printTableau(phaseTwoTableau);

        // Solve Phase 2
        processSimple(phaseTwoTableau, true, false, false);
        return phaseTwoTableau;
    }

    //*---------CORE
    public double[][] addArtificialVariables(double[][] canonicalForm, String[] constraints) {
        // Standardize the problem (add slack/surplus variables) then add artif
        double[][] stdForm = standardize(canonicalForm, constraints);
    
        int numArtificial = 0;
        for (String constraint : constraints) {
            if (constraint.equals("=") || constraint.equals(">=")) {
                numArtificial++;
            }
        }
    
        setNumArtificial(numArtificial);

        int numRows = stdForm.length;
        int numCols = stdForm[0].length + numArtificial;
        double[][] withArtificials = new double[numRows][numCols];
    
        int nextArtificialCol = stdForm[0].length;    
        for (int i = 0; i < numRows; i++) {
            System.arraycopy(stdForm[i], 0, withArtificials[i], 0, stdForm[i].length);
            
            if (constraints[i].equals("=") || constraints[i].equals(">=")) {
                withArtificials[i][nextArtificialCol] = 1;
                artificialVariableColumns.add(nextArtificialCol);
                nextArtificialCol++;
            }
        }
    
        return withArtificials;
    }

    private double[] constructPhaseOneObjective(double[][] standardizedWithRHS) {
        double[] wRow = new double[standardizedWithRHS[0].length];
        for (int i = 0; i < standardizedWithRHS.length; i++) {
            // Check if this row has an artificial variable
            boolean isArtificialRow = isArtificialRow(standardizedWithRHS[i]);
        
            if (isArtificialRow) {
                for (int j = 0; j < wRow.length; j++) {
                    wRow[j] += standardizedWithRHS[i][j];  // Sum all artificial rows
                }
            }
        }
        // Negate the W-row (since we maximize -W instead of minimizing W)
        for (int j = 0; j < wRow.length; j++) {
            wRow[j] = -wRow[j];
        }

        // Set all artificial variables to 0 for the objective row
        for (int col : artificialVariableColumns) {
            wRow[col] = 0;
        }
        return wRow;
    }

    private void adjustObjectiveRow(double[][] tableau, double[] objectiveRow) {
        int numRows = tableau.length - 1; // Exclude objective row
        int numCols = tableau[0].length;

        // For each basic variable, subtract its contribution from the objective row
        for (int i = 0; i < numRows; i++) {
            int basisCol = -1;
            // Find the basic variable in row i
            for (int j = 0; j < numCols - 1; j++) { // Exclude RHS column
                if (tableau[i][j] == 1.0) {
                    boolean isBasic = true;
                    for (int k = 0; k < numRows; k++) {
                        if (k != i && Math.abs(tableau[k][j]) > 1e-6) {
                            isBasic = false;
                            break;
                        }
                    }
                    if (isBasic) {
                        basisCol = j;
                        break;
                    }
                }
            }
            if (basisCol == -1) continue;

            // Get coefficient of the basic variable in the original objective
            double coeff = (basisCol < originalObjective.length) ? originalObjective[basisCol] : 0;

            // Subtract (coeff * row i) from the objective row
            for (int j = 0; j < numCols; j++) {
                objectiveRow[j] -= coeff * tableau[i][j];
            }
        }
    }

    //*---------UTILITY
    private double[][] cleanPhase1Tableau(double[][] tableau) {
        int newCols = tableau[0].length - artificialVariableColumns.size();
        double[][] cleaned = new double[tableau.length - 1][newCols]; // Exclude W row

        for (int i = 0; i < cleaned.length; i++) {
            int newCol = 0;
            for (int oldCol = 0; oldCol < tableau[i].length; oldCol++) {
                if (!artificialVariableColumns.contains(oldCol)) {
                    cleaned[i][newCol++] = tableau[i][oldCol];
                }
            }
        }
        return cleaned;
    }

    public double[][] fuseWithRightSide(double[][] matrix, double[] rightSide) {
        int rows = matrix.length;
        int cols = matrix[0].length + 1;
        double[][] result = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            System.arraycopy(matrix[i], 0, result[i], 0, matrix[i].length);
            result[i][cols - 1] = rightSide[i];
        }

        return result;
    }

    private boolean isArtificialRow(double[] row) {
        for (int col : artificialVariableColumns) {
            if (row[col] == 1.0) return true;
        }
        return false;
    }
}
