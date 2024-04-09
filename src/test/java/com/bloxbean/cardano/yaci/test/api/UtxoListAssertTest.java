package com.bloxbean.cardano.yaci.test.api;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.config.Configuration;
import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import com.bloxbean.cardano.client.plutus.spec.PlutusV2Script;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.yaci.test.api.Assertions.assertMe;

class UtxoListAssertTest {

    @Test
    void containsMultiAssets() {
        List<Utxo> utxos = testUtxos();
        assertMe(utxos).containsMultiAsset("a9a750f73f678495ccc869ba300c983814d3079367f0cf909afea8d6", "abc");
        assertMe(utxos).containsMultiAsset("a9a750f73f678495ccc869ba300c983814d3079367f0cf909afea8d6", "abd");
        assertMe(utxos).containsMultiAsset("96662071b76a743e44c2267e85f5fa86f9a01a1bea53be5dd812378f", "WETH");
        assertMe(utxos).containsMultiAsset("96662071b76a743e44c2267e85f5fa86f9a01a1bea53be5dd812378f", "test");
    }

    @Test
    void containsMultiAssets_notExists() {
        List<Utxo> utxos = testUtxos();
        org.junit.jupiter.api.Assertions.assertThrows(AssertionError.class, () -> {
            assertMe(utxos).containsMultiAsset("a9a750f73f678495ccc869ba300c983814d3079367f0cf909afea8d6", "efg");
        });
    }

    @Test
    void balance_success() {
        List<Utxo> utxos = testUtxos();
        assertMe(utxos).hasLovelaceBalance(adaToLovelace(107));
        assertMe(utxos).hasAssetBalance("a9a750f73f678495ccc869ba300c983814d3079367f0cf909afea8d6", "abc",
                BigInteger.valueOf(3000));
        assertMe(utxos).hasAssetBalance("a9a750f73f678495ccc869ba300c983814d3079367f0cf909afea8d6", "abd",
                BigInteger.valueOf(6000));
        assertMe(utxos).hasAssetBalance("96662071b76a743e44c2267e85f5fa86f9a01a1bea53be5dd812378f", "WETH",
                BigInteger.valueOf(100));
        assertMe(utxos).hasAssetBalance("96662071b76a743e44c2267e85f5fa86f9a01a1bea53be5dd812378f", "test",
                BigInteger.valueOf(900));
    }

    @Test
    void hasLovelaceBalance_wrong_lovelaceBalance() {
        List<Utxo> utxos = testUtxos();
        org.junit.jupiter.api.Assertions.assertThrows(AssertionError.class, () -> {
            assertMe(utxos).hasLovelaceBalance(adaToLovelace(4000));
        });
    }

    @Test
    void hasAssetBalance_wrong_assetBalance() {
        List<Utxo> utxos = testUtxos();
        org.junit.jupiter.api.Assertions.assertThrows(AssertionError.class, () -> {
            assertMe(utxos).hasAssetBalance("96662071b76a743e44c2267e85f5fa86f9a01a1bea53be5dd812378f", "WETH",
                    BigInteger.valueOf(400));
            assertMe(utxos).hasAssetBalance("96662071b76a743e44c2267e85f5fa86f9a01a1bea53be5dd812378f", "test",
                    BigInteger.valueOf(900));
        });
    }

    @Test
    void hasAssetBalance_asset_not_exists() {
        List<Utxo> utxos = testUtxos();
        org.junit.jupiter.api.Assertions.assertThrows(AssertionError.class, () -> {
            assertMe(utxos).hasAssetBalance("88662071b76a743e44c2267e85f5fa86f9a01a1bea53be5dd812378f", "WBTC",
                    BigInteger.valueOf(400));
        });
    }

    @Test
    void containsInlineDatum_not_exists() throws Exception {
        List<Utxo> utxos = testUtxosWithDatumsAndRefScriptHash();
        Datum1 datum1 = new Datum1(5, 20);

        org.junit.jupiter.api.Assertions.assertThrows(AssertionError.class, () -> {
            assertMe(utxos).containsInlineDatum(datum1);
        });
    }

    @Test
    void containsDatumHash() throws Exception {
        List<Utxo> utxos = testUtxosWithDatumsAndRefScriptHash();
        Datum1 datum1 = new Datum1(5, 10);

        assertMe(utxos).containsDatumHash(Configuration.INSTANCE.getPlutusObjectConverter().toPlutusData(datum1).getDatumHash());
    }

    @Test
    void containsDatumHash_not_exists() throws Exception {
        List<Utxo> utxos = testUtxosWithDatumsAndRefScriptHash();
        Datum1 datum1 = new Datum1(5, 20);

        org.junit.jupiter.api.Assertions.assertThrows(AssertionError.class, () -> {
            assertMe(utxos).containsDatumHash(Configuration.INSTANCE.getPlutusObjectConverter().toPlutusData(datum1).getDatumHash());
        });
    }

    @Test
    void containsReferenceScript() throws Exception {
        List<Utxo> utxos = testUtxosWithDatumsAndRefScriptHash();

        PlutusV2Script plutusScript = PlutusV2Script.builder()
                .type("PlutusScriptV2")
                .cborHex("49480100002221200101")
                .build();

        assertMe(utxos).containsReferenceScript(plutusScript);
    }

