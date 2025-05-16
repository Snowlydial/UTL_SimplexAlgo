public class Main {
    public static void main(String[] args) {
        //?--------- Test with simple version
        System.out.println("<============SIMPLEX SIMPEL TEST============>");
        double[] OGobjectiveRow = {40, 30, 50};
        double[][] testArray = {
            {3,2,4},
            {2,1,3},
            {1,1,2},
            OGobjectiveRow  //last row is the fn objectif
        };
        double[] rightSide = {
            120,
            100,
            90,
            0 //begin with zero when maximizing
        };
        String[] constraints = {
            "<=", 
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
        // System.out.println("\n<============SIMPLEX IN TWO PHASES TEST============>");
        // double[][] twoPhaseArray = {
        //     {3, 2, 4},
        //     {2, 1, -1},
        //     {0, -1, 1}
        // };
        // double[] twoPhaseRHS = {40, 30, 50};
        // String[] twoPhaseConstraints = {"<=", ">=", ">="};
        // double[] originalObjective = {2, 3, 1};  // Max(2x + 3y + z)

        // SimplexTwoPhases sptp = new SimplexTwoPhases();
        // sptp.setOriginalObjective(originalObjective);

        // System.out.println("[==================PHASE_1==================]");
        // Fraction[][] afterPhase1 = sptp.phaseOneProcess(twoPhaseArray, twoPhaseRHS, twoPhaseConstraints);

        // System.out.println("[==================PHASE_2==================]");
        // Fraction[][] afterPhase2 = sptp.phaseTwoProcess(afterPhase1);

        // SimplexSimple.printFinalSolution(afterPhase2, originalObjective);
    }
}
