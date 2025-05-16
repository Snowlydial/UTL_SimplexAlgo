import java.util.ArrayList;
import java.util.Arrays;

public class SimplexTwoPhases extends SimplexSimple {
    private int numArtificial = 0;
    private ArrayList<Integer> artificialVariableColumns = new ArrayList<>();
    private Fraction[] originalObjective;

    //?----------Constructor, Get and Set
    public SimplexTwoPhases() {
    }

    //*-------Setters
    public void setOriginalObjective(double[] objective) {
        this.originalObjective = new Fraction[objective.length];
        for (int i = 0; i < objective.length; i++) {
            this.originalObjective[i] = new Fraction((int) objective[i], 1);
        }
    }
    public void setNumArtificial(int _numArtificial) {
        this.numArtificial = _numArtificial;
    }

    //*-------Getters
    public int getNumArtificial() {
        return this.numArtificial;
    }
    public Fraction[] getOriginalObjective() {
        return this.originalObjective;
    }
    public ArrayList<Integer> getIndexOfArtificialVariables() {
        return this.artificialVariableColumns;
    }

    //?---------MAIN METHODS
    public Fraction[][] phaseOneProcess(double[][] canonicalForm, double[] rhs, String[] constraints) {
        // Step 1: Add artificial variables
        Fraction[][] standardizedWithArtificials = addArtificialVariables(canonicalForm, constraints);
        Fraction[][] standardizedWithRHS = fuseWithRightSide(standardizedWithArtificials, rhs);

        // Step 2: Create W-row
        Fraction[] wRow = constructPhaseOneObjective(standardizedWithRHS);
        
        // Step 3: Build Phase 1 tableau
        Fraction[][] phaseOneTableau = new Fraction[standardizedWithRHS.length + 1][];
        System.arraycopy(standardizedWithRHS, 0, phaseOneTableau, 0, standardizedWithRHS.length);
        phaseOneTableau[phaseOneTableau.length - 1] = wRow;

        System.out.println("Initial Phase 1 Tableau:");
        printTableau(phaseOneTableau);

        // Step 4: Solve Phase 1
        processSimple(phaseOneTableau, true, true, true);

        // Check feasibility using fractions
        Fraction wValue = phaseOneTableau[phaseOneTableau.length - 1][phaseOneTableau[0].length - 1];
        if (!wValue.isZero()) {
            throw new ArithmeticException("Problem is infeasible (W = " + wValue + ")");
        }

        // Clean up artificial variables and W-row
        return cleanPhase1Tableau(phaseOneTableau);
    }

    public Fraction[][] phaseTwoProcess(Fraction[][] phaseOneCleaned) {
        // Add original objective row
        Fraction[][] phaseTwoTableau = new Fraction[phaseOneCleaned.length + 1][];
        System.arraycopy(phaseOneCleaned, 0, phaseTwoTableau, 0, phaseOneCleaned.length);

        // Initialize objective row with original coefficients (now stored as Fraction[])
        Fraction[] adjustedObjRow = new Fraction[phaseOneCleaned[0].length];
        Arrays.fill(adjustedObjRow, new Fraction(0));
        for (int i = 0; i < originalObjective.length; i++) {
            adjustedObjRow[i] = originalObjective[i];
        }

        // Adjust objective row based on basis variables
        adjustObjectiveRow(phaseTwoTableau, adjustedObjRow);

        // Set the adjusted objective row
        phaseTwoTableau[phaseTwoTableau.length - 1] = adjustedObjRow;

        System.out.println("Adjusted Phase 2 Tableau:");
        printTableau(phaseTwoTableau);

        // Solve Phase 2 with fractions
        processSimple(phaseTwoTableau, true, false, false);
        return phaseTwoTableau;
    }

