package com.itu.core;

import java.util.*;

import com.itu.dto.BranchAndBoundStep;

public class SimplexBranchAndBound {
    private class BranchNode {
        double[][] constraints;
        double[] rhs;
        String[] constraintTypes;
        double[] objective;
        List<String> branchingConstraints;
        List<String> branchPath; // NEW: Track path from root to this node
        
        Map<String, Double> solution;
        double upperBound;
        boolean feasible;
        int nodeId;
        int parentNodeId; // NEW: Track parent
        
        public BranchNode(double[][] constraints, double[] rhs, String[] constraintTypes,
                         List<String> branchingConstraints, double[] objective) {
            this.constraints = constraints;
            this.rhs = rhs;
            this.constraintTypes = constraintTypes;
            this.branchingConstraints = branchingConstraints;
            this.objective = objective;
            this.nodeId = ++nodeCounter;
            this.feasible = true;
            this.upperBound = Double.NEGATIVE_INFINITY;
            this.branchPath = new ArrayList<>(); // Initialize empty path
            this.parentNodeId = -1; // Root has no parent
        }

        // Constructor for child nodes
        public BranchNode(double[][] constraints, double[] rhs, String[] constraintTypes,
                        List<String> branchingConstraints, double[] objective, 
                        List<String> parentPath, int parentId) {
            this(constraints, rhs, constraintTypes, branchingConstraints, objective);
            this.branchPath = new ArrayList<>(parentPath); // Copy parent's path
            this.parentNodeId = parentId;
        }
    }

    private SimplexSimple simplexSolver;
    private SimplexTwoPhases twoPhasesSolver;
    private boolean needsTwoPhase;
    
    private Map<String, Double> bestIntegerSolution;
    private double bestIntegerObjective;
    private boolean foundIntegerSolution;
    
    private List<BranchAndBoundStep> branchingSteps;
    private int nodeCounter;
    
    //?----Constructors
    public SimplexBranchAndBound() {
        this.simplexSolver = new SimplexSimple();
        this.twoPhasesSolver = new SimplexTwoPhases();
        this.bestIntegerSolution = new HashMap<>();
        this.bestIntegerObjective = Double.NEGATIVE_INFINITY;
        this.foundIntegerSolution = false;
        this.branchingSteps = new ArrayList<>();
        this.nodeCounter = 0;
    }
    
