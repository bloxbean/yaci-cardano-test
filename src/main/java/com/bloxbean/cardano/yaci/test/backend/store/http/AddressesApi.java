package com.bloxbean.cardano.yaci.test.backend.store.http;

import com.bloxbean.cardano.client.api.model.Utxo;
import feign.Param;
import feign.RequestLine;

import java.util.List;

public interface AddressesApi {
    @RequestLine("GET addresses/{address}/utxos?count={count}&page={page}&order={order}")
    List<Utxo> getUtxos(@Param("address") String address,
                              @Param("count") int count, @Param("page") int page, @Param("order") String order);
}
