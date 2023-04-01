# Yaci Cardano Test
Make Java app testing on Cardano blockchain a breeze with Yaci Cardano Test Java library.

## What is Yaci Cardano Test?

Yaci Cardano Test is a Java library that provides a simple way to test your Java Cardano application. It uses [testcontainers](https://www.testcontainers.org/) 
to start a dev Cardano node in a docker container programmatically. It also provides a simple way to interact with the container
in your test code through a Test Helper class and use Cardano specific assertions to verify the results.

## Pre-requisites

- Docker - [please see General Docker requirements](https://www.testcontainers.org/supported_docker_environment/)

## How to use

Check this sample app https://github.com/bloxbean/yaci-cardano-test-sample 

## Dependencies

### Maven dependencies

```xml
  <dependency>
     <groupId>com.bloxbean.cardano</groupId>
     <artifactId>yaci-cardano-test</artifactId>
     <version>0.0.1</version>
     <scope>test</scope>
  </dependency>
```
You also need to add following cardano-client-lib dependencies and also junit 5.

```
<dependency>
     <groupId>com.bloxbean.cardano</groupId>
     <artifactId>cardano-client-lib</artifactId>
     <version>0.4.3</version>
</dependency>
<dependency>
     <groupId>com.bloxbean.cardano</groupId>
     <artifactId>cardano-client-backend</artifactId>
     <version>0.4.3</version>
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
testImplementation "com.bloxbean.cardano:yaci-cardano-test:0.0.1"
```

Other dependencies

```
implementation "com.bloxbean.cardano:cardano-client-lib:0.4.3"
implementation "com.bloxbean.cardano:cardano-client-backend:0.4.3"

testImplementation 'org.junit.jupiter:junit-jupiter-api:5.9.2'
testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.9.2'
```
