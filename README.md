# OCPP charger simulator [![Build Status](https://secure.travis-ci.org/thenewmotion/ocpp-charger.png)](http://travis-ci.org/thenewmotion/ocpp-charger)

Actor based representation of ocpp chargers.
Can be run standalone against Central System as ordinary charger.

It now also supports OCPP-J (OCPP over WebSocket with JSON) but it does not
support receiving incoming requests this way, and does a blocking wait on the
responses from the central system. This happens because we implemented the
OCPP-SOAP API on top of the JSON API. If the package were refactored to use the
JSON API natively it could be more functional and performant.


## Setup

1. Add this repository to your pom.xml:
    ```xml
    <repository>
        <id>thenewmotion</id>
        <name>The New Motion Repository</name>
        <url>http://nexus.thenewmotion.com/content/repositories/releases-public</url>
    </repository>
    ```

2. Add dependency to your pom.xml:
    ```xml
    <dependency>
        <groupId>com.thenewmotion.chargenetwork</groupId>
        <artifactId>ocpp-charger_2.10</artifactId>
        <version>2.6.1</version>
    </dependency>
    ```

## Start the charger

Compile & run with `sbt run`

### Options

See the source file src/main/scala/com/thenewmotion/chargenetwork/ocpp/charger/ChargerApp.scala for the different options.

Options can be passed using the a `-Dexec.args="..."` option to Maven, like this:

`sbt "run --connection-type soap http://localhost:8080/ocpp/"`

or

`sbt "run --connection-type json http://localhost:8080/ocppws/"` 

with basic authentication (ocpp-json only):

`sbt "run --id 01234567 --auth-password abcdef1234abcdef1234abcdef1234abcdef1234 ws://localhost:8017/ocppws/"`

with basic authentication and a specific ssl certificate (ocpp-json only):

`sbt "run --id 01234567 --auth-password abcdef1234abcdef1234abcdef1234abcdef1234 --keystore-file ./trust.jks --keystore-password my-beautiful-password wss://test-cn-node-internet.thenewmotion.com/ocppws/"`

The password is given in a hex-encoded form. If you have a plain text password, you can quickly encode it in hex as follows:

```
$ scala                                                                                                                                                                                                                         [13:19:11]
Welcome to Scala 2.12.1 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_51).
Type in expressions for evaluation. Or try :help.

scala> "mypasswordof20chars!".getBytes("US-ASCII").map("%02x".format(_)).mkString
res1: String = 6d7970617373776f72646f663230636861727321

```

## License

© 2012 - 2017 The New Motion B.V. Licensed under the terms of the [GPL version 3](LICENSE).
