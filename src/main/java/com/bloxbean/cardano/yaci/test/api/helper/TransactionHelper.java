package com.bloxbean.cardano.yaci.test.api.helper;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.api.util.AssetUtil;
import com.bloxbean.cardano.client.api.util.PolicyUtil;
import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.crypto.bip32.key.HdPublicKey;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.helper.MintCreators;
import com.bloxbean.cardano.client.function.helper.OutputBuilders;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusScript;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.MultiAsset;
import com.bloxbean.cardano.client.transaction.spec.Policy;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.TransactionOutput;
import com.bloxbean.cardano.client.transaction.spec.Value;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.test.YaciCardanoContainer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTx;
import static com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTxWithAdditionalSigners;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;
import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;

/**
 * Helper class to create transactions  for testing
 */
@Slf4j
class TransactionHelper {
    private final YaciCardanoContainer yaciCardanoContainer;
    private String sk = "58208b7dae4189261f1c8b4fcf2fd54f9e11bda7f8e00bc85cb864094409ace43daf";
    private SecretKey faucetSecretKey;
    private String faucetAddress;

    public TransactionHelper(YaciCardanoContainer yaciCardanoContainer) {
        this.yaciCardanoContainer = yaciCardanoContainer;
        this.faucetSecretKey = new SecretKey(sk);
        this.faucetAddress = getAddress(faucetSecretKey);
    }

    public String getFaucetAddress() {
        return faucetAddress;
    }

    /**
     * Transfer ADA from faucet address to given address
     * @param address receiver address
     * @param ada   amount in ADA
     * @return  transaction hash
     */
    public Optional<String> transferAda(String address, double ada) {
        return transferAda(address, BigDecimal.valueOf(ada));
    }

    /**
     * Transfer ADA from faucet address to given address
     * @param address receiver address
     * @param ada  amount in ADA
     * @return transaction hash
     */
    public Optional<String> transferAda(String address, BigDecimal ada) {
        Output output = Output.builder()
                .address(address)
                .assetName(LOVELACE)
                .qty(adaToLovelace(ada))
                .build();

        TxBuilder txBuilder = output.outputBuilder()
                .buildInputs(createFromSender(faucetAddress, faucetAddress))
                .andThen(balanceTx(faucetAddress));

        Transaction transaction = TxBuilderContext.init(yaciCardanoContainer.getUtxoSupplier(), yaciCardanoContainer.getProtocolParamsSupplier())
                .buildAndSign(txBuilder, signerFrom(faucetSecretKey));

        return submitTx(transaction);
    }

    /**
     * Mint a token using faucet account and send to faucet address
     * @param tokenName Token name
     * @param amount token amount
     * @return Policy object which contains policy details
     */
    public Optional<Policy> mintToken(String tokenName, long amount) {
        return mintToken(getFaucetAddress(), tokenName, BigInteger.valueOf(amount));
    }

    /**
     * Mint a token using faucet account and transfer the minted token to the receiver address
     * @param receiver receiver address
     * @param tokenName  token name
     * @param amount amount of token
     * @return Policy object which contains policy details
     */
    public Optional<Policy> mintToken(String receiver, String tokenName, long amount) {
        return mintToken(receiver, tokenName, BigInteger.valueOf(amount));
    }

    /**
     * Mint a token using faucet account and transfer the minted token to the receiver address
     * @param receiver receiver address
     * @param tokenName token name
     * @param amount amount of token
     * @return Policy object which contains policy details
     */
    public Optional<Policy> mintToken(String receiver, String tokenName, BigInteger amount) {
        try {
            Policy policy = PolicyUtil.createMultiSigScriptAllPolicy("TestPolicy", 1);

            MultiAsset multiAsset = MultiAsset.builder()
                    .policyId(policy.getPolicyId())
                    .assets(Arrays.asList(new Asset(tokenName, amount)))
                    .build();

            Output output = Output.builder()
                    .address(receiver)
                    .policyId(policy.getPolicyId())
                    .assetName(tokenName)
                    .qty(amount)
                    .build();

            TxBuilder txBuilder = output.mintOutputBuilder()
                    .buildInputs(createFromSender(faucetAddress, faucetAddress))
                    .andThen(MintCreators.mintCreator(policy.getPolicyScript(), multiAsset))
                    .andThen(balanceTxWithAdditionalSigners(faucetAddress, 1));

            Transaction transaction = TxBuilderContext.init(yaciCardanoContainer.getUtxoSupplier(), yaciCardanoContainer.getProtocolParamsSupplier())
                    .buildAndSign(txBuilder, signerFrom(faucetSecretKey)
                            .andThen(signerFrom(policy.getPolicyKeys().get(0))));

            submitTx(transaction);
            return Optional.of(policy);
        } catch (Exception e) {
            log.error("Error while minting token", e);
            return Optional.empty();
        }
    }

