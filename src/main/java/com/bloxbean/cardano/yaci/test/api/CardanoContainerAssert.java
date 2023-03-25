package com.bloxbean.cardano.yaci.test.api;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.util.AssetUtil;
import com.bloxbean.cardano.yaci.test.YaciCardanoContainer;
import com.bloxbean.cardano.yaci.test.api.helper.YaciTestHelper;
import lombok.NonNull;
import org.assertj.core.api.AbstractAssert;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.List;

/**
 * Assertions for {@link YaciCardanoContainer}
 */
public class CardanoContainerAssert extends AbstractAssert<CardanoContainerAssert, YaciCardanoContainer> {
    private YaciTestHelper testHelper;

    protected CardanoContainerAssert(YaciCardanoContainer yaciCardanoContainer) {
        super(yaciCardanoContainer, CardanoContainerAssert.class);
        testHelper = actual.getTestHelper();
    }

    /**
     * Verifies if the address has expected lovelace balance
     *
     * @param address address to check
     * @param expectedBalance balance to verify
     * @return this assertion object
     * @throws AssertionError - If expected balance doesn't match with actual balance
     */
    public CardanoContainerAssert hasLovelaceBalance(@NonNull String address, @NotNull long expectedBalance) {
        return hasLovelaceBalance(address, BigInteger.valueOf(expectedBalance));
    }

    /**
     * Verifies if the address has expected lovelace balance
     *
     * @param address address to check
     * @param expectedBalance balance to verify
     * @return this assertion object
     * @throws AssertionError - If expected balance doesn't match with actual balance
     */
    public CardanoContainerAssert hasLovelaceBalance(@NonNull String address, @NotNull BigInteger expectedBalance) {
        isNotNull();

        BigInteger actualBalance = testHelper.lovelaceBalance(address).orElse(BigInteger.ZERO);
        if (!expectedBalance.equals(actualBalance))
            failWithMessage("Expected lovelace balance to be <%s> but was <%s>", expectedBalance, actualBalance);

        return this;
    }

    /**
     * Verifies if the address has expected balance for the asset with given policy and assetName.
     *
     * @param address address to check
     * @param policy policy id of the asset
     * @param asssetName asset name of the asset
     * @param expectedBalance balance to verify
     * @return this assertion object
     * @throws AssertionError - If expected balance doesn't match with actual balance
     */
    public CardanoContainerAssert hasAssetBalance(@NonNull String address, @NonNull String policy, @NotNull String asssetName,
                                                  @NotNull long expectedBalance) {
        return hasAssetBalance(address, policy, asssetName, BigInteger.valueOf(expectedBalance));
    }

    /**
     * Verifies if the address has expected balance for the asset with given policy and assetName.
     *
     * @param address address to check
     * @param policy policy id of the asset
     * @param asssetName asset name of the asset
     * @param expectedBalance balance to verify
     * @return this assertion object
     * @throws AssertionError - If expected balance doesn't match with actual balance
     */
    public CardanoContainerAssert hasAssetBalance(@NonNull String address, @NonNull String policy, @NotNull String asssetName,
                                                  @NotNull BigInteger expectedBalance) {
        isNotNull();

        BigInteger actualBalance = testHelper
                .assetBalance(address, AssetUtil.getUnit(policy, new Asset(asssetName, BigInteger.ZERO)))
                .orElse(BigInteger.ZERO);
        if (!expectedBalance.equals(actualBalance))
            failWithMessage("Expected asset balance to be <%s> but was <%s>", expectedBalance, actualBalance);

        return this;
    }

    /**
     * Returns {@link UtxoListAssert}
     * @param address address
     * @return UtxoListAsset
     */
    public UtxoListAssert utxos(@NonNull String address) {
        isNotNull();

        List<Utxo> utxoList = testHelper.utxos(address);

        return new UtxoListAssert(utxoList);
    }
}
