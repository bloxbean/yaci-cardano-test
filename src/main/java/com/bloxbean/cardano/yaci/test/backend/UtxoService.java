package com.bloxbean.cardano.yaci.test.backend;

import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.test.backend.http.AddressesApi;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class UtxoService extends BaseService implements com.bloxbean.cardano.client.backend.api.UtxoService {

    private AddressesApi addressApi;

    public UtxoService(String baseUrl, String projectId) {
        super(baseUrl, projectId);
        this.addressApi = getFeign().target(AddressesApi.class, baseUrl);
    }

    @Override
    public Result<List<Utxo>> getUtxos(String address, int count, int page) throws ApiException {
        return getUtxos(address, count, page, OrderEnum.asc);
    }

    public Result<List<Utxo>> getUtxos(String address, int count, int page, OrderEnum order) throws ApiException {
        try {
            List<Utxo> utxos = addressApi.getUtxos(address, count, page, order.toString());
            return Result.success(String.valueOf(utxos)).withValue(utxos).code(200);
        } catch (FeignException e) {
            log.debug("Error getting utxos: ", e);
            return Result.error(e.contentUTF8()).code(e.status());
        } catch (Exception e) {
            throw new ApiException("Error getting utxos", e);
        }
    }

    @Override
    public Result<List<Utxo>> getUtxos(String s, String s1, int i, int i1) throws ApiException {
        return null;
    }

    @Override
    public Result<List<Utxo>> getUtxos(String s, String s1, int i, int i1, OrderEnum orderEnum) throws ApiException {
        return null;
    }

    @Override
    public Result<Utxo> getTxOutput(String s, int i) throws ApiException {
        return null;
    }
}
