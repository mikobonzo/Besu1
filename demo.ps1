$ErrorActionPreference = "Stop"

$ProjectDirectory = $PSScriptRoot
$BesuDirectory = Join-Path $ProjectDirectory "besu-network"
$ContractDirectory = Join-Path $BesuDirectory "contracts"
$BuildDirectory = Join-Path $BesuDirectory "build"
$EnvironmentFile = Join-Path $ProjectDirectory ".env"

# Publicly known Hardhat account #0. It is funded only in this local genesis.
# Never use this key outside this disposable demonstration network.
$DevPrivateKey = "ac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"
$DevAddress = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"

function Wait-HttpOk([string]$Url, [int]$Attempts = 30) {
    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        try {
            return Invoke-RestMethod -Uri $Url -TimeoutSec 3
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "Timed out waiting for $Url"
}

function Write-AppEnvironment([string]$ContractAddress, [string]$ContractBinary) {
    $content = @"
SERVER_PORT=8080
BESU_RPC_URL=http://host.docker.internal:8545
BESU_CHAIN_ID=1337
BESU_PRIVATE_KEY=$DevPrivateKey
BESU_CONTRACT_ADDRESS=$ContractAddress
BESU_CONTRACT_BINARY=$ContractBinary
BESU_GAS_PRICE=1000000000
BESU_GAS_LIMIT=6000000
BESU_RECEIPT_ATTEMPTS=60
BESU_RECEIPT_SLEEP_MILLIS=500
"@
    Set-Content -LiteralPath $EnvironmentFile -Value $content -Encoding utf8NoBOM
}

Write-Host "`n[1/8] Starting the four-validator QBFT network..." -ForegroundColor Cyan
Push-Location $BesuDirectory
try {
    docker compose up -d
    if ($LASTEXITCODE -ne 0) { throw "Could not start the Besu network." }
} finally {
    Pop-Location
}

Write-Host "[2/8] Waiting for Besu JSON-RPC..." -ForegroundColor Cyan
$rpcRequest = @{ jsonrpc = "2.0"; method = "eth_chainId"; params = @(); id = 1 } | ConvertTo-Json -Compress
for ($attempt = 1; $attempt -le 30; $attempt++) {
    try {
        $chain = Invoke-RestMethod -Method Post -Uri "http://localhost:8545" `
            -ContentType "application/json" -Body $rpcRequest -TimeoutSec 3
        if ($chain.result -eq "0x539") { break }
    } catch { }
    if ($attempt -eq 30) { throw "Besu did not become ready on port 8545." }
    Start-Sleep -Seconds 2
}
Write-Host "      Besu chain ID: $($chain.result) (1337)" -ForegroundColor Green

Write-Host "[3/8] Compiling IouSettlement.sol..." -ForegroundColor Cyan
if (Test-Path $BuildDirectory) {
    Remove-Item -LiteralPath $BuildDirectory -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $BuildDirectory | Out-Null
$mountPath = $BesuDirectory -replace '\\', '/'
docker run --rm -v "${mountPath}:/sources" ethereum/solc:0.8.30 `
    --optimize --evm-version london --bin --abi --overwrite `
    -o /sources/build /sources/contracts/IouSettlement.sol
if ($LASTEXITCODE -ne 0) { throw "Solidity compilation failed." }
$binary = (Get-Content -Raw (Join-Path $BuildDirectory "IouSettlement.bin")).Trim()
if ([string]::IsNullOrWhiteSpace($binary)) { throw "Compiled contract bytecode is empty." }

Write-Host "[4/8] Starting Spring with deployment bytecode..." -ForegroundColor Cyan
Write-AppEnvironment -ContractAddress "" -ContractBinary $binary
Push-Location $ProjectDirectory
try {
    docker compose up -d --build --force-recreate
    if ($LASTEXITCODE -ne 0) { throw "Could not start the Spring application." }
} finally {
    Pop-Location
}
Wait-HttpOk "http://localhost:8080/actuator/health" | Out-Null

Write-Host "[5/8] Deploying the IOU smart contract..." -ForegroundColor Cyan
$deployment = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/ious/deploy"
$receiptRequest = @{
    jsonrpc = "2.0"
    method = "eth_getTransactionReceipt"
    params = @($deployment.transactionHash)
    id = 2
} | ConvertTo-Json -Depth 4 -Compress
$receipt = Invoke-RestMethod -Method Post -Uri "http://localhost:8545" `
    -ContentType "application/json" -Body $receiptRequest
$contractAddress = $receipt.result.contractAddress
if ([string]::IsNullOrWhiteSpace($contractAddress)) {
    throw "The deployment receipt did not contain a contract address."
}
Write-Host "      Contract: $contractAddress" -ForegroundColor Green

Write-Host "[6/8] Restarting Spring with the deployed contract address..." -ForegroundColor Cyan
Write-AppEnvironment -ContractAddress $contractAddress -ContractBinary ""
Push-Location $ProjectDirectory
try {
    docker compose up -d --force-recreate
    if ($LASTEXITCODE -ne 0) { throw "Could not recreate the Spring application." }
} finally {
    Pop-Location
}
Wait-HttpOk "http://localhost:8080/actuator/health" | Out-Null

Write-Host "[7/8] Issuing and reading an IOU..." -ForegroundColor Cyan
$reference = "DEMO-IOU-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
$issueBody = @{
    externalReference = $reference
    beneficiary = $DevAddress
    amountMinorUnits = 250000
    currency = "MYR"
} | ConvertTo-Json
$issued = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/ious" `
    -ContentType "application/json" -Body $issueBody
$before = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/ious/$($issued.iouId)"
if ($before.status -ne "ISSUED") { throw "Expected ISSUED, received $($before.status)." }
Write-Host "      IOU ID: $($issued.iouId)" -ForegroundColor Green
Write-Host "      Issue transaction: $($issued.transactionHash)" -ForegroundColor Green
Write-Host "      Status: $($before.status)" -ForegroundColor Green

Write-Host "[8/8] Settling the IOU and confirming final state..." -ForegroundColor Cyan
$settled = Invoke-RestMethod -Method Post `
    -Uri "http://localhost:8080/api/ious/$($issued.iouId)/settle"
$after = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/ious/$($issued.iouId)"
if ($after.status -ne "SETTLED") { throw "Expected SETTLED, received $($after.status)." }

Write-Host "`nPASS: Complete Spring + Besu QBFT IOU demonstration succeeded." -ForegroundColor Green
Write-Host "Reference:       $reference"
Write-Host "Amount:          MYR 2,500.00"
Write-Host "Contract:        $contractAddress"
Write-Host "Issue block:     $($issued.blockNumber)"
Write-Host "Settlement block:$($settled.blockNumber)"
Write-Host "Final status:    $($after.status)"
