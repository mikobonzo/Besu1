package com.uf.banking.besu_iou.service;

import com.uf.banking.besu_iou.config.BesuProperties;
import com.uf.banking.besu_iou.dto.IouResponse;
import com.uf.banking.besu_iou.dto.IssueIouRequest;
import com.uf.banking.besu_iou.dto.TransactionResponse;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class IouService {
    private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";
    private final Web3j web3j;
    private final ContractGasProvider gasProvider;
    private final BesuProperties properties;

    public IouService(Web3j web3j, ContractGasProvider gasProvider, BesuProperties properties) {
        this.web3j = web3j;
        this.gasProvider = gasProvider;
        this.properties = properties;
    }

    public TransactionResponse deploy() throws Exception {
        String binary = requireValue(properties.contractBinary(), "BESU_CONTRACT_BINARY");
        EthSendTransaction sent = transactionManager().sendTransaction(
                gasProvider.getGasPrice(), gasProvider.getGasLimit(), null,
                Numeric.prependHexPrefix(Numeric.cleanHexPrefix(binary)), BigInteger.ZERO, true);
        return receiptResponse(null, waitForReceipt(requireTransactionHash(sent)));
    }

    public TransactionResponse issue(IssueIouRequest request) throws Exception {
        byte[] id = Hash.sha3(request.externalReference().getBytes(StandardCharsets.UTF_8));
        Function function = new Function("issueIou",
                List.of(new Bytes32(id), new Address(request.beneficiary()),
                        new Uint256(request.amountMinorUnits()), new Bytes32(toBytes32(request.currency()))),
                List.of());
        TransactionReceipt receipt = sendFunction(requireContractAddress(), function);
        return receiptResponse(Numeric.toHexString(id), receipt);
    }

    public TransactionResponse settle(String hexId) throws Exception {
        byte[] id = parseBytes32(hexId);
        Function function = new Function("settleIou", List.of(new Bytes32(id)), List.of());
        return receiptResponse(Numeric.toHexString(id), sendFunction(requireContractAddress(), function));
    }

    public IouResponse get(String hexId) throws IOException {
        byte[] id = parseBytes32(hexId);
        Function function = new Function("getIou", List.of(new Bytes32(id)), List.of(
                new TypeReference<Address>() {}, new TypeReference<Address>() {},
                new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {},
                new TypeReference<Uint8>() {}));
        EthCall call = web3j.ethCall(
                Transaction.createEthCallTransaction(ZERO_ADDRESS, requireContractAddress(),
                        FunctionEncoder.encode(function)),
                DefaultBlockParameterName.LATEST).send();
        if (call.hasError()) {
            throw new IllegalStateException("Besu eth_call failed: " + call.getError().getMessage());
        }
        List<Type> values = FunctionReturnDecoder.decode(call.getValue(), function.getOutputParameters());
        if (values.size() != 5) {
            throw new IllegalStateException("Unexpected getIou response; verify the deployed contract ABI");
        }
        String beneficiary = values.get(1).getValue().toString();
        BigInteger amount = (BigInteger) values.get(2).getValue();
        String currency = bytes32ToString((byte[]) values.get(3).getValue());
        int status = ((BigInteger) values.get(4).getValue()).intValue();
        return new IouResponse(Numeric.toHexString(id), beneficiary, amount,
                currency, statusName(status));
    }

    private TransactionReceipt sendFunction(String address, Function function) throws Exception {
        EthSendTransaction sent = transactionManager().sendTransaction(
                gasProvider.getGasPrice(), gasProvider.getGasLimit(), address,
                FunctionEncoder.encode(function), BigInteger.ZERO, false);
        return waitForReceipt(requireTransactionHash(sent));
    }

    private String requireTransactionHash(EthSendTransaction response) {
        if (response.hasError()) {
            throw new IllegalStateException("Besu rejected the transaction: " + response.getError().getMessage());
        }
        return response.getTransactionHash();
    }

    private TransactionReceipt waitForReceipt(String transactionHash) throws Exception {
        for (int attempt = 0; attempt < properties.receiptAttempts(); attempt++) {
            Optional<TransactionReceipt> receipt = web3j.ethGetTransactionReceipt(transactionHash)
                    .send().getTransactionReceipt();
            if (receipt.isPresent()) return receipt.get();
            Thread.sleep(properties.receiptSleepMillis());
        }
        throw new IllegalStateException("Timed out waiting for transaction receipt: " + transactionHash);
    }

    private TransactionResponse receiptResponse(String iouId, TransactionReceipt receipt) {
        return new TransactionResponse(iouId, receipt.getTransactionHash(),
                receipt.getBlockNumber().toString(), receipt.isStatusOK() ? "MINED" : "FAILED");
    }

    private String requireContractAddress() {
        return requireValue(properties.contractAddress(), "BESU_CONTRACT_ADDRESS");
    }

    private TransactionManager transactionManager() {
        String privateKey = requireValue(properties.privateKey(), "BESU_PRIVATE_KEY");
        return new RawTransactionManager(web3j, Credentials.create(privateKey), properties.chainId());
    }

    private static String requireValue(String value, String setting) {
        if (value == null || value.isBlank()) throw new IllegalStateException(setting + " is not configured");
        return value;
    }

    private static byte[] parseBytes32(String hexId) {
        byte[] id = Numeric.hexStringToByteArray(hexId);
        if (id.length != 32) {
            throw new IllegalArgumentException("IOU id must contain exactly 32 bytes (64 hexadecimal characters)");
        }
        return id;
    }

    private static byte[] toBytes32(String value) {
        byte[] raw = value.getBytes(StandardCharsets.US_ASCII);
        if (raw.length > 32) throw new IllegalArgumentException("Value exceeds 32 bytes");
        return Arrays.copyOf(raw, 32);
    }

    private static String bytes32ToString(byte[] value) {
        int length = 0;
        while (length < value.length && value[length] != 0) length++;
        return new String(value, 0, length, StandardCharsets.US_ASCII);
    }

    private static String statusName(int status) {
        return switch (status) {
            case 0 -> "NONE";
            case 1 -> "ISSUED";
            case 2 -> "SETTLED";
            default -> "UNKNOWN_" + status;
        };
    }
}