    /**
     * Lock fund in a script address from a sender account
     * @param senderAccount Sender account
     * @param receiverScript Receiving plutus script
     * @param value Value to lock
     * @param inlineDatum Datum in the output
     * @return Transaction hash
     */
    public Optional<String> lockFund(@NonNull Account senderAccount, @NonNull PlutusScript receiverScript,
                                     @NonNull Value value, PlutusData inlineDatum) {
        String scriptAddress = AddressProvider.getEntAddress(receiverScript, Networks.testnet()).toBech32();
        return lockFund(senderAccount, scriptAddress, value, inlineDatum);
    }

    /**
     * Lock fund in a script address from a sender account
     * @param senderAccount Sender account
     * @param receiverScript Receiving plutus script
     * @param amounts  Amounts to lock
     * @param inlineDatum Datum in the output
     * @return  Transaction hash
     */
    public Optional<String> lockFund(@NonNull Account senderAccount, @NonNull PlutusScript receiverScript,
                                     @NonNull List<Amount> amounts, PlutusData inlineDatum) {
        String scriptAddress = AddressProvider.getEntAddress(receiverScript, Networks.testnet()).toBech32();
        return lockFund(senderAccount, scriptAddress, amounts, inlineDatum);
    }

    /**
     * Lock fund in a script address from a sender account
     * @param senderAccount Sender account
     * @param receiverScriptAddress   Receiving script address
     * @param amounts  Amounts to lock
     * @param inlineDatum Datum in the output
     * @return Transaction hash
     */
    public Optional<String> lockFund(@NonNull Account senderAccount, @NonNull String receiverScriptAddress,
                                     @NonNull List<Amount> amounts, PlutusData inlineDatum) {
        Value value = getValue(amounts);
        return lockFund(senderAccount, receiverScriptAddress, value, inlineDatum);
    }

    /**
     * Lock fund in a script address from a sender account
     * @param senderAccount Sender account
     * @param receiverScript Receiving script address
     * @param value Value to lock
     * @param inlineDatum Datum in the output
     * @return Transaction hash
     */
    public Optional<String> lockFund(@NonNull Account senderAccount, @NonNull String receiverScript,
                                     @NonNull Value value, PlutusData inlineDatum) {
        String senderAddress = senderAccount.baseAddress();
        TransactionOutput txOutput = TransactionOutput.builder()
                .address(receiverScript)
                .value(value)
                .inlineDatum(inlineDatum)
                .build();

        TxBuilder txBuilder = OutputBuilders.createFromOutput(txOutput)
                .buildInputs(createFromSender(senderAddress, senderAddress))
                .andThen(balanceTx(senderAddress, 1));

        Transaction transaction = TxBuilderContext.init(yaciCardanoContainer.getUtxoSupplier(),
                        yaciCardanoContainer.getProtocolParamsSupplier())
                .buildAndSign(txBuilder, signerFrom(senderAccount));

        return submitTx(transaction);
    }

    /**
     * Lock fund in a script address from faucet account
     * @param receiverScript Receiving plutus script
     * @param value Value to lock
     * @param inlineDatum Datum in the output
     * @return Transaction hash
     */
    public Optional<String> lockFund(@NonNull PlutusScript receiverScript,
                                     @NonNull Value value, PlutusData inlineDatum) {
        String scriptAddress = AddressProvider.getEntAddress(receiverScript, Networks.testnet()).toBech32();
        return lockFund(scriptAddress, value, inlineDatum);
    }

    /**
     * Lock fund in a script address from faucet account
     * @param receiverScript Receiving plutus script
     * @param amounts Amounts to lock
     * @param inlineDatum Datum in the output
     * @return  Transaction hash
     */
    public Optional<String> lockFund(@NonNull PlutusScript receiverScript,
                                     @NonNull List<Amount> amounts, PlutusData inlineDatum) {
        String scriptAddress = AddressProvider.getEntAddress(receiverScript, Networks.testnet()).toBech32();
        return lockFund(scriptAddress, amounts, inlineDatum);
    }


    /**
     * Lock fund in a script address from faucet account
     * @param receiverScriptAddress Receiving script address
     * @param amounts Amounts to lock
     * @param inlineDatum Datum in the output
     * @return Transaction hash
     */
    public Optional<String> lockFund(@NonNull String receiverScriptAddress,
                                     @NonNull List<Amount> amounts, PlutusData inlineDatum) {
        Value value = getValue(amounts);
        return lockFund(receiverScriptAddress, value, inlineDatum);
    }

