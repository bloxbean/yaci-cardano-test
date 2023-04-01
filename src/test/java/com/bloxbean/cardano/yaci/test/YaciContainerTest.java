package com.bloxbean.cardano.yaci.test;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.helper.MintCreators;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.MultiAsset;
import com.bloxbean.cardano.client.transaction.spec.Policy;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.util.PolicyUtil;
import com.bloxbean.cardano.yaci.test.api.helper.YaciTestHelper;
import com.bloxbean.cardano.yaci.test.backend.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;

import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTx;
import static com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTxWithAdditionalSigners;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;
import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;
import static com.bloxbean.cardano.yaci.test.api.Assertions.assertMe;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class YaciContainerTest {
    private static String senderMnemonic = "flush together outer effort tenant photo waste distance rib grocery aunt broken weather arrow jungle debris finger flee casino doctor group echo baby near";
    private static Account account = new Account(Networks.testnet(), senderMnemonic);
    private static UtxoSupplier utxoSupplier;
    private static ProtocolParamsSupplier protocolParamSupplier;
    private static TransactionService transactionService;

    @Container
    private YaciCardanoContainer cardanoContainer = new YaciCardanoContainer();
    private YaciTestHelper testHelper = cardanoContainer.getTestHelper();

    @BeforeEach
    void setup() {
        if (!cardanoContainer.isRunning()) {
            cardanoContainer
                    .withInitialFunding(new Funding(account.baseAddress(), 20000))
                    .withLogConsumer(outputFrame -> log.info(outputFrame.getUtf8String()))
                    .start();

            utxoSupplier = cardanoContainer.getUtxoSupplier();
            protocolParamSupplier = cardanoContainer.getProtocolParamsSupplier();
            transactionService = cardanoContainer.getTransactionService();
        }
    }

    @Test
    void transfer_lovelace() throws Exception {
        String senderAddress = account.baseAddress();
        log.info("Sender address : " + senderAddress);

        String receiverAddress = new Account(Networks.testnet()).baseAddress();
        Output output = Output.builder()
                .address(receiverAddress)
                .assetName(LOVELACE)
                .qty(adaToLovelace(2.1))
                .build();

        TxBuilder txBuilder = output.outputBuilder()
                .buildInputs(createFromSender(account.baseAddress(), senderAddress))
                .andThen(balanceTx(senderAddress, 1));

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamSupplier)
                .buildAndSign(txBuilder, signerFrom(account));

        Result<String> result = transactionService.submitTransaction(signedTransaction.serialize());
        testHelper.waitForTransactionHash(result);

        assertMe(cardanoContainer).hasLovelaceBalance(receiverAddress, adaToLovelace(2.1));
        assertMe(cardanoContainer).hasLovelaceBalance(receiverAddress, 2100000);
        assertMe(cardanoContainer).utxos(receiverAddress).hasSize(1);
        assertMe(cardanoContainer).utxos(receiverAddress).hasLovelaceBalance(adaToLovelace(2.1));
    }

    @Test
    void mint_transfer_assets() throws Exception {
        String senderAddress = account.baseAddress();
        log.info("Sender address : " + senderAddress);

        String receiverAddress = new Account(Networks.testnet()).baseAddress();
        Policy policy = PolicyUtil.createMultiSigScriptAllPolicy("policy", 1);
        MultiAsset multiAsset = MultiAsset
                .builder()
                .policyId(policy.getPolicyId())
                .assets(List.of(
                        new Asset("abc", BigInteger.valueOf(1000)),
                        new Asset("xyz", BigInteger.valueOf(2000))))
                .build();

        Output output1 = Output.builder()
                .address(receiverAddress)
                .assetName(LOVELACE)
                .qty(adaToLovelace(2.1))
                .build();

        Output output2 = Output.builder()
                .address(receiverAddress)
                .policyId(policy.getPolicyId())
                .assetName("abc")
                .qty(BigInteger.valueOf(1000))
                .build();

        Output output3 = Output.builder()
                .address(receiverAddress)
                .policyId(policy.getPolicyId())
                .assetName("xyz")
                .qty(BigInteger.valueOf(2000))
                .build();

        TxBuilder txBuilder = output1.outputBuilder()
                .and(output2.mintOutputBuilder())
                .and(output3.mintOutputBuilder())
                .buildInputs(createFromSender(senderAddress, senderAddress))
                .andThen(MintCreators.mintCreator(policy.getPolicyScript(), multiAsset))
                .andThen(balanceTxWithAdditionalSigners(senderAddress, 1));

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamSupplier)
                .buildAndSign(txBuilder, signerFrom(account)
                        .andThen(signerFrom(policy.getPolicyKeys().get(0))));

        Result<String> result = transactionService.submitTransaction(signedTransaction.serialize());
        testHelper.waitForTransactionHash(result);

        assertMe(cardanoContainer).hasAssetBalance(receiverAddress, policy.getPolicyId(), "abc", BigInteger.valueOf(1000));
        assertMe(cardanoContainer).hasAssetBalance(receiverAddress, policy.getPolicyId(), "abc", 1000);
        assertMe(cardanoContainer).hasAssetBalance(receiverAddress, policy.getPolicyId(), "xyz", BigInteger.valueOf(2000));
        assertMe(cardanoContainer).hasLovelaceBalance(receiverAddress, adaToLovelace(2.1));
        assertMe(cardanoContainer).hasAssetBalance(receiverAddress, policy.getPolicyId(), "aaa", BigInteger.valueOf(0)); //non-exists
        assertMe(cardanoContainer).utxos(receiverAddress)
                .hasAssetBalance(policy.getPolicyId(), "abc", balance -> balance.compareTo(BigInteger.valueOf(1000)) == 0);

        //Predicate
        assertMe(cardanoContainer).utxos(senderAddress)
                .hasLovelaceBalance(balance -> balance.compareTo(adaToLovelace(20000)) < 0);

        //failed asserts
        Assertions.assertThrows(AssertionError.class, () -> {
            assertMe(cardanoContainer).hasAssetBalance(receiverAddress, policy.getPolicyId(), "abc", BigInteger.valueOf(300));
        });
        Assertions.assertThrows(AssertionError.class, () -> {
            assertMe(cardanoContainer).hasAssetBalance(receiverAddress, policy.getPolicyId(), "xyz", BigInteger.valueOf(500));
        });

        Assertions.assertThrows(AssertionError.class, () -> {
            assertMe(cardanoContainer).utxos(senderAddress)
                    .hasLovelaceBalance(balance -> balance.compareTo(adaToLovelace(10000)) < 0);
        });

    }

    @Test
    void transferTest() throws Exception {
        String senderAddress = account.baseAddress();
        log.info("Sender address : " + senderAddress);

        String receiverAddress = new Account(Networks.testnet()).baseAddress();
        Output output = Output.builder()
                .address(receiverAddress)
                .assetName(LOVELACE)
                .qty(adaToLovelace(2.1))
                .build();

        TxBuilder txBuilder = output.outputBuilder()
                .buildInputs(createFromSender(account.baseAddress(), senderAddress))
                .andThen(balanceTx(senderAddress, 1));

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamSupplier)
                .buildAndSign(txBuilder, signerFrom(account));

        Result<String> result = transactionService.submitTransaction(signedTransaction.serialize());
        testHelper.waitForTransactionHash(result);

        System.out.println(result);
        assertThat(testHelper.lovelaceBalance(receiverAddress).get()).isEqualTo(adaToLovelace(2.1));
        assertThat(testHelper.amounts(receiverAddress)).hasSize(1);
    }

    @AfterEach
    void tearDown() {
        cardanoContainer.stop();
    }
}
