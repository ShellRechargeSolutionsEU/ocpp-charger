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
            <artifactId>ocpp-charger_2.9.2</artifactId>
            <version>1.0-SNAPSHOT</version>
    </dependency>
```

## Start the charger

1. First compile with `mvn compile`
2. Then start the charger with `mvn exec:java -Dexec.mainClass="com.thenewmotion.chargenetwork.ocpp.charger.ChargerApp"`