    //?----Functions
    public Map<String, Object> solve(double[] objective, double[][] constraints, 
                                   double[] rhs, String[] constraintTypes) {
        // Check if two-phase method is needed
        needsTwoPhase = Arrays.stream(constraintTypes)
                             .anyMatch(c -> c.equals(">=") || c.equals("="));
        
        // Create root node
        BranchNode rootNode = new BranchNode(constraints, rhs, constraintTypes, 
                                           new ArrayList<>(), objective);
        
        // Initialize priority queue for branch exploration (best-first search)
        PriorityQueue<BranchNode> nodeQueue = new PriorityQueue<>((a, b) -> 
            Double.compare(b.upperBound, a.upperBound));
        
        // Solve root node
        Map<String, Object> rootResult = solveNode(rootNode);
        if (rootResult.containsKey("error")) {
            return rootResult;
        }
        
        nodeQueue.offer(rootNode);
        
        // Branch and bound main loop
        while (!nodeQueue.isEmpty()) {
            BranchNode currentNode = nodeQueue.poll();
            
            // Pruning: if upper bound is worse than best integer solution, skip
            if (foundIntegerSolution && currentNode.upperBound <= bestIntegerObjective) {
                continue;
            }
            
            // Check if solution is integer
            if (isIntegerSolution(currentNode.solution)) {
                double objectiveValue = calculateObjectiveValue(currentNode.solution, objective);
                if (objectiveValue > bestIntegerObjective) {
                    bestIntegerObjective = objectiveValue;
                    bestIntegerSolution = new HashMap<>(currentNode.solution);
                    foundIntegerSolution = true;
                    
                    // Record this as a solution step WITH PATH TRACKING
                    BranchAndBoundStep step = new BranchAndBoundStep();
                    step.setNodeId(currentNode.nodeId);
                    step.setNodeType("INTEGER_SOLUTION");
                    step.setObjectiveValue(objectiveValue);
                    step.setSolution(new HashMap<>(currentNode.solution));
                    step.setBranchPath(new ArrayList<>(currentNode.branchPath)); // NEW: Add path
                    step.setParentNodeId(currentNode.parentNodeId); // NEW: Add parent
                    
                    // Create detailed message with path
                    StringBuilder pathMessage = new StringBuilder();
                    pathMessage.append("Found integer solution with objective value: ").append(objectiveValue);
                    if (!currentNode.branchPath.isEmpty()) {
                        pathMessage.append("\nBranch Path: ");
                        pathMessage.append(String.join(" → ", currentNode.branchPath));
                    }
                    step.setMessage(pathMessage.toString());
                    
                    branchingSteps.add(step);
                }
                continue;
            }
            
            // Find fractional variable to branch on
            String branchVariable = selectBranchingVariable(currentNode.solution);
            if (branchVariable == null) continue;
            
            double fractionalValue = currentNode.solution.get(branchVariable);
            int floorValue = (int) Math.floor(fractionalValue);
            
            // Create two child nodes
            BranchNode leftChild = createChildNode(currentNode, branchVariable, "<=", floorValue);
            BranchNode rightChild = createChildNode(currentNode, branchVariable, ">=", floorValue + 1);

            // Solve left child (x_i <= floor(x_i*))
            Map<String, Object> leftResult = solveNode(leftChild);
            double leftBound = 0.0;
            if (!leftResult.containsKey("error") && leftChild.feasible) {
                leftBound = leftChild.upperBound;
                if (leftChild.upperBound > bestIntegerObjective) {
                    nodeQueue.offer(leftChild);
                }
            } else {
                leftBound = Double.NEGATIVE_INFINITY;
            }

            // Solve right child (x_i >= floor(x_i*) + 1)
            Map<String, Object> rightResult = solveNode(rightChild);
            double rightBound = 0.0;
            if (!rightResult.containsKey("error") && rightChild.feasible) {
                rightBound = rightChild.upperBound;
                if (rightChild.upperBound > bestIntegerObjective) {
                    nodeQueue.offer(rightChild);
                }
            } else {
                rightBound = Double.NEGATIVE_INFINITY;
            }
            
            // Record branching step
            BranchAndBoundStep step = new BranchAndBoundStep();
            step.setNodeId(currentNode.nodeId);
            step.setNodeType("BRANCHING");
            step.setObjectiveValue(currentNode.upperBound);
            step.setBranchVariable(branchVariable);
            step.setBranchValue(fractionalValue);
            // Handle NEGATIVE_INFINITY properly
            step.setLeftChildBound(Double.isInfinite(leftBound) && leftBound < 0 ? -999999.0 : leftBound);
            step.setRightChildBound(Double.isInfinite(rightBound) && rightBound < 0 ? -999999.0 : rightBound);
            step.setMessage(String.format("Branching on %s = %.3f", branchVariable, fractionalValue));
            branchingSteps.add(step);
        }
        
        // Prepare final result
        Map<String, Object> result = new HashMap<>();
        if (foundIntegerSolution) {
            result.put("variables", bestIntegerSolution);
            result.put("objective", bestIntegerObjective);
            result.put("branchingSteps", branchingSteps);
            result.put("solutionType", "INTEGER_OPTIMAL");
        } else {
            result.put("error", "No integer solution found");
        }
        
        return result;
    }
    
    private Map<String, Object> solveNode(BranchNode node) {
        try {
            Map<String, Object> result;
            
            if (needsTwoPhase) {
                twoPhasesSolver = new SimplexTwoPhases();
                twoPhasesSolver.setOriginalObjective(node.objective);
                
                Fraction[][] phase1Result = twoPhasesSolver.phaseOneProcess(
                    node.constraints, node.rhs, node.constraintTypes);
                Fraction[][] finalSolution = twoPhasesSolver.phaseTwoProcess(phase1Result);
                
                result = buildNodeResult(finalSolution, twoPhasesSolver.getVariableNames());
            } else {
                simplexSolver = new SimplexSimple();
                
                // Combine constraints with objective for simple method
                double[][] constraintsWithObj = combineConstraintsWithObjective(
                    node.constraints, node.objective);
                double[] rhsWithObj = combineRhsWithObjective(node.rhs);
                String[] constraintTypesWithObj = combineConstraintTypesWithObjective(
                    node.constraintTypes);
                
                Fraction[][] standardized = simplexSolver.standardize_then_fuse_rightSide(
                    constraintsWithObj, constraintTypesWithObj, rhsWithObj);
                
                Fraction[][] solution = simplexSolver.processSimple(
                    standardized, true, false, false, new ArrayList<>());
                
                result = buildNodeResult(solution, simplexSolver.getVariableNames());
            }
            
            if (result.containsKey("variables")) {
                Map<String, Double> variables = (Map<String, Double>) result.get("variables");
                double objectiveValue = (Double) result.get("objective");
                
                node.solution = variables;
                node.upperBound = objectiveValue;
                node.feasible = true;
            } else {
                node.feasible = false;
                node.upperBound = Double.NEGATIVE_INFINITY;
            }
            
            return result;
            
        } catch (Exception e) {
            node.feasible = false;
            node.upperBound = Double.NEGATIVE_INFINITY;
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Node infeasible: " + e.getMessage());
            return errorResult;
        }
    }
    
