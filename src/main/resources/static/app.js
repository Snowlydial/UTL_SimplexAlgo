let numVariables = 0;
let numConstraints = 0;

//?--------STEP 1: Init the problem then show the form
function createObjectiveInputs() {
    const objInput_container = document.getElementById('objective-inputs');
    objInput_container.innerHTML = '';
    
    for(let i = 0; i < numVariables; i++) {
        objInput_container.innerHTML += `
            <label>x${i+1}: <input type="number" class="coeff" id="obj-x${i+1}"></label>
        `;
    }
}

function initProblem() {
    numVariables = parseInt(document.getElementById('numVars').value);
    document.getElementById('problem-form').style.display = 'block';
    createObjectiveInputs();
}

//?--------STEP 2: Add a new constraint input
function addConstraint() {
    const constraintInput_container = document.getElementById('constraint-inputs');
    const constraintId = `constraint-${numConstraints}`;
    
    // create an array then fill it with input fields, then create a select/option
    constraintInput_container.innerHTML += `
        <div class="constraint" id="${constraintId}">
            <button class="delete-btn" onclick="deleteConstraint('${constraintId}')">×</button>
            ${Array.from({length: numVariables}, (_, i) => 
                    `<input type="number" class="coeff" placeholder="x${i+1}">`)
                .join('')
            }
                <select class="constraint-type">
                    <option value="<=">≤</option>
                    <option value=">=">≥</option>
                    <option value="=">=</option>
                </select>
                <input type="number" class="rhs" placeholder="RHS">
        </div>
    `;
    numConstraints++;
}

function deleteConstraint(constraintId) {
    const constraint = document.getElementById(constraintId);
    if (constraint) {
        constraint.remove();
    }
}

//?--------STEP 3: Solve the problem
async function solve() {
    //*---Fetch the objective input
    const objective = Array.from(document.getElementsByClassName('coeff'))
        .filter(el => el.id.startsWith('obj-'))
        .map(el => parseFloat(el.value))
    ;

    //*---Fetch all constraint inputs
    const constraints = Array.from(document.getElementsByClassName('constraint'))
        .map(constraint => {
            return {
                coefficients: Array.from(constraint.getElementsByClassName('coeff')).map(input => parseFloat(input.value)), // get all input then convert str to num
                type: constraint.querySelector('.constraint-type').value, // querySelector to get the selected option
                rhs: parseFloat(constraint.querySelector('.rhs').value)
            };
        })
    ;

    //*---Check if integer programming is selected
    const isIntegerProgramming = document.getElementById('integer-programming') ? 
        document.getElementById('integer-programming').checked : false;
        console.log(`Value of IntegerProgramming: ${isIntegerProgramming}`);

    //*---Prepare request to JSON-ify and send to URL for a response
    const requestBody = {
        objective: objective,
        constraints: constraints.map(c => c.coefficients),
        rhs: constraints.map(c => c.rhs),
        constraintTypes: constraints.map(c => c.type),
        integerProgramming: isIntegerProgramming
    };

    try {
        //*---Send request and wait for response
        const response = await fetch('/api/simplex/solve', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(requestBody)
        });

        //*---Fetch response from the URL
        const result = await response.json();
        
        if (!response.ok) {
            const serverMessage = result.error || 'No additional error information';
            throw new Error(`HTTP ${response.status} - Server Message: ${serverMessage}`);
        }
        
        if (result.error) {
            displayError(result.error);
        } else {
            //*---Display the results based on solution type
            if (result.branchingSteps) {
                displayBranchAndBoundResults(result);
            } else {
                displayResults(result);
            }
        }
    } catch (error) {
        displayError(error.message);
    }
}

