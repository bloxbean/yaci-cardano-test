package com.bloxbean.cardano.yaci.test.backend;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.model.EvaluationResult;
import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.client.backend.model.TxContentUtxo;
import com.bloxbean.cardano.yaci.test.backend.http.TransactionApi;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

public class TransactionService extends BaseService implements com.bloxbean.cardano.client.backend.api.TransactionService {
    private TransactionApi transactionApi;

    public TransactionService(String baseUrl, String projectId) {
        super(baseUrl, projectId);
        this.transactionApi = getRetrofit().create(TransactionApi.class);
    }

    @Override
    public Result<String> submitTransaction(byte[] cborData) throws ApiException {

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/cbor"), cborData);

        Call<String> txnCall = transactionApi.submit(getProjectId(), requestBody);
        try {
            Response<String> response = txnCall.execute();
            return processResponse(response);

        } catch (IOException e) {
            throw new ApiException("Error submit transaction", e);
        }
    }

    @Override
    public Result<TransactionContent> getTransaction(String txnHash) throws ApiException {
        Call<TransactionContent> txnCall = transactionApi.getTransaction(getProjectId(), txnHash);
        try {
            Response<TransactionContent> response = txnCall.execute();
            return processResponse(response);

        } catch (IOException e) {
            throw new ApiException("Error getting transaction for id : " + txnHash, e);
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
