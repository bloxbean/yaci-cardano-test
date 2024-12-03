package com.bloxbean.cardano.yaci.test;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.backend.KupmiosBackendService;
import com.bloxbean.cardano.client.backend.api.*;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFEpochService;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFTransactionService;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFUtxoService;
import com.bloxbean.cardano.client.backend.kupo.KupoUtxoService;
import com.bloxbean.cardano.client.backend.ogmios.http.OgmiosEpochService;
import com.bloxbean.cardano.yaci.test.api.helper.YaciTestHelper;
import com.bloxbean.cardano.yaci.test.backend.ogmios.TestOgmiosTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * This container wraps Yaci CLI container which provides ability to create Cardano local dev cluster
 *
 */
@Slf4j
public class YaciCardanoContainer extends GenericContainer<YaciCardanoContainer> {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("bloxbean/yaci-cli");
    private static final String DEFAULT_TAG = "0.10.0-preview2";
    public static final int STORE_PORT = 8080;
    public static final int CLUSTER_HTTP_PORT = 10000;
    public static final int SUBMIT_API_PORT = 8090;
    public static final int NODE_PORT = 3001;
    public static final int OGMIOS_PORT = 1337;
    public static final int KUPO_PORT = 1442;
    private static float DEFAULT_SLOT_LENGTH = 1f;
    private static float DEFAULT_BLOCK_TIME = 1f;
    private static long DEFAULT_WAIT_TIMEOUT = 120;

    private static long waitTimeout;

    private ApiMode apiMode = ApiMode.YACI_STORE;
    private YaciTestHelper testHelper;

    public YaciCardanoContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public YaciCardanoContainer(String tag) {
        this(DEFAULT_IMAGE_NAME.withTag(tag));
    }

    public YaciCardanoContainer(final DockerImageName dockerImageName) {
        this(dockerImageName, DEFAULT_BLOCK_TIME, DEFAULT_WAIT_TIMEOUT);
    }

    public YaciCardanoContainer(final DockerImageName dockerImageName, float blockTime) {
        this(dockerImageName, blockTime, DEFAULT_WAIT_TIMEOUT);
    }

    public YaciCardanoContainer(final DockerImageName dockerImageName, float blockTime, long waitTimeout) {
        super(dockerImageName);
        this.waitTimeout = waitTimeout;

        if (blockTime >= 1 && blockTime <= 20) {
            dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
            withExposedPorts(STORE_PORT, CLUSTER_HTTP_PORT, SUBMIT_API_PORT, NODE_PORT, OGMIOS_PORT, KUPO_PORT);
            withCommand("create-node", "-o", "--slot-length", String.valueOf(DEFAULT_SLOT_LENGTH), "--block-time", String.valueOf(blockTime), "--start");
        } else {
            throw new IllegalArgumentException("Invalid blockTime. Value should be between 1 to 20");
        }
    }

    @Override
    public void start() {
        init();
        super.start();
    }

