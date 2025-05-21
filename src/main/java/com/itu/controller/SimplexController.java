package com.itu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.itu.core.*;
import com.itu.dto.SimplexRequest;

@RestController // combines @Controller and @ResponseBody, this class handles HTTP requests and returns JSON responses
@RequestMapping("/api/simplex") // every redirection to /api/simplex/something goes here
@CrossOrigin // allows the frontend (from a different port) to access this API
public class SimplexController {

    @PostMapping("/solve") // every page redirecting to /api/simplex/solve call this function
    public ResponseEntity<Map<String, Object>> solve(@RequestBody SimplexRequest request) {
        boolean needsTwoPhase = Arrays
            .stream(request.getConstraintTypes())
            .anyMatch(c -> c.equals(">=") || c.equals("="));
        if(needsTwoPhase) {
            return solveTwoPhase(request);
        } else {
            return solveSimple(request);
        }
    }

    //?-----Simple related method:
    private ResponseEntity<Map<String, Object>> solveSimple(SimplexRequest request) {
        SimplexSimple spsp = new SimplexSimple();
        
        Fraction[][] standardized = spsp.standardize_then_fuse_rightSide(
            request.getConstraintsWithObjective(),
            request.getConstraintTypesWithObjectiveDummy(),
            request.getRhsWithObjectiveDummy()
        );
        
        Fraction[][] solution = spsp.processSimple(standardized, true, false, false, spsp.getIterationSteps());
        
        return buildResponse(solution, spsp);
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