    /**
     * Lock fund in a script address from faucet account
     * @param receiverScript Receiving script address
     * @param value Value to lock
     * @param inlineDatum Datum in the output
     * @return Transaction hash
     */
    public Optional<String> lockFund(@NonNull String receiverScript,
                                     @NonNull Value value, PlutusData inlineDatum) {
        TransactionOutput txOutput = TransactionOutput.builder()
                .address(receiverScript)
                .value(value)
                .inlineDatum(inlineDatum)
                .build();

        TxBuilder txBuilder = OutputBuilders.createFromOutput(txOutput)
                .buildInputs(createFromSender(faucetAddress, faucetAddress))
                .andThen(balanceTx(faucetAddress, 1));

        Transaction transaction = TxBuilderContext.init(yaciCardanoContainer.getUtxoSupplier(),
                        yaciCardanoContainer.getProtocolParamsSupplier())
                .buildAndSign(txBuilder, signerFrom(faucetSecretKey));

        return submitTx(transaction);
    }

    /**
     * Create a transaction with a reference script in output
     * @param referenceScript reference script
     * @param ada amount of ada to send
     * @return Reference script output (utxo) from this transaction
     */
    public Optional<Utxo> createReferenceScriptTx(PlutusScript referenceScript, double ada) {
        Account account = new Account(Networks.testnet());
        String receiverAddress = account.baseAddress();
        Output output = Output.builder()
                .address(receiverAddress)
                .assetName(LOVELACE)
                .qty(adaToLovelace(ada))
                .scriptRef(referenceScript)
                .build();

        TxBuilder txBuilder = output.outputBuilder()
                .buildInputs(createFromSender(faucetAddress, faucetAddress))
                .andThen(balanceTx(faucetAddress));

        Transaction transaction = TxBuilderContext.init(yaciCardanoContainer.getUtxoSupplier(), yaciCardanoContainer.getProtocolParamsSupplier())
                .buildAndSign(txBuilder, signerFrom(faucetSecretKey));

        Optional<String> txHash = submitTx(transaction);
        if (txHash.isPresent()) {
            return yaciCardanoContainer.getUtxoSupplier()
                    .getAll(receiverAddress)
                    .stream()
                    .filter(utxo -> utxo.getTxHash().equals(txHash.get()) && utxo.getOutputIndex() == 0)
                    .findFirst();
        } else {
            return Optional.empty();
        }
    }

    @NotNull
    private static Value getValue(@NotNull List<Amount> amounts) {
        Value value = new Value();
        List<MultiAsset> multiAssets = new ArrayList<>();
        for (Amount amount : amounts) {
            if (LOVELACE.equals(amount.getUnit())) {
                value.setCoin(amount.getQuantity());
            } else {
                MultiAsset multiAsset = AssetUtil.getMultiAssetFromUnitAndAmount(amount.getUnit(), amount.getQuantity());
                multiAssets = MultiAsset.mergeMultiAssetLists(multiAssets, Arrays.asList(multiAsset));
            }
        }
        value.setMultiAssets(multiAssets);
        return value;
    }

    @NotNull
    public Optional<String> submitTx(Transaction transaction) {
        Result<String> result;
        try {
            result = yaciCardanoContainer.getTransactionService().submitTransaction(transaction.serialize());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (result.isSuccessful()) {
            log.info("Transaction submitted successfully");
            waitForTransactionHash(result);
            log.info("Transaction hash: " + result.getValue());
            return Optional.of(result.getValue());
        } else {
            log.info("Transaction submission failed");
            return Optional.empty();
        }
    }

    public void waitForTransactionHash(Result<String> result) {
        try {
            if (result.isSuccessful()) { //Wait for transaction to be mined
                int count = 0;
                while (count < 5) {
                    Result<TransactionContent> txnResult = yaciCardanoContainer.getTransactionService().getTransaction(result.getValue());
                    if (txnResult.isSuccessful()) {
                        System.out.println(JsonUtil.getPrettyJson(txnResult.getValue().getHash()));
                        break;
                    } else {
                        System.out.println("Waiting for transaction to be processed ....");
                    }

                    count++;
                    Thread.sleep(2000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getAddress(SecretKey secretKey) {
        try {
            VerificationKey vk = KeyGenUtil.getPublicKeyFromPrivateKey(secretKey);
            HdPublicKey hdPublicKey = new HdPublicKey();
            hdPublicKey.setKeyData(vk.getBytes());
            Address address = AddressProvider.getEntAddress(hdPublicKey, Networks.testnet());

            return address.toBech32();
        } catch (Exception e) {
            throw new IllegalStateException("Error while getting address from secret key", e);
        }
    }
}
