package com.bloxbean.cardano.yaci.test.api;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.util.AssetUtil;
import lombok.NonNull;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.PredicateAssert;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.yaci.test.api.helper.YaciTestHelper.amounts;

public class UtxoListAssert extends ListAssert<Utxo> {
    public UtxoListAssert(List<Utxo> utxos) {
        super(utxos);
    }

    public UtxoListAssert containsMultiAsset(@NonNull String policyId, @NonNull String assetName) {
        List<Amount> amounts = amounts((List<Utxo>) actual);
        if (amounts == null) amounts = Collections.emptyList();

        String unit = AssetUtil.getUnit(policyId, new Asset(assetName, BigInteger.ZERO));

        String units = amounts.stream().map(amount -> amount.getUnit()).collect(Collectors.joining("\n,"));
        if (amounts.size() == 0 ||
                !amounts.stream().anyMatch(amount -> amount.getUnit().equals(unit))) {
            failWithMessage("Expected asset with policy : policy: <%s>, asset: <%s>. \nBut not found. \nunits: <%s>", policyId, assetName, units);
        }

        return this;
    }

    public UtxoListAssert hasAssetBalance(@NonNull String policyId, @NonNull String assetName, @NonNull BigInteger expectedBalance) {
        isNotNull();

        List<Amount> amounts = amounts((List<Utxo>) actual);
        String unit = AssetUtil.getUnit(policyId, new Asset(assetName, BigInteger.ZERO));

        BigInteger actual = getAssetBalance(amounts, unit);

        if (!expectedBalance.equals(actual)) {
            failWithMessage("Expected asset balance to be <%s> but was <%s>", expectedBalance, actual);
        }

        return this;
    }

    public UtxoListAssert hasAssetBalance(@NonNull String policyId, @NonNull String assetName, @NonNull Predicate<BigInteger> predicate) {
        isNotNull();

        List<Amount> amounts = amounts((List<Utxo>) actual);
        String unit = AssetUtil.getUnit(policyId, new Asset(assetName, BigInteger.ZERO));

        BigInteger actualBalance = getAssetBalance(amounts, unit);

        if (!predicate.test(actualBalance)) {
            failWithMessage("Failed predicate. Actual balance <%s>", actualBalance);
        }

        return this;
    }

    public UtxoListAssert hasLovelaceBalance(@NonNull BigInteger expectedBalance) {
        List<Amount> amounts = amounts((List<Utxo>) actual);

        BigInteger actualBalance = getAssetBalance(amounts, LOVELACE);

        if (!expectedBalance.equals(actualBalance)) {
            failWithMessage("Expected lovelace balance to be <%s> but was <%s>", expectedBalance, actualBalance);
        }

        return this;
    }

    public UtxoListAssert hasLovelaceBalance(@NonNull Predicate<BigInteger> predicate) {
        List<Amount> amounts = amounts((List<Utxo>) actual);

        BigInteger actualBalance = getAssetBalance(amounts, LOVELACE);

        if (!predicate.test(actualBalance)) {
            failWithMessage("Failed predicate. Actual balance <%s>", actualBalance);
        }

        return this;
    }

    @NotNull
    private static BigInteger getAssetBalance(List<Amount> amounts, String unit) {
        BigInteger actual = amounts.stream().filter(amount -> amount.getUnit().equals(unit))
                .findFirst()
                .map(amount -> amount.getQuantity())
                .orElse(BigInteger.ZERO);
        return actual;
    }
}
