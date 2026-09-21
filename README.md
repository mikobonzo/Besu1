# Besu IOU Spring Client

Spring Boot REST client for the Hyperledger Besu QBFT IOU proof of concept.

## Runtime configuration

```powershell
$env:BESU_RPC_URL = "http://localhost:8545"
$env:BESU_CHAIN_ID = "1337"
$env:BESU_PRIVATE_KEY = "<FUNDED_DEVELOPMENT_PRIVATE_KEY>"
$env:BESU_CONTRACT_ADDRESS = "0x..."
```

`BESU_CONTRACT_BINARY` is needed only for the POC deployment endpoint. Use a
disposable development key locally and never commit a private key.

## Build and run

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

## Endpoints

- `GET /api/network/status`
- `POST /api/ious/deploy`
- `POST /api/ious`
- `GET /api/ious/{id}`
- `POST /api/ious/{id}/settle`

The contract integration assumes these signatures:

```solidity
issueIou(bytes32 id, address beneficiary, uint256 amount, bytes32 currency)
settleIou(bytes32 id)
getIou(bytes32 id) returns (address beneficiary, uint256 amount, bytes32 currency, uint8 status)
```

Update the `Function` definitions in `IouService` if the real ABI differs.
