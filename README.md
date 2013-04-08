# OCPP charger simulator [![Build Status](https://secure.travis-ci.org/thenewmotion/ocpp-charger.png)](http://travis-ci.org/thenewmotion/ocpp-charger)

Actor based representation of ocpp chargers.
Can be run standalone against Central System as ordinary charger.


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