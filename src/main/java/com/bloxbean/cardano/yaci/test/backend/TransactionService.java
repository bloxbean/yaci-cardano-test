package com.bloxbean.cardano.yaci.test.backend;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.model.EvaluationResult;
import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.client.backend.model.TxContentUtxo;
import com.bloxbean.cardano.yaci.test.backend.http.TransactionApi;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class TransactionService extends BaseService implements com.bloxbean.cardano.client.backend.api.TransactionService {
    private TransactionApi transactionApi;

    public TransactionService(String baseUrl, String projectId) {
        super(baseUrl, projectId);
        this.transactionApi = getFeign().target(TransactionApi.class, baseUrl);
    }

    @Override
    public Result<String> submitTransaction(byte[] cborData) throws ApiException {
        try {
            String response = transactionApi.submit(cborData);
            return Result.success(response).withValue(response).code(200);
        } catch (FeignException e) {
            log.debug("Error submit transaction: ", e);
            return Result.error(e.contentUTF8()).code(e.status());
        } catch (Exception e) {
            throw new ApiException("Error submit transaction", e);
        }
    }

    @Override
    public Result<TransactionContent> getTransaction(String txnHash) throws ApiException {
        try {
            TransactionContent txnContent = transactionApi.getTransaction(txnHash);
            return Result.success(String.valueOf(txnContent)).withValue(txnContent).code(200);
        } catch (FeignException e) {
            log.trace("Error getting transaction hash: " + txnHash, e);
            return Result.error(e.contentUTF8()).code(e.status());
        } catch (Exception e) {
            throw new ApiException("Error getting transaction hash: " + txnHash, e);
        }
    }

    @Override
    public Result<TxContentUtxo> getTransactionUtxos(String txnHash) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Result<List<EvaluationResult>> evaluateTx(byte[] cborData) {
        throw new UnsupportedOperationException("Not supported");
    }

}
