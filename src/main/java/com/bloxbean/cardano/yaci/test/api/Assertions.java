package com.bloxbean.cardano.yaci.test.api;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.test.YaciCardanoContainer;

import java.util.List;

public class Assertions {

    public static CardanoContainerAssert assertMe(YaciCardanoContainer cardanoContainer) {
        return new CardanoContainerAssert(cardanoContainer);
    }

    public static UtxoListAssert assertMe(List<Utxo> utxos) {
        return new UtxoListAssert(utxos);
    }
}
