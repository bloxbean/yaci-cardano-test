package com.bloxbean.cardano.yaci.test.backend;

import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.test.backend.http.AddressesApi;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

public class UtxoService extends BaseService implements com.bloxbean.cardano.client.backend.api.UtxoService {

    private AddressesApi addressApi;

    public UtxoService(String baseUrl, String projectId) {
        super(baseUrl, projectId);
        this.addressApi = getRetrofit().create(AddressesApi.class);
    }

    @Override
    public Result<List<Utxo>> getUtxos(String address, int count, int page) throws ApiException {
        return getUtxos(address, count, page, OrderEnum.asc);
    }

    public Result<List<Utxo>> getUtxos(String address, int count, int page, OrderEnum order) throws ApiException {
        Call<List<Utxo>> utxosCall = addressApi.getUtxos(getProjectId(), address, count, page, order.toString());

        try {
            Response<List<Utxo>> response = utxosCall.execute();
            return processResponse(response);

        } catch (IOException e) {
            throw new ApiException("Error getting utxos", e);
        }
    }
}
