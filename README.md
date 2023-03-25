# Yaci Cardano Test
Make Java app testing on Cardano blockchain a breeze with Yaci Cardano Test Java library.

## What is Yaci Cardano Test?

Yaci Cardano Test is a Java library that provides a simple way to test your Java Cardano application. It uses testcontainer 
to start a dev Cardano node in a docker container programmatically. It also provides a simple way to interact with the container
in your test code through a Test Helper class and use Cardano specific assertions to verify the results.

## Dependencies

Maven dependency:

```xml
  <dependency>
     <groupId>com.bloxbean.cardano</groupId>
     <artifactId>yaci-cardano-test</artifactId>
     <version>{version}</version>
     <scope>test</scope>
  </dependency>
```