//?--------STEP 3.5: Display the tableau + final sol
function displayResults(result) {
    const resultsDiv = document.getElementById('results');
    let html = '<div class="solution-container">';
    
    //*---Case with no variables (error)
    if (!result.variables || Object.keys(result.variables).length === 0) {
        html += '<p class="error">No solution found. Problem may be infeasible or unbounded.</p>';
        resultsDiv.innerHTML = html;
        return;
    }

    //*---Display iterations
    html += '<h3>Solution Steps</h3>';
    html += '<div class="iterations-container">';
    if (result.phase1Steps && result.phase2Steps) { //*---Phase 1 & 2 Steps
        //*P1
        html += '<h3>Phase 1:</h3>';
        result.phase1Steps.forEach((step, index) => {
            html += buildStepHtml(step, index + 1);
        });

        

        //*P2
        html += '<h3>Phase 2:</h3>';
        result.phase2Steps.forEach((step, index) => {
            html += buildStepHtml(step, index + 1);
        });
    } else if (result.steps) { //*---Simple steps
        result.steps.forEach((step, index) => {
            html += buildStepHtml(step, index + 1);
        });
    }
    html += '</div>';

    //*---Display final solution
    html += '<h2>Final Solution</h2><ul class="variables-list">';
    for (const [variable, value] of Object.entries(result.variables)) {
        html += `
            <li>
                <span class="variable-name">${variable}</span> = ${value.toFixed(4)}
            </li>
        `;
    }
    html += `</ul>
        <div class="objective-value">
            | Objective Value | = ${Math.abs(result.objective).toFixed(4)}
        </div>`;
    resultsDiv.innerHTML = html;
}

function displayError(message) {
    const resultsDiv = document.getElementById('results');
    resultsDiv.innerHTML = `<div class="error"><h3>Error</h3>${message}</div>`;
}

function buildStepHtml(step, iterationNumber) {
    return `
        <div class="iteration-step">
            <h4>Iteration ${iterationNumber}</h4>
            <div class="basis-vars">Basis: ${step.basisVariables.join(', ')}</div>
            <table class="tableau-table">
                ${step.tableauRows.map(row => `
                    <tr>
                        ${row.map(cell => `<td>${cell}</td>`).join('')}
                    </tr>
                `).join('')}
            </table>
            <!--<div class="objective-value">Current Objective: ${step.objectiveValue}</div>--!>
        </div>
    `;
}

//?Notes about forEach(), map:
/* 
    * first param: current item
    * second param: index
    * third param(sometimes): the array itself 
    for(let i = 0; i < phase1Steps.length; i++) {
        const currentItem = phase1Steps[i]
        callback(currentItem, i, phase1Steps);
    }
*/

//?======= BRANCH AND BOUNF STUFF
function displayBranchAndBoundResults(result) {
    const resultsDiv = document.getElementById('results');
    let html = '<div class="solution-container">';
    
    //*---Case with no solution
    if (!result.variables || Object.keys(result.variables).length === 0) {
        html += '<p class="error">No integer solution found. Problem may be infeasible or unbounded.</p>';
        resultsDiv.innerHTML = html;
        return;
    }

    //*---Display Branch and Bound steps
    html += '<h2>Branch and Bound Solution Process</h2>';
    html += '<div class="branch-bound-container">';
    
    if (result.branchingSteps && result.branchingSteps.length > 0) {
        html += '<h3>Branching Steps:</h3>';
        html += '<div class="branching-steps">';
        
        result.branchingSteps.forEach((step, index) => {
            html += buildBranchingStepHtml(step, index + 1);
        });
        
        html += '</div>';
    }
    
    html += '</div>';

    //*---Display final integer solution
    html += '<h2>Final Integer Solution</h2><ul class="variables-list">';
    for (const [variable, value] of Object.entries(result.variables)) {
        // Only show original variables (x1, x2, etc.)
        if (variable.startsWith('x') && /^x\d+$/.test(variable)) {
            html += `
                <li>
                    <span class="variable-name">${variable}</span> = ${Math.round(value)}
                </li>
            `;
        }
    }
    html += `</ul>
        <div class="objective-value">
            | Integer Objective Value | = ${result.objective.toFixed(4)}
        </div>
        <div class="solution-type">
            Solution Type: ${result.solutionType || 'INTEGER_OPTIMAL'}
        </div>`;
    
    html += '</div>';
    resultsDiv.innerHTML = html;
}

