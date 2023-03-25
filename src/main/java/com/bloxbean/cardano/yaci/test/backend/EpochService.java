package com.bloxbean.cardano.yaci.test.backend;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.model.EpochContent;
import com.bloxbean.cardano.yaci.test.backend.http.EpochApi;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EpochService extends BaseService implements com.bloxbean.cardano.client.backend.api.EpochService {

    private EpochApi epochApi;

    public EpochService(String baseUrl, String projectId) {
        super(baseUrl, projectId);
        this.epochApi = getFeign().target(EpochApi.class, baseUrl);
    }

    @Override
    public Result<EpochContent> getLatestEpoch() throws ApiException {
        try {
            EpochContent epochContent = epochApi.getLatestEpoch();
            return Result.success(String.valueOf(epochContent)).withValue(epochContent).code(200);
        } catch (FeignException e) {
            log.debug("Error getting latest epoch: ", e);
            return Result.error(e.contentUTF8()).code(e.status());
        } catch (Exception e) {
            throw new ApiException("Error getting latest epoch", e);
        }
    }

    @Override
    public Result<EpochContent> getEpoch(Integer epoch) throws ApiException {
        try {
            EpochContent epochContent = epochApi.getEpochByNumber(epoch);
            return Result.success(String.valueOf(epochContent)).withValue(epochContent).code(200);
        } catch (FeignException e) {
            log.debug("Error getting epoch: ", e);
            return Result.error(e.contentUTF8()).code(e.status());
        } catch (Exception e) {
            throw new ApiException("Getting epoch", e);
        }
    }

    @Override
    public Result<ProtocolParams> getProtocolParameters(Integer epoch) throws ApiException {
        try {
            ProtocolParams protocolParams = epochApi.getProtocolParameters(epoch);
            return Result.success(String.valueOf(protocolParams)).withValue(protocolParams).code(200);
        } catch (FeignException e) {
            log.debug("Error getting protocol parameters by number : " + epoch, e);
            return Result.error(e.contentUTF8()).code(e.status());
        } catch (Exception e) {
            throw new ApiException("Error getting protocol parameters by number : " + epoch, e);
        }
    }

    @Override
    public Result<ProtocolParams> getProtocolParameters() throws ApiException {
        Result<EpochContent> epochContentResult = getLatestEpoch();
        if(!epochContentResult.isSuccessful())
            throw new ApiException("Unable to get latest epoch info to get protocol parameters");

        EpochContent epochContent = epochContentResult.getValue();
        if(epochContent == null)
            throw new ApiException("Unable to get latest epoch info to get protocol parameters");

        return getProtocolParameters(epochContent.getEpoch());
    }
}
