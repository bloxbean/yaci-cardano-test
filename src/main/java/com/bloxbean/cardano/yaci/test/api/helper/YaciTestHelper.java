package com.bloxbean.cardano.yaci.test.api.helper;

import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.test.YaciCardanoContainer;
import lombok.NonNull;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

/**
 * Provides test utility methods
 */
public class YaciTestHelper {
    private YaciCardanoContainer container;
    private TransactionHelper transactionHelper;

    public YaciTestHelper(YaciCardanoContainer yaciCardanoContainer) {
        this.container = yaciCardanoContainer;
    }

    public List<Utxo> utxos(String address) {
        UtxoSupplier utxoSupplier = container.getUtxoSupplier();
        List<Utxo> utxoList = utxoSupplier.getAll(address);
        if (utxoList == null)
            return Collections.EMPTY_LIST;
        else
            return utxoList;
    }

    public List<Amount> amounts(String address) {
        return amounts(utxos(address));
    }

    public Optional<BigInteger> lovelaceBalance(String address) {
        return assetBalance(address, LOVELACE);
    }

    public Optional<BigInteger> assetBalance(String address, String unit) {
        List<Amount> amounts = amounts(address);
        return assetBalance(unit, amounts);
    }

    public List<PlutusData> inlineDatums(String address) {
        return inlineDatums(utxos(address));
    }

    public List<String> referenceScriptHashes(String address) {
        return referenceScriptHashes(utxos(address));
    }

    public static List<Amount> amounts(@NonNull List<Utxo> utxoList) {
        Map<String, List<Amount>> amountMap = utxoList.stream()
                .flatMap(utxo -> utxo.getAmount().stream())
                .collect(Collectors.groupingBy(Amount::getUnit));

        return amountMap.entrySet()
                .stream()
                .map(entry -> entry.getValue().stream()
                        .map(amount -> amount.getQuantity())
                        .collect(Collectors.reducing((b1, b2) -> b1.add(b2)))
                        .map(quantity -> new Amount(entry.getKey(), quantity)))
                .map(amountOptional -> amountOptional.orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static Optional<BigInteger> assetBalance(String unit, List<Amount> amounts) {
        return amounts.stream().filter(amount -> unit.equals(amount.getUnit()))
                .findFirst()
                .map(amount -> amount.getQuantity());
    }

    public static List<PlutusData> inlineDatums(@NonNull List<Utxo> utxos) {
        return utxos
                .stream()
                .filter(utxo -> utxo.getInlineDatum() != null)
                .map(utxo -> utxo.getInlineDatum())
                .map(datum -> {
                    try {
                        return PlutusData.deserialize(HexUtil.decodeHexString(datum));
                    } catch (CborDeserializationException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public List<String> datumHashes(String address) {
        return datumHashes(utxos(address));
    }

    public static List<String> datumHashes(@NonNull List<Utxo> utxos) {
        return utxos
                .stream()
                .filter(utxo -> utxo.getDataHash() != null)
                .map(utxo -> utxo.getDataHash())
                .collect(Collectors.toList());
    }

    public static List<String> referenceScriptHashes(@NonNull List<Utxo> utxos) {
        return utxos
                .stream()
                .filter(utxo -> utxo.getReferenceScriptHash() != null)
                .map(utxo -> utxo.getReferenceScriptHash())
                .collect(Collectors.toList());
    }

    public TransactionHelper getTransactionHelper() {
        if (transactionHelper == null)
            this.transactionHelper = new TransactionHelper(container);
        return transactionHelper;
    }
}
