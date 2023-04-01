package com.bloxbean.cardano.yaci.test.api.helper;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.transaction.spec.*;
import com.bloxbean.cardano.client.util.AssetUtil;
import com.bloxbean.cardano.yaci.test.Funding;
import com.bloxbean.cardano.yaci.test.YaciCardanoContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.yaci.test.api.Assertions.assertMe;

@Slf4j
class TransactionHelperTest {
    private static String senderMnemonic = "flush together outer effort tenant photo waste distance rib grocery aunt broken weather arrow jungle debris finger flee casino doctor group echo baby near";
    private static Account account = new Account(Networks.testnet(), senderMnemonic);

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
        }
    }

    @Test
    void transferAda() {
        String receiver = "addr_test1qqp6l53xshenlc939a0q74rd09e7dva8lke0fvs3a7ld5f7y7h8vnukjnluapukncvvpxvjgg4nlwu34w3ywvzngw99sy2rpy3";
        testHelper.transferAda(receiver, 10000);

        assertMe(cardanoContainer).hasLovelaceBalance(receiver, adaToLovelace(10000));
    }

    @Test
    void mintToken() throws Exception {
        String receiver = "addr_test1qqp6l53xshenlc939a0q74rd09e7dva8lke0fvs3a7ld5f7y7h8vnukjnluapukncvvpxvjgg4nlwu34w3ywvzngw99sy2rpy3";
        Policy policy = testHelper.mintToken(receiver, "TestToken", 10000).get();

        assertMe(cardanoContainer).hasAssetBalance(receiver, policy.getPolicyId(), "TestToken", BigInteger.valueOf(10000));
    }

    @Test
    void lockFund_withAmounts() throws Exception {
        Account account = new Account(Networks.testnet());
        testHelper.transferAda(account.baseAddress(), 100);
        Policy policy = testHelper.mintToken(account.baseAddress(), "TestToken", 10000).get();
        PlutusV1Script plutusScript = PlutusV1Script.builder()
                .type("PlutusScriptV1")
                .cborHex("4e4d01000033222220051200120011")
                .build();
        String scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();
        String unit = AssetUtil.getUnit(policy.getPolicyId(), new Asset("TestToken", BigInteger.ZERO));
        List<Amount> amounts = Arrays.asList(
                new Amount(LOVELACE, adaToLovelace(4)), new Amount(unit, BigInteger.valueOf(50)));

        PlutusData plutusData = BigIntPlutusData.of(400);
        Optional<String> txId = testHelper.lockFund(account, plutusScript, amounts, plutusData);

        assertMe(cardanoContainer).utxos(scriptAddress).hasLovelaceBalance(balance -> balance.compareTo(adaToLovelace(4)) >= 0);
        assertMe(cardanoContainer).utxos(scriptAddress).hasAssetBalance(policy.getPolicyId(), "TestToken", BigInteger.valueOf(50));
        assertMe(cardanoContainer).utxos(scriptAddress).containsInlineDatum(plutusData);
    }

    @Test
    void lockFund_withValue() throws Exception {
        Account account = new Account(Networks.testnet());
        PlutusV2Script plutusScript = PlutusV2Script.builder()
                .type("PlutusScriptV2")
                .cborHex("49480100002221200101")
                .build();
        String scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();

        testHelper.transferAda(account.baseAddress(), 100);
        Policy policy1 = testHelper.mintToken(account.baseAddress(), "TestToken1", 10000).get();
        Policy policy2 = testHelper.mintToken(account.baseAddress(), "TestToken2", 5000).get();

        Value value = Value.builder()
                .coin(adaToLovelace(4))
                .multiAssets(Arrays.asList(
                        new MultiAsset(policy1.getPolicyId(), Arrays.asList(
                                new Asset("TestToken1", BigInteger.valueOf(60))
                        )),
                        new MultiAsset(policy2.getPolicyId(), Arrays.asList(
                                new Asset("TestToken2", BigInteger.valueOf(50))
                        ))
                )).build();

        PlutusData plutusData = BigIntPlutusData.of(400);
        Optional<String> txId = testHelper.lockFund(account, plutusScript, value, plutusData);

        assertMe(cardanoContainer).utxos(scriptAddress).hasLovelaceBalance(balance -> balance.compareTo(adaToLovelace(4)) >= 0);
        assertMe(cardanoContainer).utxos(scriptAddress).hasAssetBalance(policy1.getPolicyId(), "TestToken1", BigInteger.valueOf(60));
        assertMe(cardanoContainer).utxos(scriptAddress).hasAssetBalance(policy2.getPolicyId(), "TestToken2", BigInteger.valueOf(50));
        assertMe(cardanoContainer).utxos(scriptAddress).containsInlineDatum(plutusData);
    }

    @Test
    void lockFund_fromFaucetAcc_withAmounts() throws Exception {
        Policy policy = testHelper.mintToken("FaucetToken", 40000).get();
        PlutusV1Script plutusScript = PlutusV1Script.builder()
                .type("PlutusScriptV1")
                .cborHex("4e4d01000033222220051200120011")
                .build();
        String scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();
        String unit = AssetUtil.getUnit(policy.getPolicyId(), new Asset("FaucetToken", BigInteger.ZERO));
        List<Amount> amounts = Arrays.asList(
                new Amount(LOVELACE, adaToLovelace(4)), new Amount(unit, BigInteger.valueOf(50)));

        PlutusData plutusData = BigIntPlutusData.of(900);
        Optional<String> txId = testHelper.lockFund(plutusScript, amounts, plutusData);

        assertMe(cardanoContainer).utxos(scriptAddress).hasLovelaceBalance(balance -> balance.compareTo(adaToLovelace(4)) >= 0);
        assertMe(cardanoContainer).utxos(scriptAddress).hasAssetBalance(policy.getPolicyId(), "FaucetToken", BigInteger.valueOf(50));
        assertMe(cardanoContainer).utxos(scriptAddress).containsInlineDatum(plutusData);
    }

    @Test
    void lockFund_fromFaucetAcc_withValue() throws Exception {
        PlutusV2Script plutusScript = PlutusV2Script.builder()
                .type("PlutusScriptV2")
                .cborHex("49480100002221200101")
                .build();
        String scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();

        Policy policy1 = testHelper.mintToken("FaucetToken1", 10000).get();
        Policy policy2 = testHelper.mintToken("FaucetToken2", 5000).get();

        Value value = Value.builder()
                .coin(adaToLovelace(4))
                .multiAssets(Arrays.asList(
                        new MultiAsset(policy1.getPolicyId(), Arrays.asList(
                                new Asset("FaucetToken1", BigInteger.valueOf(60))
                        )),
                        new MultiAsset(policy2.getPolicyId(), Arrays.asList(
                                new Asset("FaucetToken2", BigInteger.valueOf(50))
                        ))
                )).build();

        PlutusData plutusData = BigIntPlutusData.of(400);
        Optional<String> txId = testHelper.lockFund(plutusScript, value, plutusData);

        assertMe(cardanoContainer).utxos(scriptAddress).hasLovelaceBalance(balance -> balance.compareTo(adaToLovelace(4)) >= 0);
        assertMe(cardanoContainer).utxos(scriptAddress).hasAssetBalance(policy1.getPolicyId(), "FaucetToken1", BigInteger.valueOf(60));
        assertMe(cardanoContainer).utxos(scriptAddress).hasAssetBalance(policy2.getPolicyId(), "FaucetToken2", BigInteger.valueOf(50));
        assertMe(cardanoContainer).utxos(scriptAddress).containsInlineDatum(plutusData);
    }

    @Test
    void createReferenceScript() {
        PlutusV2Script plutusScript = PlutusV2Script.builder()
                .type("PlutusScriptV2")
                .cborHex("49480100002221200101")
                .build();

        Optional<Utxo> utxo = testHelper.createReferenceScriptTx(plutusScript, 3);
        assertMe(utxo.get()).containsReferenceScript(plutusScript);
    }

    @AfterEach
    void tearDown() {
        cardanoContainer.stop();
    }
}
