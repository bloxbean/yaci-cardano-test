package com.bloxbean.cardano.yaci.test.backend.http;

import com.bloxbean.cardano.client.backend.model.TransactionContent;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface TransactionApi {
    @Headers("Content-Type: application/cbor")
    @RequestLine("POST tx/submit")
    String submit(byte[] signedTxn);

    @RequestLine("GET txs/{hash}")
    TransactionContent getTransaction(@Param("hash") String txnHash);
}
