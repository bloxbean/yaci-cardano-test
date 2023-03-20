package com.bloxbean.cardano.yaci.test;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.backend.api.DefaultProtocolParamsSupplier;
import com.bloxbean.cardano.client.backend.api.DefaultTransactionProcessor;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.yaci.test.api.helper.YaciTestHelper;
import com.bloxbean.cardano.yaci.test.backend.EpochService;
import com.bloxbean.cardano.yaci.test.backend.TransactionService;
import com.bloxbean.cardano.yaci.test.backend.UtxoService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
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
    private static final String DEFAULT_TAG = "0.0.11";
    public static final int STORE_PORT = 8080;
    public static final int CLUSTER_HTTP_PORT = 10000;
    public static final int SUBMIT_API_PORT = 8090;
    public static final int NODE_PORT = 3001;
    private static float DEFAULT_SLOT_LENGTH = 0.15f;

    public YaciCardanoContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public YaciCardanoContainer(final DockerImageName dockerImageName) {
        this(dockerImageName, DEFAULT_SLOT_LENGTH);
    }

    public YaciCardanoContainer(final DockerImageName dockerImageName, float slotLength) {
        super(dockerImageName);
        if (slotLength >= 0.1 && slotLength <= 1) {
            dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
            withExposedPorts(STORE_PORT, CLUSTER_HTTP_PORT, SUBMIT_API_PORT, NODE_PORT);
            withCommand("create-cluster", "-o", "--slotLength", String.valueOf(slotLength), ", start");
            addEnv("yaci_store_enabled", "true");

            waitingFor(Wait.forHttp("/api/v1/epochs/1/parameters")
                    .forPort(STORE_PORT)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofSeconds(90)));
            withStartupTimeout(Duration.ofSeconds(90));
        } else {
            throw new IllegalArgumentException("Invalid slotLength. Value should be between 0.1 to 1");
        }
    }

    public YaciCardanoContainer withInitialFunding(Funding... fundings) {
        if (fundings == null || fundings.length == 0)
            return this;

        String topupAddresses = Arrays.stream(fundings)
                .map(funding -> funding.getAddress() + ":" + funding.getAdaValue())
                .collect(Collectors.joining(","));

        addEnv("topup_addresses", topupAddresses);
        System.out.println(getEnv());
        return this;
    }

    public int getYaciStorePort() {
        return getMappedPort(STORE_PORT);
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
        return new EpochService(getYaciStoreApiUrl(), "dummy key");
    }

    @NotNull
    public UtxoService getUtxoService() {
        return new UtxoService(getYaciStoreApiUrl(), "dummy key");
    }

    @NotNull
    public TransactionService getTransactionService() {
        TransactionService transactionService = new TransactionService(getYaciStoreApiUrl(), "dummy key");
        return transactionService;
    }

    public YaciTestHelper getTestHelper() {
        return new YaciTestHelper(this);
    }
}
