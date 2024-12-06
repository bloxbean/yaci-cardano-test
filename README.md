# Yaci Cardano Test
Make Java app testing on Cardano blockchain a breeze with Yaci Cardano Test Java library.

## What is Yaci Cardano Test?

Yaci Cardano Test is a Java library that provides a simple way to test your Java Cardano application. It uses [testcontainers](https://www.testcontainers.org/) 
to start a dev Cardano node in a docker container programmatically. It also provides a simple way to interact with the container
in your test code through a Test Helper class and use Cardano specific assertions to verify the results.

## Pre-requisites

- Docker - [please see General Docker requirements](https://www.testcontainers.org/supported_docker_environment/)

## Dependencies

### Maven dependencies

```xml
  <dependency>
     <groupId>com.bloxbean.cardano</groupId>
     <artifactId>yaci-cardano-test</artifactId>
     <version>0.1.0</version>
     <scope>test</scope>
  </dependency>
```
You also need to add following cardano-client-lib dependencies and also junit 5.

```
<dependency>
     <groupId>com.bloxbean.cardano</groupId>
     <artifactId>cardano-client-lib</artifactId>
     <version>0.6.2</version>
</dependency>
<dependency>
     <groupId>com.bloxbean.cardano</groupId>
     <artifactId>cardano-client-backend</artifactId>
     <version>0.6.2</version>
</dependency>
<dependency>
     <groupId>com.bloxbean.cardano</groupId>
     <artifactId>cardano-client-backend-ogmios</artifactId>
     <version>0.6.2</version>
</dependency>
<dependency>
     <groupId>com.bloxbean.cardano</groupId>
     <artifactId>cardano-client-backend-blockfrost</artifactId>
     <version>0.6.2</version>
 </dependency>
 <dependency>
     <groupId>org.junit.jupiter</groupId>
     <artifactId>junit-jupiter-engine</artifactId>
     <version>5.9.2</version>
     <scope>test</scope>
</dependency>

```

### Gradle Dependencies

```
testImplementation "com.bloxbean.cardano:yaci-cardano-test:0.1.0"
```

Other dependencies

```
implementation "com.bloxbean.cardano:cardano-client-lib:0.6.2"
implementation "com.bloxbean.cardano:cardano-client-backend:0.6.2"
implementation "com.bloxbean.cardano:cardano-client-backend-ogmios:0.6.2"
implementation "com.bloxbean.cardano:cardano-client-backend-blockfrost:0.6.2"

testImplementation 'org.junit.jupiter:junit-jupiter-api:5.9.2'
testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.9.2'
```

## Quick Start

### Start a Cardano Node with Yaci Store API (Blockfrost Compatible API) support

The following code snippet demonstrates how to launch a Cardano node in a Docker container and fund an account with 20,000 ADA. 
By default, the Yaci Store API is enabled, which can be used to query and submit transactions to the running node. 
The Yaci Store API is compatible with the Blockfrost API.

```java
YaciCardanoContainer yaciCardanoContainer = new YaciCardanoContainer()
                .withInitialFunding(new Funding(account.baseAddress(), 20000))
                .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()));

yaciCardanoContainer.start();
```

### Start a Cardano Node with Ogmios & Kupo Api Support

```java
 YaciCardanoContainer yaciCardanoContainer = new YaciCardanoContainer()
                .withApiMode(ApiMode.OGMIOS)
                .withInitialFunding(new Funding(account.baseAddress(), 20000))
                .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()));

yaciCardanoContainer.start();
```

### Build and Submit a Transaction

Get Cardano Client Lib Suppliers required to build and submit transactions

```java
var backendService = yaciCardanoContainer.getBackendService();

var utxoSupplier = yaciCardanoContainer.getUtxoSupplier();
var protocolParamsSupplier = yaciCardanoContainer.getProtocolParamsSupplier();
var transactionProcessor = yaciCardanoContainer.getTransactionProcessor();
```

Using the above suppliers, you can build and submit transactions using one of the transaction builders in the Cardano Client Library.

### Assertion Library

Yaci Cardano Test provides a set of Cardano specific assertions to verify the results of your tests. 
For example, you can verify the ada or asset balance of an address, verify utxo size, etc.

```java
  Assertions.assertMe(cardanoContainer).hasLovelaceBalance(receiverAddress, adaToLovelace(2.1));
  Assertions.assertMe(cardanoContainer).hasAssetBalance(receiverAddress, policy.getPolicyId(), "abc", BigInteger.valueOf(300));
  Assertions.assertMe(cardanoContainer).utxos(receiverAddress).hasSize(1);
  Assertions.assertMe(cardanoContainer).utxos(receiverAddress).hasLovelaceBalance(adaToLovelace(2.1));
```

## Examples

Check this sample project https://github.com/bloxbean/yaci-cardano-test-sample

For more tests check the [test](https://github.com/bloxbean/yaci-cardano-test/tree/main/src/test/java/com/bloxbean/cardano/yaci/test) pacakage in this project.

# Any questions, ideas or issues?

- Create a Github [Discussion](https://github.com/bloxbean/yaci-cardano-test/discussions)
- Create a Github [Issue](https://github.com/bloxbean/yaci-cardano-test/issues)
- [Discord Server](https://discord.gg/JtQ54MSw6p)
