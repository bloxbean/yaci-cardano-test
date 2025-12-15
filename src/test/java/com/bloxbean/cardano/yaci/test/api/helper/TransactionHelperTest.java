package com.bloxbean.cardano.yaci.test.api.helper;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.api.util.AssetUtil;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusV1Script;
import com.bloxbean.cardano.client.plutus.spec.PlutusV2Script;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.MultiAsset;
import com.bloxbean.cardano.client.transaction.spec.Policy;
import com.bloxbean.cardano.client.transaction.spec.Value;
import com.bloxbean.cardano.yaci.test.Funding;
import com.bloxbean.cardano.yaci.test.YaciCardanoContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.yaci.test.api.Assertions.assertMe;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionHelperTest {
    private static final String senderMnemonic = "flush together outer effort tenant photo waste distance rib grocery aunt broken weather arrow jungle debris finger flee casino doctor group echo baby near";
    private static final Account account = Account.createFromMnemonic(Networks.testnet(), senderMnemonic);

    private static final YaciCardanoContainer cardanoContainer = new YaciCardanoContainer();
    private static final YaciTestHelper testHelper = cardanoContainer.getTestHelper();

    @BeforeAll
    void setup() throws InterruptedException {
        if (!cardanoContainer.isRunning()) {
            cardanoContainer
                    .withInitialFunding(new Funding(account.baseAddress(), 20000))
                    .withLogConsumer(outputFrame -> log.info(outputFrame.getUtf8String()))
                    .start();
            UtxoSupplier utxoSupplier = cardanoContainer.getUtxoSupplier();
            log.info("Waiting for Yaci Store to be ready...");
            List<Utxo> utxos = utxoSupplier.getAll(account.baseAddress());
            int retries = 4;

            while (utxos.size() == 0 || retries < 0) {
                utxos = utxoSupplier.getAll(account.baseAddress());
                retries--;
                //noinspection BusyWait
                Thread.sleep(2000);
            }
        }
    }

    @Test
    @Order(1)
    void transferAda() {
        String receiver = "addr_test1qqp6l53xshenlc939a0q74rd09e7dva8lke0fvs3a7ld5f7y7h8vnukjnluapukncvvpxvjgg4nlwu34w3ywvzngw99sy2rpy3";
        testHelper.transferAda(receiver, 10000);

        assertMe(cardanoContainer).hasLovelaceBalance(receiver, adaToLovelace(10000));
    }

    @Test
    @Order(2)
    void mintToken() throws Exception {
        String receiver = "addr_test1qqp6l53xshenlc939a0q74rd09e7dva8lke0fvs3a7ld5f7y7h8vnukjnluapukncvvpxvjgg4nlwu34w3ywvzngw99sy2rpy3";
        Optional<Policy> optionalPolicy = testHelper.mintToken(receiver, "TestToken", 10000);
        Assertions.assertTrue(optionalPolicy.isPresent());
        Policy policy = optionalPolicy.get();

        assertMe(cardanoContainer).hasAssetBalance(receiver, policy.getPolicyId(), "TestToken", BigInteger.valueOf(10000));
    }

    @Test
    @Order(3)
    void lockFund_withAmounts() throws Exception {
        Account account = new Account(Networks.testnet());
        testHelper.transferAda(account.baseAddress(), 100);
        Optional<Policy> optionalPolicy = testHelper.mintToken(account.baseAddress(), "TestToken", 10000);

        Assertions.assertTrue(optionalPolicy.isPresent());
        Policy policy = optionalPolicy.get();
        PlutusV1Script plutusScript = PlutusV1Script.builder()
                .type("PlutusScriptV1")
                .cborHex("4e4d01000033222220051200120011")
                .build();
        String scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();
        String unit = AssetUtil.getUnit(policy.getPolicyId(), new Asset("TestToken", BigInteger.ZERO));
        List<Amount> amounts = Arrays.asList(
                new Amount(LOVELACE, adaToLovelace(4)), new Amount(unit, BigInteger.valueOf(50)));

        PlutusData plutusData = BigIntPlutusData.of(400);
        testHelper.lockFund(account, plutusScript, amounts, plutusData);

        assertMe(cardanoContainer).utxos(scriptAddress).hasLovelaceBalance(balance -> balance.compareTo(adaToLovelace(4)) >= 0);
        assertMe(cardanoContainer).utxos(scriptAddress).hasAssetBalance(policy.getPolicyId(), "TestToken", BigInteger.valueOf(50));
        assertMe(cardanoContainer).utxos(scriptAddress).containsInlineDatum(plutusData);
    }

    @Test
    @Order(4)
    void lockFund_withValue() throws Exception {
        Account account = new Account(Networks.testnet());
        PlutusV2Script plutusScript = PlutusV2Script.builder()
                .type("PlutusScriptV2")
                .cborHex("49480100002221200101")
                .build();
        String scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();

        testHelper.transferAda(account.baseAddress(), 100);

        Optional<Policy> optionalPolicy1 = testHelper.mintToken(account.baseAddress(), "TestToken1", 10000);
        Optional<Policy> optionalPolicy2 = testHelper.mintToken(account.baseAddress(), "TestToken2", 5000);
        Assertions.assertTrue(optionalPolicy1.isPresent());
        Assertions.assertTrue(optionalPolicy2.isPresent());

        Policy policy1 = optionalPolicy1.get();
        Policy policy2 = optionalPolicy2.get();

        Value value = Value.builder()
                .coin(adaToLovelace(4))
                .multiAssets(Arrays.asList(
                        new MultiAsset(policy1.getPolicyId(), List.of(
                                new Asset("TestToken1", BigInteger.valueOf(60))
                        )),
                        new MultiAsset(policy2.getPolicyId(), List.of(
                                new Asset("TestToken2", BigInteger.valueOf(50))
                        ))
                )).build();

        PlutusData plutusData = BigIntPlutusData.of(400);
        testHelper.lockFund(account, plutusScript, value, plutusData);

        assertMe(cardanoContainer).utxos(scriptAddress).hasLovelaceBalance(balance -> balance.compareTo(adaToLovelace(4)) >= 0);
        assertMe(cardanoContainer).utxos(scriptAddress).hasAssetBalance(policy1.getPolicyId(), "TestToken1", BigInteger.valueOf(60));
        assertMe(cardanoContainer).utxos(scriptAddress).hasAssetBalance(policy2.getPolicyId(), "TestToken2", BigInteger.valueOf(50));
        assertMe(cardanoContainer).utxos(scriptAddress).containsInlineDatum(plutusData);
    }

    @Test
    @Order(5)
    void lockFund_fromFaucetAcc_withAmounts() throws Exception {
        Optional<Policy> optionalPolicy = testHelper.mintToken("FaucetToken", 40000);
        Assertions.assertTrue(optionalPolicy.isPresent());
        Policy policy = optionalPolicy.get();
        PlutusV1Script plutusScript = PlutusV1Script.builder()
                .type("PlutusScriptV1")
                .cborHex("4e4d01000033222220051200120011")
                .build();
        String scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();
        String unit = AssetUtil.getUnit(policy.getPolicyId(), new Asset("FaucetToken", BigInteger.ZERO));
        List<Amount> amounts = Arrays.asList(
                new Amount(LOVELACE, adaToLovelace(4)), new Amount(unit, BigInteger.valueOf(50)));

        PlutusData plutusData = BigIntPlutusData.of(900);
        testHelper.lockFund(plutusScript, amounts, plutusData);

        assertMe(cardanoContainer).utxos(scriptAddress).hasLovelaceBalance(balance -> balance.compareTo(adaToLovelace(4)) >= 0);
        assertMe(cardanoContainer).utxos(scriptAddress).hasAssetBalance(policy.getPolicyId(), "FaucetToken", BigInteger.valueOf(50));
        assertMe(cardanoContainer).utxos(scriptAddress).containsInlineDatum(plutusData);
    }

    @Test
    @Order(6)
    void lockFund_fromFaucetAcc_withValue() throws Exception {
        PlutusV2Script plutusScript = PlutusV2Script.builder()
                .type("PlutusScriptV2")
                .cborHex("49480100002221200101")
                .build();
        String scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();

        Optional<Policy> optionalPolicy1 = testHelper.mintToken("FaucetToken1", 10000);
        Optional<Policy> optionalPolicy2 = testHelper.mintToken("FaucetToken2", 5000);

        Assertions.assertTrue(optionalPolicy1.isPresent());
        Assertions.assertTrue(optionalPolicy2.isPresent());

        Policy policy1 = optionalPolicy1.get();
        Policy policy2 = optionalPolicy2.get();

        Value value = Value.builder()
                .coin(adaToLovelace(4))
                .multiAssets(Arrays.asList(
                        new MultiAsset(policy1.getPolicyId(), List.of(
                                new Asset("FaucetToken1", BigInteger.valueOf(60))
                        )),
                        new MultiAsset(policy2.getPolicyId(), List.of(
                                new Asset("FaucetToken2", BigInteger.valueOf(50))
                        ))
                )).build();

        PlutusData plutusData = BigIntPlutusData.of(400);
        testHelper.lockFund(plutusScript, value, plutusData);

        assertMe(cardanoContainer).utxos(scriptAddress).hasLovelaceBalance(balance -> balance.compareTo(adaToLovelace(4)) >= 0);
        assertMe(cardanoContainer).utxos(scriptAddress).hasAssetBalance(policy1.getPolicyId(), "FaucetToken1", BigInteger.valueOf(60));
        assertMe(cardanoContainer).utxos(scriptAddress).hasAssetBalance(policy2.getPolicyId(), "FaucetToken2", BigInteger.valueOf(50));
        assertMe(cardanoContainer).utxos(scriptAddress).containsInlineDatum(plutusData);
    }

    @Test
    @Order(7)
    void createReferenceScript() {
        PlutusV2Script plutusScript = PlutusV2Script.builder()
                .type("PlutusScriptV2")
                .cborHex("49480100002221200101")
                .build();

        Optional<Utxo> utxo = testHelper.createReferenceScriptTx(plutusScript, 3);
        Assertions.assertTrue(utxo.isPresent());
        assertMe(utxo.get()).containsReferenceScript(plutusScript);
    }

    @AfterAll
    static void tearDown() {
        cardanoContainer.stop();
    }
}
