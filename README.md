# openliberty-microprofile-metrics-gradle

A minimal, production-style Open Liberty application built with **Gradle** that exposes MicroProfile Health and MicroProfile Metrics endpoints.
---

## What this project does

- Exposes `/health`, `/health/live`, and `/health/ready` via **MicroProfile Health 4.0**
- Exposes `/metrics` in Prometheus format via **MicroProfile Metrics 5.0**
- Runs on **Open Liberty** — a lightweight, modular Jakarta EE application server by IBM
- Built and managed entirely with **Gradle** using the Liberty Gradle Plugin

---

## Tech stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 (OpenJDK) | Runtime language |
| Open Liberty | 26.x (auto-downloaded) | Application server |
| Gradle | 8.8 (via wrapper) | Build tool |
| Liberty Gradle Plugin | 3.9.5 | Manages Liberty lifecycle from Gradle |
| MicroProfile Health | 4.0 | `/health` endpoints |
| MicroProfile Metrics | 5.0 | `/metrics` Prometheus endpoint |
| Jakarta EE | 10.0 | Platform APIs |
| CDI | 4.0 | Dependency injection for health check classes |

---

## Project structure

```
openliberty-microprofile-metrics-gradle/
├── build.gradle                          # Gradle build config — dependencies, Liberty plugin
├── settings.gradle                       # Project name declaration
├── gradlew                               # Gradle wrapper script (always use this, not system gradle)
├── gradlew.bat                           # Windows wrapper (ignore on Linux)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar            # Wrapper bootstrapper
│       └── gradle-wrapper.properties     # Pins Gradle to version 8.8
└── src/
    └── main/
        ├── java/
        │   └── com/poc/health/
        │       ├── LivenessCheck.java     # @Liveness — is the app alive?
        │       └── ReadinessCheck.java   # @Readiness — is the app ready for traffic?
        ├── liberty/
        │   └── config/
        │       └── server.xml            # Liberty config: features, ports, security
        └── webapp/
            └── WEB-INF/
                └── beans.xml             # Activates CDI so Java annotations work
```

---

## Prerequisites

| Requirement | Version | Check command |
|---|---|---|
| Java JDK | 21 | `java -version` |
| Gradle (optional) | any | Only needed to generate wrapper — wrapper handles the rest |
| Internet access | — | First run downloads Liberty + dependencies |

Install Java 21 on Ubuntu if not present:
```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
java -version
```

You do **not** need Gradle installed globally. The `./gradlew` wrapper script downloads and uses Gradle 8.8 automatically on first run.

---

## Getting started

### Clone and run

```bash
git clone https://github.com/YOUR_USERNAME/openliberty-microprofile-metrics-gradle.git
cd openliberty-microprofile-metrics-gradle

# First run downloads: Gradle 8.8, Open Liberty runtime, all dependencies
# Takes 3-5 minutes on first run, fast after that
./gradlew libertyRun
```

Wait for this line in the output:
```
[AUDIT] CWWKF0011I: The server pocServer is ready to run a smarter planet.
```

### Test the endpoints

Open a second terminal in the same directory:

```bash
# Overall health — aggregates all checks
curl http://localhost:9080/health

# Liveness check only
curl http://localhost:9080/health/live

# Readiness check only
curl http://localhost:9080/health/ready

# All metrics in Prometheus format (no auth required on HTTP)
curl http://localhost:9080/metrics

# JVM thread count specifically
curl http://localhost:9080/metrics | grep thread
```

---

## Expected responses

**`/health`**
```json
{
  "status": "UP",
  "checks": [
    {
      "name": "poc-liveness",
      "status": "UP",
      "data": {}
    },
    {
      "name": "poc-readiness",
      "status": "UP",
      "data": {
        "instance": "B-canary",
        "version": "1.0"
      }
    }
  ]
}
```

**`/metrics | grep thread`** (Prometheus format)
```
thread_count{mp_scope="base",} 35.0
thread_daemon_count{mp_scope="base",} 32.0
thread_max_count{mp_scope="base",} 42.0
threadpool_activeThreads{mp_scope="vendor",pool="Default_Executor",} 1.0
threadpool_size{mp_scope="vendor",pool="Default_Executor",} 4.0
```

---

## Gradle commands reference

```bash
# Foreground — see all logs live, Ctrl+C to stop
./gradlew libertyRun

# Background — Liberty runs as a daemon process
./gradlew libertyStart

# Stop a background Liberty instance
./gradlew libertyStop

# Compile and build WAR only — does not start Liberty
./gradlew build

# Build WAR, skipping tests
./gradlew build -x test

# Delete all build artifacts and start fresh
./gradlew clean

# Dev mode — auto-reloads on file save (like nodemon for Java)
./gradlew libertyDev
```

---

## Key configuration files explained

### `build.gradle`

The Gradle equivalent of Maven's `pom.xml`. Defines:
- The Liberty Gradle Plugin (downloads and manages Liberty)
- `providedCompile` dependencies — APIs needed to compile but NOT bundled in the WAR (Liberty provides them at runtime)
- The WAR filename — must match `server.xml`'s `<webApplication location="..."/>`

### `server.xml`

