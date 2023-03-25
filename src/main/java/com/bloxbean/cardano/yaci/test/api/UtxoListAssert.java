package com.bloxbean.cardano.yaci.test.api;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.plutus.impl.DefaultPlutusObjectConverter;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.PlutusScript;
import com.bloxbean.cardano.client.util.AssetUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import lombok.NonNull;
import org.assertj.core.api.ListAssert;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.yaci.test.api.helper.YaciTestHelper.amounts;

/**
 * Assertions for list of {@link Utxo}
 */
public class UtxoListAssert extends ListAssert<Utxo> {
    protected UtxoListAssert(List<Utxo> utxos) {
        super(utxos);
    }

    public static UtxoListAssert of(List<Utxo> utxos) {
        return new UtxoListAssert(utxos);
    }

    public static UtxoListAssert of(Utxo utxo) {
        return new UtxoListAssert(Collections.singletonList(utxo));
    }

    /**
     * Verifies that asset with given policy id and asset name exists
     *
     * @param policyId  Policy Id of the asset
     * @param assetName Asset name
     * @return this assertion object
     * @throws AssertionError - If utxo list doesn't contain multiasset
     */
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

    /**
     * Verifies that asset has expected balance
     *
     * @param policyId        policy id of the asset
     * @param assetName       asset name of the asset
     * @param expectedBalance balance to verify
     * @return this assertion object
     * @throws AssertionError - If asset balance doesn't match
     */
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

    /**
     * Verifies predicate evaluates to true for asset balance
     *
     * @param policyId  policy id of the asset
     * @param assetName asset name of the asset
     * @param predicate predicate to check against asset balance
     * @return this assertion object
     * @throws AssertionError - If predicate evaluates to false
     */
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

    /**
     * Verifies lovelace balance
     *
     * @param expectedBalance balance to verify
     * @return this assertion object
     * @throws AssertionError - If expected lovelace balance doesn't match with actual balance
     */
    public UtxoListAssert hasLovelaceBalance(@NonNull BigInteger expectedBalance) {
        List<Amount> amounts = amounts((List<Utxo>) actual);

        BigInteger actualBalance = getAssetBalance(amounts, LOVELACE);

        if (!expectedBalance.equals(actualBalance)) {
            failWithMessage("Expected lovelace balance to be <%s> but was <%s>", expectedBalance, actualBalance);
        }

        return this;
    }

    /**
     * Verifies predicate evaluate to true for lovelace balance
     *
     * @param predicate predicate to check lovelace balance
     * @return this assertion object
     * @throws AssertionError - If predicate evaluates to false
     */
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

    /**
     * Verifies if utxo list contains the given inline datum.
     *
     * @param datumObj Datum object. An object of type PlutusData or object of a class with PlutusData annotations
     *                 (@{@link com.bloxbean.cardano.client.plutus.annotation.PlutusField}, {@link com.bloxbean.cardano.client.plutus.annotation.Constr})
     * @return this assertion object
     * @throws AssertionError - If datum object not found in inlineDatum field
     */
    public UtxoListAssert containsInlineDatum(@NonNull Object datumObj) {
        isNotNull();

        String datumCborHex = new DefaultPlutusObjectConverter().toPlutusData(datumObj).serializeToHex();
        boolean found = actual.stream()
                .anyMatch(utxo -> datumCborHex.equals(utxo.getInlineDatum()));
        if (!found)
            failWithMessage("Expected but not found.\n InlineDatum : <%s>", datumCborHex);

        return this;
    }

    /**
     * Verifies if utxo list contains given datumHash
     *
     * @param datumHash Datum Hash
     * @return this assertion object
     * @throws AssertionError - If datumHash not found
     */
    public UtxoListAssert containsDatumHash(@NonNull String datumHash) {
        isNotNull();

        boolean found = actual.stream()
                .anyMatch(utxo -> datumHash.equals(utxo.getDataHash()));
        if (!found)
            failWithMessage("Expected but not found.\n Datum Hash : <%s>", datumHash);

        return this;
    }

    /**
     * Verifies if utxo list contains referenceScript
     * @param plutusScript PlutusScript to check
     * @return this assertion object
     * @throws AssertionError - If reference script not found
     */
    public UtxoListAssert containsReferenceScript(@NonNull PlutusScript plutusScript) {
        isNotNull();

        try {
            String scriptRefHex = HexUtil.encodeHexString(plutusScript.scriptRefBytes());

            boolean found = actual.stream()
                    .anyMatch(utxo -> scriptRefHex.equals(utxo.getReferenceScriptHash()));
            //TODO -- The reference script hash in utxo object is actually reference script body. Need to fix this

            if (!found)
                failWithMessage("Expected but not found.\n ReferenceScript : <%s>", scriptRefHex);

            return this;
        } catch (CborSerializationException e) {
            throw new RuntimeException(e);
        }
    }
}