    @Test
    void containsReferenceScriptHash_not_exists() throws Exception {
        List<Utxo> utxos = testUtxosWithDatumsAndRefScriptHash();

        PlutusV2Script plutusScript = PlutusV2Script.builder()
                .type("PlutusScriptV2")
                .cborHex("4e4d01000033222220051200120011")
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(AssertionError.class, () -> {
            assertMe(utxos).containsReferenceScript(plutusScript);
        });
    }

    @NotNull
    private static List<Utxo> testUtxos() {
        Utxo utxo1 = Utxo.builder()
                .txHash("568f91e262553950230d77cd27e30dfbf7a1dcaef96c40329ab7737858a76fa8")
                .outputIndex(0)
                .amount(List.of(new Amount(LOVELACE, adaToLovelace(100))))
                .build();
        Utxo utxo2 = Utxo.builder()
                .txHash("ff8f7e4753b93cb380d5a30a3c08fe7b13d15043154d8ca037092fdfe5702bcb")
                .outputIndex(0)
                .amount(List.of(
                        new Amount(LOVELACE, adaToLovelace(2)),
                        new Amount("a9a750f73f678495ccc869ba300c983814d3079367f0cf909afea8d6616263", BigInteger.valueOf(3000)),
                        new Amount("a9a750f73f678495ccc869ba300c983814d3079367f0cf909afea8d6616264", BigInteger.valueOf(2000)),
                        new Amount("96662071b76a743e44c2267e85f5fa86f9a01a1bea53be5dd812378f57455448", BigInteger.valueOf(100))
                ))
                .build();

        Utxo utxo3 = Utxo.builder()
                .txHash("ff8f7e4753b93cb380d5a30a3c08fe7b13d15043154d8ca037092fdfe5702bcb")
                .outputIndex(0)
                .amount(List.of(
                        new Amount(LOVELACE, adaToLovelace(5)),
                        new Amount("a9a750f73f678495ccc869ba300c983814d3079367f0cf909afea8d6616264", BigInteger.valueOf(4000)),
                        new Amount("96662071b76a743e44c2267e85f5fa86f9a01a1bea53be5dd812378f74657374", BigInteger.valueOf(900))
                ))
                .build();

        List<Utxo> utxos = List.of(utxo1, utxo2, utxo3);
        return utxos;
    }

    @NotNull
    private static List<Utxo> testUtxosWithDatumsAndRefScriptHash() throws Exception {
        Utxo utxo1 = Utxo.builder()
                .txHash("568f91e262553950230d77cd27e30dfbf7a1dcaef96c40329ab7737858a76fa8")
                .outputIndex(0)
                .amount(List.of(new Amount(LOVELACE, adaToLovelace(100))))
                .build();
        Utxo utxo2 = Utxo.builder()
                .txHash("ff8f7e4753b93cb380d5a30a3c08fe7b13d15043154d8ca037092fdfe5702bcb")
                .outputIndex(0)
                .amount(List.of(
                        new Amount(LOVELACE, adaToLovelace(2)),
                        new Amount("a9a750f73f678495ccc869ba300c983814d3079367f0cf909afea8d6616263", BigInteger.valueOf(3000)),
                        new Amount("a9a750f73f678495ccc869ba300c983814d3079367f0cf909afea8d6616264", BigInteger.valueOf(2000)),
                        new Amount("96662071b76a743e44c2267e85f5fa86f9a01a1bea53be5dd812378f57455448", BigInteger.valueOf(100))
                ))
                .dataHash(Configuration.INSTANCE.getPlutusObjectConverter().toPlutusData(new Datum1(5, 10)).getDatumHash())
                .referenceScriptHash("3a888d65f16790950a72daee1f63aa05add6d268434107cfa5b67712")
                .build();

        Utxo utxo3 = Utxo.builder()
                .txHash("ff8f7e4753b93cb380d5a30a3c08fe7b13d15043154d8ca037092fdfe5702bcb")
                .outputIndex(0)
                .amount(List.of(
                        new Amount(LOVELACE, adaToLovelace(5)),
                        new Amount("a9a750f73f678495ccc869ba300c983814d3079367f0cf909afea8d6616264", BigInteger.valueOf(4000)),
                        new Amount("96662071b76a743e44c2267e85f5fa86f9a01a1bea53be5dd812378f74657374", BigInteger.valueOf(900))
                ))
                .inlineDatum(Configuration.INSTANCE.getPlutusObjectConverter().toPlutusData(new Datum1(5, 10)).serializeToHex())
                .build();

        Utxo utxo4 = Utxo.builder()
                .txHash("ee8f7e4753b93cb380d5a30a3c08fe7b13d15043154d8ca037092fdfe5702bcb")
                .outputIndex(0)
                .amount(List.of(
                        new Amount(LOVELACE, adaToLovelace(5))
                ))
                .inlineDatum(Configuration.INSTANCE.getPlutusObjectConverter().toPlutusData(new Datum2("John")).serializeToHex())
                .build();
        List<Utxo> utxos = List.of(utxo1, utxo2, utxo3, utxo4);
        return utxos;
    }

    @Data
    @AllArgsConstructor
    @Constr(alternative = 1)
    static class Datum1 {
        @PlutusField
        long a;

        @PlutusField
        long b;
    }

    @Data
    @AllArgsConstructor
    @Constr(alternative = 1)
    static class Datum2 {
        @PlutusField
        String name;
    }
}