    //*---------CORE
    private Fraction[][] addArtificialVariables(double[][] canonicalForm, String[] constraints) {
        Fraction[][] stdForm = standardize(canonicalForm, constraints);
        int numArtificial = 0;
        for (String c : constraints) if (c.equals(">=") || c.equals("=")) numArtificial++;
        
        Fraction[][] withArtificials = new Fraction[stdForm.length][stdForm[0].length + numArtificial];
        
        // Initialize all elements first
        for (int i = 0; i < withArtificials.length; i++) {
            for (int j = 0; j < withArtificials[i].length; j++) {
                withArtificials[i][j] = new Fraction(0);  // Initialize to zero
            }
        }
        
        int nextArtCol = stdForm[0].length;
        
        for (int i = 0; i < stdForm.length; i++) {
            // Copy existing values
            for (int j = 0; j < stdForm[i].length; j++) {
                withArtificials[i][j] = stdForm[i][j];
            }
            
            if (constraints[i].equals(">=") || constraints[i].equals("=")) {
                withArtificials[i][nextArtCol] = new Fraction(1);
                artificialVariableColumns.add(nextArtCol);
                nextArtCol++;
            }
        }
        return withArtificials;
    }

    private Fraction[] constructPhaseOneObjective(Fraction[][] standardizedWithRHS) {
        Fraction[] wRow = new Fraction[standardizedWithRHS[0].length];
        // Initialize wRow
        for (int j = 0; j < wRow.length; j++) {
            wRow[j] = new Fraction(0);
        }

        for (int i = 0; i < standardizedWithRHS.length; i++) {
            if (isArtificialRow(standardizedWithRHS[i])) {
                for (int j = 0; j < wRow.length; j++) {
                    if (standardizedWithRHS[i][j] != null) {
                        wRow[j] = wRow[j].add(standardizedWithRHS[i][j]);
                    }
                }
            }
        }
        // Negate W-row and zero artificials (since we maximize -W instead of minimizing W)
        for (int j = 0; j < wRow.length; j++) {
            wRow[j] = wRow[j].multiply(new Fraction(-1));
            if (artificialVariableColumns.contains(j)) {
                wRow[j] = new Fraction(0);
            }
        }
        return wRow;
    }

    private void adjustObjectiveRow(Fraction[][] tableau, Fraction[] objectiveRow) {
        int numRows = tableau.length - 1;
        int numCols = tableau[0].length;

        // For each basic variable, subtract its contribution from the objective row
        for (int i = 0; i < numRows; i++) {
            int basisCol = -1;
            // Find the basic variable in row i
            for (int j = 0; j < numCols - 1; j++) {
            if (tableau[i][j].equals(new Fraction(1))) {
                boolean isBasic = true;
                for (int k = 0; k < numRows; k++) {
                    if (k != i && !tableau[k][j].isZero()) {
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
    // Subtract basis variable's contribution from objective row
            Fraction coeff = (basisCol < originalObjective.length) ? 
                originalObjective[basisCol] : new Fraction(0);
            for (int j = 0; j < numCols; j++) {
                Fraction product = coeff.multiply(tableau[i][j]);
                objectiveRow[j] = objectiveRow[j].subtract(product);
            }
        }
    }

    //*---------UTILITY
    private Fraction[][] cleanPhase1Tableau(Fraction[][] tableau) {
        int newCols = tableau[0].length - artificialVariableColumns.size();
        Fraction[][] cleaned = new Fraction[tableau.length - 1][newCols]; // Exclude W-row

        for (int i = 0; i < cleaned.length; i++) {
            int newColIdx = 0;
            for (int oldColIdx = 0; oldColIdx < tableau[i].length; oldColIdx++) {
                // Keep only non-artificial columns
                if (!artificialVariableColumns.contains(oldColIdx)) {
                    cleaned[i][newColIdx++] = tableau[i][oldColIdx];
                }
            }
        }
        return cleaned;
    }

    private Fraction[][] fuseWithRightSide(Fraction[][] matrix, double[] rhs) {
        Fraction[][] result = new Fraction[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            result[i] = new Fraction[matrix[i].length + 1];
            for (int j = 0; j < matrix[i].length; j++) {
                result[i][j] = matrix[i][j] != null ? matrix[i][j] : new Fraction(0);
            }
            result[i][matrix[i].length] = new Fraction((int) rhs[i], 1);
        }
        return result;
    }

    private boolean isArtificialRow(Fraction[] row) {
        if (row == null) return false;
        
        Fraction one = new Fraction(1);
        for (int col : artificialVariableColumns) {
            if (col < row.length && row[col] != null && row[col].equals(one)) {
                return true;
            }
        }
        return false;
    }
}