    private void init() {
        addEnv("yaci_cli_mode", "native");
        addEnv("yaci_store_mode", "native");

        addEnv("conwayHardForkAtEpoch", "1");
        addEnv("shiftStartTimeBehind", "true");

        //Update entry point to use yaci-cli directly
        withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint("/app/yaci-cli");
        });

        if (apiMode == ApiMode.YACI_STORE) {
            addEnv("yaci_store_enabled", "true");
            addEnv("ogmios_enabled", "true");

            waitingFor(Wait.forHttp("/api/v1/epochs/parameters")
                    .forPort(STORE_PORT)
                    .forStatusCode(200)
                    .forResponsePredicate(resp -> resp.contains("cost_models") && resp.contains("pool_deposit"))
                    .withStartupTimeout(Duration.ofSeconds(waitTimeout)));
            withStartupTimeout(Duration.ofSeconds(waitTimeout));
        } else if (apiMode == ApiMode.OGMIOS) {
            addEnv("ogmios_enabled", "true");
            addEnv("kupo_enabled", "true");
            waitingFor(Wait.forHttp("/")
                    .forPort(OGMIOS_PORT)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofSeconds(waitTimeout)));
            withStartupTimeout(Duration.ofSeconds(waitTimeout));
        } else
            throw new IllegalArgumentException("Invalid ApiMode : " + apiMode);
    }

    public YaciCardanoContainer withApiMode(ApiMode apiMode) {
        this.apiMode = apiMode;
        return this;
    }

    public YaciCardanoContainer withInitialFunding(Funding... fundings) {
        if (fundings == null || fundings.length == 0)
            return this;

        String topupAddresses = Arrays.stream(fundings)
                .map(funding -> funding.getAddress() + ":" + funding.getAdaValue())
                .collect(Collectors.joining(","));

        if (apiMode == ApiMode.YACI_STORE) {
            WaitStrategy waitStrategy = new WaitAllStrategy()
                    .withStrategy(Wait.forHttp("/api/v1/epochs/parameters")
                            .forPort(STORE_PORT)
                            .forStatusCode(200)
                            .forResponsePredicate(resp -> resp.contains("cost_models") && resp.contains("pool_deposit"))
                            .withStartupTimeout(Duration.ofSeconds(waitTimeout)))
                    .withStrategy(Wait.forHttp("/api/v1/addresses/" + fundings[0].getAddress() + "/utxos")
                            .forPort(STORE_PORT)
                            .forResponsePredicate(s -> s.contains(fundings[0].getAddress()))
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofSeconds(waitTimeout)));

            //Override default wait strategy if initial funding
            waitingFor(waitStrategy);
        }

        addEnv("topup_addresses", topupAddresses);

        return this;
    }

    public int getYaciStorePort() {
        return getMappedPort(STORE_PORT);
    }

    public int getOgmiosPort() {
        return getMappedPort(OGMIOS_PORT);
    }

    public int getKupoPort() {
        return getMappedPort(KUPO_PORT);
    }

    public int getLocalClusterPort() {
        return getMappedPort(CLUSTER_HTTP_PORT);
    }

    public int getSubmitApiPort() {
        return getMappedPort(SUBMIT_API_PORT);
    }

    public int getCardanoNodePort() {
        return getMappedPort(NODE_PORT);
    }

    public String getYaciStoreApiUrl() {
        int port = getYaciStorePort();
        return "http://localhost:" + port + "/api/v1/";
    }

    public String getLocalClusterApiUrl() {
        int port = getLocalClusterPort();
        return "http://localhost:" + port + "/local-cluster/api/";
    }

    public String getOgmiosHttpUrl() {
        int port = getOgmiosPort();
        return "http://localhost:" + port + "/";
    }

    public String getOgmiosWsUrl() {
        int port = getOgmiosPort();
        return "ws://localhost:" + port + "/";
    }

    public String getKupoUrl() {
        int port = getKupoPort();
        return "http://localhost:" + port + "/";
    }

    public UtxoSupplier getUtxoSupplier() {
        return new DefaultUtxoSupplier(getUtxoService());
    }

    public ProtocolParamsSupplier getProtocolParamsSupplier() {
        return new DefaultProtocolParamsSupplier(getEpochService());
    }

    public TransactionProcessor getTransactionProcessor() {
        TransactionService transactionService = getTransactionService();
        return new DefaultTransactionProcessor(transactionService);
    }

    @NotNull
    public EpochService getEpochService() {
        switch (apiMode) {
            case YACI_STORE:
                return new BFEpochService(getYaciStoreApiUrl(), "dummy key");
            case OGMIOS:
                return new OgmiosEpochService(getOgmiosHttpUrl());
            default:
                throw new IllegalArgumentException("Invalid ApiMode : " + apiMode);
        }
    }

    @NotNull
    public UtxoService getUtxoService() {
        switch (apiMode) {
            case YACI_STORE:
                return new BFUtxoService(getYaciStoreApiUrl(), "dummy key");
            case OGMIOS:
                return new KupoUtxoService(getKupoUrl());
            default:
                throw new IllegalArgumentException("Invalid ApiMode : " + apiMode);
        }
    }

    @NotNull
    public TransactionService getTransactionService() {
        switch (apiMode) {
            case YACI_STORE:
                return new BFTransactionService(getYaciStoreApiUrl(), "dummy key");
            case OGMIOS:
                return new TestOgmiosTransactionService(getOgmiosHttpUrl(), getKupoUrl());
            default:
                throw new IllegalArgumentException("Invalid ApiMode : " + apiMode);
        }
    }

    @NotNull
    public BackendService getBackendService() {
        switch (apiMode) {
            case YACI_STORE:
                return new BFBackendService(getYaciStoreApiUrl(), "dummy key");
            case OGMIOS:
                return new KupmiosBackendService(getOgmiosHttpUrl(), getKupoUrl());
            default:
                throw new IllegalArgumentException("Invalid ApiMode : " + apiMode);
        }
    }

    public YaciTestHelper getTestHelper() {
        if (testHelper == null)
            testHelper = new YaciTestHelper(this);
        return testHelper;
    }

    @Override
    public void stop() {
        super.stop();
    }
}
