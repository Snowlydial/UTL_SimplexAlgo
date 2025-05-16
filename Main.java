public class Main {
    public static void main(String[] args) {
        //?--------- Test with simple version
        System.out.println("<============SIMPLEX SIMPEL TEST============>");
        double[] OGobjectiveRow = {12, 16};
        double[][] testArray = {
            {10,20},
            {8,8},
            OGobjectiveRow  //last row is the fn objectif
        };
        double[] rightSide = {
            120,
            80,
            0 //begin with zero when maximizing
        };
        String[] constraints = {
            "<=", 
            "<=",
            "==" //whatever sign here, doesn't matter in simple ver
        };

        SimplexSimple spsp = new SimplexSimple();
        Fraction[][] standardized =spsp.standardize_then_fuse_rightSide(testArray, constraints, rightSide);
        System.out.println("Initial tableau:");
        SimplexSimple.printTableau(standardized);
        
        Fraction[][] afterProcess = spsp.processSimple(standardized, true, false, false);
        SimplexSimple.printFinalSolution(afterProcess, OGobjectiveRow);

        //?--------- Test with two phase version
        System.out.println("\n<============SIMPLEX IN TWO PHASES TEST============>");
        double[][] twoPhaseArray = {
            {1, 1, 1},
            {2, 1, -1},
            {1, 0, 0},
            {0, 1, 0},
            {0, 0, 1} 
        };
        double[] twoPhaseRHS = {10, 10, 0, 0, 0};
        String[] twoPhaseConstraints = {"=", ">=", ">=", ">=", ">="};
        double[] originalObjective = {4, 5};  // Max(2x + 3y + z)

        SimplexTwoPhases sptp = new SimplexTwoPhases();
        sptp.setOriginalObjective(originalObjective);

        System.out.println("[==================PHASE_1==================]");
        Fraction[][] afterPhase1 = sptp.phaseOneProcess(twoPhaseArray, twoPhaseRHS, twoPhaseConstraints);

        System.out.println("[==================PHASE_2==================]");
        Fraction[][] afterPhase2 = sptp.phaseTwoProcess(afterPhase1);

        SimplexSimple.printFinalSolution(afterPhase2, originalObjective);
    }
}
