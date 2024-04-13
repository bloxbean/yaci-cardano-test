package com.bloxbean.cardano.yaci.test.backend.ogmios.http;

import com.bloxbean.cardano.yaci.test.backend.ogmios.KupoTxn;
import feign.Param;
import feign.RequestLine;

import java.util.List;

public interface MatchesApi {

    @RequestLine("GET v1/matches?transaction_id={transaction_id}")
    List<KupoTxn> getTransaction(@Param("transaction_id") String transactionId);

}
