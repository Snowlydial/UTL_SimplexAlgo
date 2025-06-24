package com.itu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.itu.core.*;
import com.itu.dto.SimplexRequest;

@RestController
@RequestMapping("/api/simplex")
@CrossOrigin
public class SimplexController {

    @PostMapping("/solve")
    public ResponseEntity<Map<String, Object>> solve(@RequestBody SimplexRequest request) {
        // Check if this is an integer programming problem
        if (request.isIntegerProgramming()) {
            return solveBranchAndBound(request);
        }
        
        // Regular LP solving
        boolean needsTwoPhase = Arrays
            .stream(request.getConstraintTypes())
            .anyMatch(c -> c.equals(">=") || c.equals("="));
        if(needsTwoPhase) {
            return solveTwoPhase(request);
        } else {
            return solveSimple(request);
        }
    }

    //?-----Branch and Bound method:
    private ResponseEntity<Map<String, Object>> solveBranchAndBound(SimplexRequest request) {
        try {
            SimplexBranchAndBound bnbSolver = new SimplexBranchAndBound();
            
            Map<String, Object> result = bnbSolver.solve(
                request.getObjective(),
                request.getConstraints(),
                request.getRhs(),
                request.getConstraintTypes()
            );
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("error", "Branch-and-Bound failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //?-----Simple related method:
    private ResponseEntity<Map<String, Object>> solveSimple(SimplexRequest request) {
        try {
            SimplexSimple spsp = new SimplexSimple();
            
            Fraction[][] standardized = spsp.standardize_then_fuse_rightSide(
                request.getConstraintsWithObjective(),
                request.getConstraintTypesWithObjectiveDummy(),
                request.getRhsWithObjectiveDummy()
            );
            
            Fraction[][] solution = spsp.processSimple(standardized, true, false, false, spsp.getIterationSteps());
            
            return buildResponse(solution, spsp);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //?-----Two-phase related methods:
    private ResponseEntity<Map<String, Object>> solveTwoPhase(SimplexRequest request) {
        try {
            SimplexTwoPhases sptp = new SimplexTwoPhases();
            sptp.setOriginalObjective(request.getObjective());

            // Phase 1
            Fraction[][] phase1Result = sptp.phaseOneProcess(request.getConstraints(), request.getRhs(), request.getConstraintTypes());
            // Phase 2
            Fraction[][] finalSolution = sptp.phaseTwoProcess(phase1Result);

            return buildResponse(finalSolution, sptp);
        } catch (ArithmeticException e) {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //?-----Helper method: 
    //*--- Build the response which contains varNames, objValue, tableauSteps
    private ResponseEntity<Map<String, Object>> buildResponse(Fraction[][] solution, SimplexSimple _spsp) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("variables", extractVariables(solution, _spsp.getVariableNames()));
        response.put("objective", Math.abs(solution[solution.length-1][solution[0].length-1].toDouble()));
        if(_spsp instanceof SimplexTwoPhases) {
            SimplexTwoPhases _sptp = (SimplexTwoPhases)_spsp;
            response.put("phase1Steps", _sptp.getIterationStepsPhase1());
            response.put("phase2Steps", _sptp.getIterationStepsPhase2());
        } else {
            response.put("steps", _spsp.getIterationSteps());
        }
        return ResponseEntity.ok(response);
    }

    //*---Uses the variableNames list from simplexSimple algo
    private Map<String, Double> extractVariables(Fraction[][] tableau, List<String> varNames) {
        Map<String, Double> variables = new LinkedHashMap<>();
        int numCols = tableau[0].length - 1; // Exclude RHS column
        
        for (int j = 0; j < numCols; j++) {
            int pivotRow = -1;
            boolean isBasic = true;
            
            // Check if column is basic (exactly one '1' in column)
            for (int i = 0; i < tableau.length - 1; i++) {
                if (tableau[i][j].equals(new Fraction(1))) {
                    if (pivotRow == -1) pivotRow = i;
                    else {
                        isBasic = false;
                        break;
                    }
                } else if (!tableau[i][j].isZero()) {
                    isBasic = false;
                }
            }
            
            if (isBasic && pivotRow != -1) {
                String varName = varNames.get(j);
                variables.put(varName, tableau[pivotRow][numCols].toDouble());
            }
        }
        return variables;
    }
}