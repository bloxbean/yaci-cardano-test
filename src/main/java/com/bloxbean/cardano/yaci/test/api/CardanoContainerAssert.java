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

public class CardanoContainerAssert extends AbstractAssert<CardanoContainerAssert, YaciCardanoContainer> {
    private YaciTestHelper testHelper;
    protected CardanoContainerAssert(YaciCardanoContainer yaciCardanoContainer) {
        super(yaciCardanoContainer, CardanoContainerAssert.class);
        testHelper = actual.getTestHelper();
    }

    public CardanoContainerAssert hasLovelaceBalance(@NonNull String address, @NotNull BigInteger expectedBalance) {
        isNotNull();

        BigInteger actualBalance = testHelper.lovelaceBalance(address).orElse(BigInteger.ZERO);
        if (!expectedBalance.equals(actualBalance))
            failWithMessage("Expected lovelace balance to be <%s> but was <%s>", expectedBalance, actualBalance);

        return this;
    }

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

    public UtxoListAssert utxos(@NonNull String address) {
        isNotNull();

        List<Utxo> utxoList = testHelper.utxos(address);

        return new UtxoListAssert(utxoList);
    }
}