Liberty's main configuration file. Controls:
- Which features are loaded (`cdi-4.0`, `mpHealth-4.0`, `mpMetrics-5.0`)
- HTTP/HTTPS ports (9080 / 9443)
- Security config for the `/metrics` endpoint
- `<mpMetrics authentication="false"/>` — allows Prometheus to scrape `/metrics` over HTTP without credentials (appropriate for internal network POC)

### `beans.xml`

Required by CDI (Jakarta Contexts and Dependency Injection). Without this file, Liberty ignores your `@Liveness` and `@Readiness` annotated classes entirely. The `bean-discovery-mode="all"` setting tells CDI to scan all classes in the WAR.

### `LivenessCheck.java`

Implements `@Liveness`. Answers: **"Is the app alive, or should the server restart it?"**

In Kubernetes, if liveness returns `DOWN`, the pod is killed and restarted. For this POC it always returns `UP`. In production you'd check things like: is the JVM out of memory? Are threads deadlocked?

### `ReadinessCheck.java`

Implements `@Readiness`. Answers: **"Is the app ready to receive traffic?"**

In Kubernetes, if readiness returns `DOWN`, the pod is temporarily removed from the load balancer rotation but not killed. Use this to signal: "I'm starting up" or "I'm in maintenance mode". This check returns instance metadata (`instance: B-canary`) which is useful for verifying canary routing.

---

## Deploying to a server (scp method)

```bash
# Build the WAR locally
./gradlew build -x test

# Copy WAR to remote server
scp build/libs/liberty-poc.war ubuntu@YOUR_SERVER_IP:/tmp/

# On the remote server — drop into Liberty's dropins folder for auto-deploy
ssh ubuntu@YOUR_SERVER_IP \
  "sudo cp /tmp/liberty-poc.war \
   ~/openliberty-microprofile-metrics-gradle/build/wlp/usr/servers/pocServer/dropins/"
```

Liberty auto-detects and deploys any WAR placed in the `dropins/` directory — no restart needed.

---

## Prometheus scrape config

Add this to your `prometheus.yml` to scrape this instance:

```yaml
scrape_configs:
  - job_name: 'liberty-canary'
    static_configs:
      - targets: ['YOUR_VM3_IP:9080']
        labels:
          instance: 'VM3'
          version:  'canary'
          build:    'gradle'
```

---

## Key metrics for Grafana dashboards

| Metric name | Type | Description |
|---|---|---|
| `thread_count` | gauge | Current JVM live thread count |
| `thread_daemon_count` | gauge | Current daemon thread count |
| `thread_max_count` | gauge | Peak thread count since JVM start |
| `threadpool_activeThreads` | gauge | Liberty executor threads currently working |
| `threadpool_size` | gauge | Total Liberty thread pool size |
| `memory_committedHeap_bytes` | gauge | JVM committed heap in bytes |
| `cpu_processCpuLoad_percent` | gauge | JVM CPU utilization |
| `jvm_uptime_seconds` | gauge | Seconds since Liberty started |

Grafana query to show thread count across both Liberty instances side by side:
```
thread_count{mp_scope="base"}
```

---

## Industry concepts demonstrated

**MicroProfile Health** — a specification for Java microservices to expose standardised health check endpoints. Used by Kubernetes liveness and readiness probes to manage pod lifecycle automatically.

**MicroProfile Metrics** — exposes JVM and application metrics in Prometheus text format. The `/metrics` endpoint is scraped by Prometheus and visualised in Grafana.

**Liberty Gradle Plugin** — IBM's official Gradle plugin that manages the full Liberty server lifecycle: download, configure, start, deploy, stop. Equivalent to the `liberty-maven-plugin` for Maven projects.

**Gradle Wrapper (`./gradlew`)** — pins the Gradle version inside the project so every developer and CI pipeline uses the exact same version regardless of what's installed on the machine. Industry standard practice — always commit the wrapper, always use `./gradlew` not `gradle`.

**WAR file** — Web Application Archive. The standard Java deployment artifact for web applications. Contains your compiled classes, dependencies, and configuration. Dropped into an application server (Liberty, Tomcat, etc.) to run.

**CDI (Contexts and Dependency Injection)** — Jakarta EE's dependency injection framework. The `@Liveness`, `@Readiness`, and `@ApplicationScoped` annotations only work because CDI is active. `beans.xml` is what activates it.

---

## Troubleshooting

**`gradle: command not found` when generating wrapper**
```bash
# Install via SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle 8.8
```

**`./gradlew: Permission denied`**
```bash
chmod +x gradlew
```

**Port 9080 already in use**
```bash
# Find what's using it
ss -tlnp | grep 9080
# Kill it or change the port in server.xml
```

**`JAVA_HOME is not set`**
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

**`/metrics` returns empty on HTTP**

Make sure `server.xml` contains:
```xml
<mpMetrics authentication="false"/>
```
Without this, Liberty only serves `/metrics` over HTTPS (port 9443) with credentials.

**Liberty not picking up code changes**

Use `./gradlew libertyDev` instead of `libertyRun`. Dev mode watches for file changes and hot-reloads automatically.

---

## Author

Built as a DevOps POC demonstrating Open Liberty, MicroProfile observability, Gradle build tooling, and NGINX canary deployments on an on-premise 3-VM cluster.
