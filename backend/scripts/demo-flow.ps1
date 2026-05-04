# AgentGuard Demo Flow Script
# Usage: .\demo-flow.ps1

$baseUrl = "http://localhost:8080"
$projectName = "AgentGuard"
$projectPath = "D:/project/copilot/AgentGuard"

function Invoke-Api {
    param([string]$Method, [string]$Path, [string]$Body)
    $uri = "$baseUrl$Path"
    try {
        if ($Body) {
            $result = Invoke-RestMethod -Method $Method -Uri $uri -ContentType "application/json" -Body $Body
        } else {
            $result = Invoke-RestMethod -Method $Method -Uri $uri
        }
        Write-Host "  [OK] $uri" -ForegroundColor Green
        return $result
    } catch {
        Write-Host "  [FAIL] $uri - $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " AgentGuard V1 Demo Flow" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Health Check
Write-Host "`n[1/10] Health Check" -ForegroundColor Yellow
Invoke-Api -Method GET -Path "/api/health"

# 2. Project Scan
Write-Host "`n[2/10] Project Scan" -ForegroundColor Yellow
$scanBody = @{ projectName = $projectName; projectPath = $projectPath } | ConvertTo-Json
$scan = Invoke-Api -Method POST -Path "/api/projects/scan" -Body $scanBody
$projectId = $scan.data.projectId
Write-Host "  Project ID: $projectId"

# 3. Generate CODEX Rule
Write-Host "`n[3/10] Generate CODEX Rule" -ForegroundColor Yellow
$ruleBody = @{ projectId = $projectId; agentType = "CODEX" } | ConvertTo-Json
$rule = Invoke-Api -Method POST -Path "/api/agent-rules/generate" -Body $ruleBody
$ruleId = $rule.data.id
Write-Host "  Rule ID: $ruleId"

# 4. Write CODEX Rule
Write-Host "`n[4/10] Write CODEX Rule to Project" -ForegroundColor Yellow
$writeBody = @{ overwrite = $true; backup = $true } | ConvertTo-Json
Invoke-Api -Method POST -Path "/api/agent-rules/$ruleId/write" -Body $writeBody

# 5. Permission Assessment
Write-Host "`n[5/10] Permission Risk Assessment" -ForegroundColor Yellow
$permBody = @{
    projectId = $projectId
    agentType = "CODEX"
    taskType = "FRONTEND_REFACTOR"
    sandboxMode = "WORKSPACE_WRITE"
    approvalPolicy = "ON_REQUEST"
    networkAccess = $false
    allowDelete = $false
} | ConvertTo-Json
Invoke-Api -Method POST -Path "/api/risk/permission-assess" -Body $permBody

# 6. Command Audit
Write-Host "`n[6/10] Command Audit" -ForegroundColor Yellow
$cmdBody = @{
    projectId = $projectId
    commands = @("npm run build", "git status")
} | ConvertTo-Json
Invoke-Api -Method POST -Path "/api/commands/audit" -Body $cmdBody

# 7. Preflight Check
Write-Host "`n[7/10] Preflight Check" -ForegroundColor Yellow
$preBody = @{
    projectId = $projectId
    agentType = "CODEX"
    taskType = "FRONTEND_REFACTOR"
    sandboxMode = "WORKSPACE_WRITE"
    approvalPolicy = "ON_REQUEST"
    networkAccess = $false
    allowDelete = $false
    plannedCommands = @("npm run build", "git status")
} | ConvertTo-Json
Invoke-Api -Method POST -Path "/api/preflight/check" -Body $preBody

# 8. Git Diff Audit
Write-Host "`n[8/10] Git Diff Audit" -ForegroundColor Yellow
$gitBody = @{ projectId = $projectId } | ConvertTo-Json
Invoke-Api -Method POST -Path "/api/git-audit/diff" -Body $gitBody

# 9. Markdown Report Generate
Write-Host "`n[9/10] Markdown Report Generate" -ForegroundColor Yellow
$reportBody = @{
    projectId = $projectId
    includeScanResult = $true
    includeAgentRules = $true
    includeRiskReports = $true
    includeGitAudit = $true
    includePreflight = $true
} | ConvertTo-Json
Invoke-Api -Method POST -Path "/api/reports/markdown/generate" -Body $reportBody

# 10. Timeline Overview
Write-Host "`n[10/10] Timeline Overview" -ForegroundColor Yellow
Invoke-Api -Method GET -Path "/api/timeline/project/$projectId/overview"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Demo Flow Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
