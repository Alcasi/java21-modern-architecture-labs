# Modern Java 21 & Spring Boot 3 Architecture Labs

A comprehensive hands-on architecture laboratory focused on modern JVM capabilities, cloud-ready architectures, and enterprise engineering patterns using **Java 21 LTS** and **Spring Boot 3.3+**.

---

## 🏛️ Architecture & Modules

This repository is organized as a Maven multi-module project:

### 1. [`lab-virtual-threads`](./lab-virtual-threads)
* **Project Loom Deep-Dive:** Comparative benchmark executing 10,000 concurrent blocking I/O tasks.
  * *Traditional Platform Threads (Java 8 Pool):* **50,053 ms**
  * *Virtual Threads (Java 21 Loom):* **1,057 ms** (~47x throughput increase)
* **Thread Pinning Diagnosis & Mitigation:** Identified carrier thread pinning caused by `synchronized` blocks using `-Djdk.tracePinnedThreads=full`. Re-engineered synchronization using `java.util.concurrent.Semaphore` to protect carrier threads.

### 2. [`lab-java21-records`](./lab-java21-records)
* **Modern Domain Modeling:** Implemented immutable domain structures using Java 21 `record` and `sealed interface` hierarchies.
* **Pattern Matching for Switch:** Exhaustive type-checking and record pattern deconstruction without legacy `instanceof` casting or verbose DTO boilerplate.

### 3. [`lab-springboot3-core`](./lab-springboot3-core)
* **Tomcat Virtual Threads:** Embedded Tomcat configured with `spring.threads.virtual.enabled=true` verifying `Thread.currentThread().isVirtual()`.
* **Modern HTTP Client:** Migration from legacy `RestTemplate` to the fluent, non-blocking `RestClient`.
* **Declarative HTTP Interfaces:** Decoupled business services from transport layers using Spring 6 native `@HttpExchange` and `HttpServiceProxyFactory`, replacing legacy Spring Cloud OpenFeign dependencies.

---

## 🚀 Getting Started

### Prerequisites
* Java 21 LTS (Eclipse Temurin)
* Apache Maven 3.9+
* Docker CE

### Build All Modules
```bash
mvn clean package


cd lab-springboot3-core
mvn spring-boot:run

curl -i http://localhost:8080/api/cambio/USD/BRL

]
