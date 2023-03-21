package com.bloxbean.cardano.yaci.test.api;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.test.YaciCardanoContainer;

import java.util.List;

/**
 * Helper class to return assertion implementations
 */
public class Assertions {

    /**
     * Return {@link CardanoContainerAssert}
     * @param cardanoContainer
     * @return
     */
    public static CardanoContainerAssert assertMe(YaciCardanoContainer cardanoContainer) {
        return new CardanoContainerAssert(cardanoContainer);
    }

    public static UtxoListAssert assertMe(List<Utxo> utxos) {
        return new UtxoListAssert(utxos);
    }
}
