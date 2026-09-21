$ErrorActionPreference = "Stop"

$BesuImage = "hyperledger/besu:26.6.0"
$ProjectDirectory = $PSScriptRoot
$NetworkDirectory = Join-Path $ProjectDirectory "network"
$GeneratedDirectory = Join-Path $NetworkDirectory "generated"

New-Item -ItemType Directory -Force -Path $NetworkDirectory | Out-Null

if (-not (Test-Path (Join-Path $GeneratedDirectory "genesis.json"))) {
    $dockerMount = $ProjectDirectory -replace '\\', '/'
    docker run --rm `
        -v "${dockerMount}:/work" `
        $BesuImage `
        operator generate-blockchain-config `
        --config-file=/work/qbftConfigFile.json `
        --to=/work/network/generated `
        --private-key-file-name=key

    if ($LASTEXITCODE -ne 0) {
        throw "Besu blockchain configuration generation failed."
    }
} else {
    Write-Host "Using the validator material already generated in network/generated."
}

Copy-Item -LiteralPath (Join-Path $GeneratedDirectory "genesis.json") `
    -Destination (Join-Path $NetworkDirectory "genesis.json")

$validatorDirectories = @(Get-ChildItem -Path (Join-Path $GeneratedDirectory "keys") -Directory | Sort-Object Name)
if ($validatorDirectories.Count -ne 4) {
    throw "Expected four generated validator directories, found $($validatorDirectories.Count)."
}

for ($index = 1; $index -le 4; $index++) {
    $nodeDirectory = Join-Path $NetworkDirectory "node$index"
    New-Item -ItemType Directory -Force -Path (Join-Path $nodeDirectory "data") | Out-Null
    Copy-Item -LiteralPath (Join-Path $validatorDirectories[$index - 1].FullName "key") `
        -Destination (Join-Path $nodeDirectory "key")
    Copy-Item -LiteralPath (Join-Path $validatorDirectories[$index - 1].FullName "key.pub") `
        -Destination (Join-Path $nodeDirectory "key.pub")
}

$publicKey = (Get-Content -Raw (Join-Path $NetworkDirectory "node1\key.pub")).Trim()
$publicKey = $publicKey -replace '^0x', '' -replace '^04', ''
$bootnode = "enode://${publicKey}@172.28.0.11:30303"

for ($index = 1; $index -le 4; $index++) {
    $config = @"
genesis-file="/config/genesis.json"
data-path="/data"
node-private-key-file="/config/key"
network-id=1337
p2p-host="0.0.0.0"
p2p-port=30303
bootnodes=["$bootnode"]
host-allowlist=["*"]
sync-mode="FULL"
data-storage-format="BONSAI"
tx-pool="sequenced"
min-gas-price=0
rpc-http-enabled=true
rpc-http-host="0.0.0.0"
rpc-http-port=8545
rpc-http-api=["ETH","NET","WEB3","QBFT"]
rpc-http-cors-origins=["*"]
"@
    $configPath = Join-Path $NetworkDirectory "node$index\config.toml"
    Set-Content -LiteralPath $configPath -Value $config -Encoding utf8NoBOM
}

Write-Host "Generated a four-validator QBFT network in $NetworkDirectory"
