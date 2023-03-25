package com.bloxbean.cardano.yaci.test.backend.http;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.backend.model.EpochContent;
import feign.Param;
import feign.RequestLine;

public interface EpochApi {
    @RequestLine("GET epochs/latest")
    EpochContent getLatestEpoch();

    @RequestLine("GET epochs/{number}")
    EpochContent getEpochByNumber(@Param("number") Integer number);

    @RequestLine("GET epochs/{number}/parameters")
    ProtocolParams getProtocolParameters(@Param("number") Integer number);

}