//*--------Build HTML for branching steps
function buildBranchingStepHtml(step, stepNumber) {
    let html = `<div class="branching-step">`;
    
    if (step.nodeType === 'BRANCHING') {
        html += `
            <h4>Step ${stepNumber} - Node ${step.nodeId}</h4>
            <div class="step-info">
                <p><strong>Type:</strong> Branching Node</p>
                <p><strong>Current Objective:</strong> ${(step.objectiveValue || 0).toFixed(4)}</p>
                <p><strong>Branching Variable:</strong> ${step.branchVariable || 'N/A'} = ${(step.branchValue || 0).toFixed(4)}</p>
                <div class="branch-bounds">
                    <p>Left Child (${step.branchVariable || 'N/A'} ≤ ${Math.floor(step.branchValue || 0)}): 
                       ${formatBound(step.leftChildBound)}</p>
                    <p>Right Child (${step.branchVariable || 'N/A'} ≥ ${Math.floor(step.branchValue || 0) + 1}): 
                       ${formatBound(step.rightChildBound)}</p>
                </div>
                <p class="step-message">${step.message || 'No message'}</p>
            </div>
        `;
    } else if (step.nodeType === 'INTEGER_SOLUTION') {
        html += `
            <h4> Step ${stepNumber} - Node ${step.nodeId} - INTEGER SOLUTION FOUND!</h4>
            <div class="step-info integer-solution">
                <p><strong>Type:</strong> Integer Solution Found!</p>
                <p><strong>Objective Value:</strong> ${(step.objectiveValue || 0).toFixed(4)}</p>
                
                <!-- NEW: Display branch path -->
                ${step.branchPath && step.branchPath.length > 0 ? `
                <div class="branch-path">
                    <p><strong> Branch Path to Solution:</strong></p>
                    <div class="path-visualization">
                        <span class="path-root">Root</span>
                        ${step.branchPath.map(pathStep => 
                            `<span class="path-arrow">→</span><span class="path-step">${pathStep}</span>`
                        ).join('')}
                        <span class="path-arrow">→</span><span class="path-solution">✅ INTEGER SOLUTION</span>
                    </div>
                    <p class="path-text"><strong>Path:</strong> ${step.branchPath.join(' → ')}</p>
                </div>
                ` : '<p><strong>Path:</strong> Found at root (no branching needed)</p>'}
                
                <div class="integer-variables">
                    <p><strong>Integer Variables:</strong></p>
                    <ul>
        `;
        
        if (step.solution) {
            for (const [variable, value] of Object.entries(step.solution)) {
                if (variable.startsWith('x') && /^x\d+$/.test(variable)) {
                    html += `<li>${variable} = ${Math.round(value || 0)}</li>`;
                }
            }
        }
        
        html += `
                    </ul>
                </div>
                <p class="step-message">${step.message || 'No message'}</p>
            </div>
        `;
    } else if (step.nodeType === 'PRUNED') {
        html += `
            <h4>Step ${stepNumber} - Node ${step.nodeId}</h4>
            <div class="step-info pruned">
                <p><strong>Type:</strong> Pruned Node</p>
                <p><strong>Reason:</strong> Upper bound (${(step.objectiveValue || 0).toFixed(4)}) ≤ Best integer solution</p>
                <p class="step-message">${step.message || 'No message'}</p>
            </div>
        `;
    } else if (step.nodeType === 'INFEASIBLE') {
        html += `
            <h4>Step ${stepNumber} - Node ${step.nodeId}</h4>
            <div class="step-info infeasible">
                <p><strong>Type:</strong> Infeasible Node</p>
                <p><strong>Reason:</strong> Branching constraints created an impossible/contradictory system</p>
                <p class="step-message">${step.message || 'No message'}</p>
            </div>
        `;
    }
    
    html += `</div>`;
    return html;
}

// Helper function to format bounds (handles -999999 case)
function formatBound(bound) {
    if (bound === undefined || bound === null) {
        return "N/A";
    }
    if (bound <= -999999 || bound === Number.NEGATIVE_INFINITY) {
        return "INFEASIBLE ";
    }
    return bound.toFixed(4);
}