    private BranchNode createChildNode(BranchNode parent, String variable, String operator, int value) {
        //*--Copy parent constraints
        List<double[]> constraintsList = new ArrayList<>();
        for (double[] constraint : parent.constraints) {
            constraintsList.add(constraint.clone());
        }
        
        List<Double> rhsList = new ArrayList<>();
        for (double rhs : parent.rhs) {
            rhsList.add(rhs);
        }
        
        List<String> constraintTypesList = new ArrayList<>();
        for (String type : parent.constraintTypes) {
            constraintTypesList.add(type);
        }
        
        //*--Add new branching constraint
        double[] newConstraint = new double[parent.objective.length];
        Arrays.fill(newConstraint, 0.0);
        
        //*--Find variable index
        int varIndex = -1;
        for (int i = 0; i < parent.objective.length; i++) {
            if (variable.equals("x" + (i + 1))) {
                varIndex = i;
                break;
            }
        }
        
        if (varIndex != -1) {
            newConstraint[varIndex] = 1.0;
            constraintsList.add(newConstraint);
            rhsList.add((double) value);
            constraintTypesList.add(operator);
        }
        
        //*--Create child node
        BranchNode child = new BranchNode(
            constraintsList.toArray(new double[0][]),
            rhsList.stream().mapToDouble(Double::doubleValue).toArray(),
            constraintTypesList.toArray(new String[0]),
            new ArrayList<>(parent.branchingConstraints),
            parent.objective.clone(),
            parent.branchPath, // Pass parent's path
            parent.nodeId // Pass parent's ID
        );
        
        String branchDecision = variable + " " + operator + " " + value;
        child.branchingConstraints.add(branchDecision);
        child.branchPath.add(branchDecision); // Add to path
        
        return child;
    }
    
    private boolean isIntegerSolution(Map<String, Double> solution) {
        if (solution == null) return false;
        
        for (Map.Entry<String, Double> entry : solution.entrySet()) {
            String varName = entry.getKey();
            if (varName.startsWith("x")) { // Only check original variables
                double value = entry.getValue();
                if (Math.abs(value - Math.round(value)) > 1e-9) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private String selectBranchingVariable(Map<String, Double> solution) {
        if (solution == null) return null;
        
        String mostFractionalVar = null;
        double maxFractionalPart = 0;
        
        for (Map.Entry<String, Double> entry : solution.entrySet()) {
            String varName = entry.getKey();
            if (varName.startsWith("x")) { // Only consider original variables
                double value = entry.getValue();
                double fractionalPart = Math.abs(value - Math.round(value));
                if (fractionalPart > maxFractionalPart) {
                    maxFractionalPart = fractionalPart;
                    mostFractionalVar = varName;
                }
            }
        }
        
        return mostFractionalVar;
    }
    
    private double calculateObjectiveValue(Map<String, Double> solution, double[] objective) {
        double value = 0;
        for (int i = 0; i < objective.length; i++) {
            String varName = "x" + (i + 1);
            if (solution.containsKey(varName)) {
                value += objective[i] * solution.get(varName);
            }
        }
        return value;
    }
    
    private Map<String, Object> buildNodeResult(Fraction[][] tableau, List<String> varNames) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Double> variables = new HashMap<>();
        
        int numCols = tableau[0].length - 1;
        
        for (int j = 0; j < numCols; j++) {
            int pivotRow = -1;
            boolean isBasic = true;
            
            for (int i = 0; i < tableau.length - 1; i++) {
                if (tableau[i][j].equals(new Fraction(1))) {
                    if (pivotRow == -1) pivotRow = i;
                    else {
                        isBasic = false;
                        break;
                    }
                } else if (!tableau[i][j].isZero()) {
                    isBasic = false;
                    break;
                }
            }
            
            if (isBasic && pivotRow != -1) {
                String varName = varNames.get(j);
                variables.put(varName, tableau[pivotRow][numCols].toDouble());
            }
        }
        
        if (!variables.isEmpty()) {
            result.put("variables", variables);
            result.put("objective", Math.abs(tableau[tableau.length-1][numCols].toDouble()));
        }
        
        return result;
    }
    
    //?========Helper Functions
    private double[][] combineConstraintsWithObjective(double[][] constraints, double[] objective) {
        double[][] result = new double[constraints.length + 1][];
        System.arraycopy(constraints, 0, result, 0, constraints.length);
        result[constraints.length] = objective.clone();
        return result;
    }
    
    private double[] combineRhsWithObjective(double[] rhs) {
        double[] result = new double[rhs.length + 1];
        System.arraycopy(rhs, 0, result, 0, rhs.length);
        result[rhs.length] = 0;
        return result;
    }
    
    private String[] combineConstraintTypesWithObjective(String[] constraintTypes) {
        String[] result = new String[constraintTypes.length + 1];
        System.arraycopy(constraintTypes, 0, result, 0, constraintTypes.length);
        result[constraintTypes.length] = "==";
        return result;
    }
    
    public List<BranchAndBoundStep> getBranchingSteps() {
        return branchingSteps;
    }
}