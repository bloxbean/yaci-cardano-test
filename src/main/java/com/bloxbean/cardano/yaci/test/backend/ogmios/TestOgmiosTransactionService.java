package com.bloxbean.cardano.yaci.test.backend.ogmios;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.client.backend.ogmios.http.OgmiosTransactionService;
import com.bloxbean.cardano.yaci.test.backend.ogmios.http.MatchesApi;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class TestOgmiosTransactionService extends OgmiosTransactionService {
    private MatchesApi kupoMatchApi;

    public TestOgmiosTransactionService(String ogmiosUrl, String kupoUrl) {
        super(ogmiosUrl);
        this.kupoMatchApi = getFeign().target(MatchesApi.class, kupoUrl);
    }

    @Override
    public Result<TransactionContent> getTransaction(String txnHash) throws ApiException {
        log.debug("Getting transaction from Kupo : " + txnHash);
        List<KupoTxn> kupoTxnList = kupoMatchApi.getTransaction(txnHash);
        log.trace("Kupo Txn : " + kupoTxnList);

        if (kupoTxnList == null || kupoTxnList.isEmpty()) {
            return Result.error("Transaction not found in Kupo");
        } else {
            var transactionContent = TransactionContent.builder()
                    .index(kupoTxnList.get(0).getTransactionIndex())
                    .hash(kupoTxnList.get(0).getTransactionId())
                    .build();
            return Result.success("Transaction found in Kupo").withValue(transactionContent).code(200);
        }
    }

    protected Feign.Builder getFeign() {
        return Feign.builder()
                .decoder(new JacksonDecoder());
    }
}
