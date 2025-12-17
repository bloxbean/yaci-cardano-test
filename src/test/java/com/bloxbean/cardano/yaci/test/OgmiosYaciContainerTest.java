package com.bloxbean.cardano.yaci.test;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.api.util.PolicyUtil;
import com.bloxbean.cardano.client.backend.api.TransactionService;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.helper.MintCreators;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.MultiAsset;
import com.bloxbean.cardano.client.transaction.spec.Policy;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.yaci.test.api.helper.YaciTestHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTx;
import static com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTxWithAdditionalSigners;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;
import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;
import static com.bloxbean.cardano.yaci.test.api.Assertions.assertMe;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OgmiosYaciContainerTest {
    private static final String senderMnemonic = "flush together outer effort tenant photo waste distance rib grocery aunt broken weather arrow jungle debris finger flee casino doctor group echo baby near";
    private static final Account account = Account.createFromMnemonic(Networks.testnet(), senderMnemonic);
    private static UtxoSupplier utxoSupplier;
    private static ProtocolParamsSupplier protocolParamSupplier;
    private static TransactionService transactionService;

    private static final YaciCardanoContainer cardanoContainer = new YaciCardanoContainer();
    private static final YaciTestHelper testHelper = cardanoContainer.getTestHelper();

    @BeforeAll
    void setup() throws InterruptedException {
        if (!cardanoContainer.isRunning()) {
            cardanoContainer
                    .withApiMode(ApiMode.OGMIOS)
                    .withInitialFunding(new Funding(account.baseAddress(), 20000))
                    .withLogConsumer(outputFrame -> log.info(outputFrame.getUtf8String()))
                    .start();

            utxoSupplier = cardanoContainer.getUtxoSupplier();
            protocolParamSupplier = cardanoContainer.getProtocolParamsSupplier();
            transactionService = cardanoContainer.getTransactionService();
        }
        log.info("Waiting for Kupo and Ogmios to be ready...");
        List<Utxo> utxos = utxoSupplier.getAll(account.baseAddress());
        int retries = 4;

        while (utxos.size() == 0 || retries < 0) {
            utxos = utxoSupplier.getAll(account.baseAddress());
            retries--;
            //noinspection BusyWait
            Thread.sleep(2000);
        }
    }

    @Test
    @Order(1)
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
    @Order(2)
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
    @Order(3)
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

        Optional<BigInteger> lovelaceBalance = testHelper.lovelaceBalance(receiverAddress);

        assertThat(lovelaceBalance).isPresent();
        
        assertThat(lovelaceBalance.get()).isEqualTo(adaToLovelace(2.1));
        assertThat(testHelper.amounts(receiverAddress)).hasSize(1);
    }

    @AfterAll
    static void tearDown() {
        cardanoContainer.stop();
    }
}
