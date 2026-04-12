# Simplex Solver

Simplex Solver is a lightweight web calculator I built to solve linear optimization problems with full step visibility.

It supports classic simplex flows, two-phase simplex when constraints require artificial variables, and Branch-and-Bound for integer linear programming.

The goal is to make LP/IP solving practical and visual: enter a model, solve it, and inspect how each tableau or branch decision was produced.

## Features

### What Simplex Solver can do today

- Solve maximization LP problems with standard <= constraints (Simplex)
- Automatically switch to Two-Phase Simplex when >= or = constraints are present
- Solve integer programming problems with Branch-and-Bound
- Show detailed iteration steps (tableau basis and rows)
- Separate and display Phase 1 and Phase 2 steps for two-phase problems
- Display branching history, node status, and final integer solution for ILP
- Provide both a browser UI and a JSON API endpoint

### Why this project matters

- It combines algorithmic learning and practical solving in the same tool
- It exposes intermediate computations, not just final values
- It provides a simple way to compare LP and ILP behavior on the same model

## Tech Stack

- Backend: Java 21 + Spring Boot 3
- Build tool: Maven (wrapper included)
- Frontend: Static HTML, vanilla JavaScript, custom CSS

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+ (optional if using wrapper)

### Local setup

```bash
# 1) Clone the repository
git clone "https://github.com/Snowlydial/UTL_SimplexAlgo"
cd UTL_SimplexAlgo

# 2) Run the application
# Windows
./mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

The application will be available at http://localhost:8080

## API

Base route: /api/simplex

### Solve endpoint

- Method: POST
- Route: /api/simplex/solve
- Content-Type: application/json

Request body:

```json
{
  "objective": [3, 5],
  "constraints": [[1, 0], [0, 2], [3, 2]],
  "rhs": [4, 12, 18],
  "constraintTypes": ["<=", "<=", "<="],
  "integerProgramming": false
}
```

Field notes:

- objective: coefficients of the objective function (maximize)
- constraints: constraint coefficient matrix
- rhs: right-hand-side constants
- constraintTypes: one of <=, >=, = for each constraint
- integerProgramming: true to use Branch-and-Bound

## Response format

### LP response (Simplex or Two-Phase)

```json
{
  "variables": {
    "x1": 2.0,
    "x2": 6.0
  },
  "objective": 36.0,
  "steps": []
}
```

For Two-Phase runs, step arrays are split:

```json
{
  "variables": {
    "x1": 2.0,
    "x2": 6.0
  },
  "objective": 36.0,
  "phase1Steps": [],
  "phase2Steps": []
}
```

### ILP response (Branch-and-Bound)

```json
{
  "variables": {
    "x1": 2.0,
    "x2": 5.0
  },
  "objective": 31.0,
  "branchingSteps": [],
  "solutionType": "INTEGER_OPTIMAL"
}
```

Error responses return:

```json
{
  "error": "...message..."
}
```

## Project Structure

- src/main/java/com/itu/controller: REST controller and API entry points
- src/main/java/com/itu/core: Solver implementations (Simplex, Two-Phase, Branch-and-Bound)
- src/main/java/com/itu/dto: Request/step DTOs
- src/main/resources/static: Web UI (index, JS logic, CSS)

## Notes

- The solver currently targets maximization problems.
- Branch-and-Bound checks integrality on decision variables (x1, x2, ...).
- The UI sends data directly to the REST endpoint and renders returned steps.
