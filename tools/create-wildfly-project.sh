#!/usr/bin/env bash
set -euo pipefail

if [[ "${BASH_VERSINFO[0]}" -lt 4 ]]; then
    echo "ERROR: Bash 4+ is required (for associative arrays). Found: ${BASH_VERSION}" >&2
    exit 1
fi

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------
readonly DEFAULT_VERSION="1.0.0-SNAPSHOT"
readonly DEFAULT_WILDFLY_VERSION="39.0.1.Final"
readonly DEFAULT_PLUGIN_VERSION="6.0.0.Beta3"
readonly SCRIPT_NAME="$(basename "$0")"

# ---------------------------------------------------------------------------
# Output helpers
# ---------------------------------------------------------------------------
info()  { printf '\033[0;32m[INFO]\033[0m  %s\n' "$1"; }
warn()  { printf '\033[0;33m[WARN]\033[0m  %s\n' "$1"; }
error() { printf '\033[0;31m[ERROR]\033[0m %s\n' "$1" >&2; }
die()   { error "$1"; exit 1; }

# ---------------------------------------------------------------------------
# Usage
# ---------------------------------------------------------------------------
usage() {
    cat <<USAGE
Usage: ${SCRIPT_NAME} -g GROUP_ID -a ARTIFACT_ID [options] [feature-flags]

Generate a Maven WAR project configured for WildFly deployment.

Required:
  -g, --group-id ID           Maven groupId
  -a, --artifact-id ID        Maven artifactId

Optional:
  -v, --version VER           Project version           (default: ${DEFAULT_VERSION})
  -p, --package PKG           Java package              (default: same as groupId)
  -o, --output-dir DIR        Output directory           (default: .)
      --wildfly-version VER   WildFly server version     (default: ${DEFAULT_WILDFLY_VERSION})
      --wildfly-plugin-version VER  WildFly Maven plugin (default: ${DEFAULT_PLUGIN_VERSION})
  -h, --help                  Show this help

Feature flags (each adds dependencies and sample source code):
      --rest                  Jakarta REST (JAX-RS)
      --cdi                   CDI (Contexts and Dependency Injection)
      --ejb                   EJB (Enterprise JavaBeans)
      --jpa                   JPA / Hibernate
      --jsf                   Jakarta Faces (JSF)
      --websocket             WebSocket
      --jms                   JMS messaging (implies --rest --cdi)
      --batch                 Batch processing (implies --cdi)
      --microprofile-config   MicroProfile Config (implies --rest --cdi)
      --microprofile-health   MicroProfile Health (implies --rest --cdi)
      --microprofile-fault-tolerance  MicroProfile Fault Tolerance (implies --rest --cdi)
      --microprofile-openapi  MicroProfile OpenAPI (implies --rest --cdi)

If no feature flags are given, a Hello World servlet is generated.

Examples:
  ${SCRIPT_NAME} -g com.example -a demo
  ${SCRIPT_NAME} -g com.example -a demo-rest --rest --cdi
  ${SCRIPT_NAME} -g com.example -a demo-mp --microprofile-config --microprofile-health
  ${SCRIPT_NAME} -g com.example -a demo-full --rest --cdi --jpa --ejb
USAGE
}

# ---------------------------------------------------------------------------
# Parse arguments
# ---------------------------------------------------------------------------
GROUP_ID=""
ARTIFACT_ID=""
VERSION="${DEFAULT_VERSION}"
PACKAGE=""
OUTPUT_DIR="."
WILDFLY_VERSION="${DEFAULT_WILDFLY_VERSION}"
PLUGIN_VERSION="${DEFAULT_PLUGIN_VERSION}"

FEAT_REST=0
FEAT_CDI=0
FEAT_EJB=0
FEAT_JPA=0
FEAT_JSF=0
FEAT_WEBSOCKET=0
FEAT_JMS=0
FEAT_BATCH=0
FEAT_MP_CONFIG=0
FEAT_MP_HEALTH=0
FEAT_MP_FT=0
FEAT_MP_OPENAPI=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        -g|--group-id)          GROUP_ID="$2";          shift 2 ;;
        -a|--artifact-id)       ARTIFACT_ID="$2";       shift 2 ;;
        -v|--version)           VERSION="$2";           shift 2 ;;
        -p|--package)           PACKAGE="$2";           shift 2 ;;
        -o|--output-dir)        OUTPUT_DIR="$2";        shift 2 ;;
        --wildfly-version)      WILDFLY_VERSION="$2";   shift 2 ;;
        --wildfly-plugin-version) PLUGIN_VERSION="$2";  shift 2 ;;
        --rest)                 FEAT_REST=1;            shift ;;
        --cdi)                  FEAT_CDI=1;             shift ;;
        --ejb)                  FEAT_EJB=1;             shift ;;
        --jpa)                  FEAT_JPA=1;             shift ;;
        --jsf)                  FEAT_JSF=1;             shift ;;
        --websocket)            FEAT_WEBSOCKET=1;       shift ;;
        --jms)                  FEAT_JMS=1;             shift ;;
        --batch)                FEAT_BATCH=1;           shift ;;
        --microprofile-config)  FEAT_MP_CONFIG=1;       shift ;;
        --microprofile-health)  FEAT_MP_HEALTH=1;       shift ;;
        --microprofile-fault-tolerance) FEAT_MP_FT=1;   shift ;;
        --microprofile-openapi) FEAT_MP_OPENAPI=1;      shift ;;
        -h|--help)              usage; exit 0 ;;
        *)                      die "Unknown option: $1. Use -h for help." ;;
    esac
done

# ---------------------------------------------------------------------------
# Validate
# ---------------------------------------------------------------------------
[[ -z "${GROUP_ID}" ]]    && die "Missing required parameter: --group-id (-g)"
[[ -z "${ARTIFACT_ID}" ]] && die "Missing required parameter: --artifact-id (-a)"
command -v mvn >/dev/null 2>&1 || die "Maven (mvn) is not installed or not on PATH"

PACKAGE="${PACKAGE:-${GROUP_ID}}"
PACKAGE_PATH="${PACKAGE//\.//}"
PROJECT_DIR="${OUTPUT_DIR}/${ARTIFACT_ID}"

[[ -d "${PROJECT_DIR}" ]] && die "Directory already exists: ${PROJECT_DIR}"

# ---------------------------------------------------------------------------
# Compute implied features
# ---------------------------------------------------------------------------
NEEDS_REST=0
NEEDS_CDI=0
NEEDS_MICROPROFILE=0

# MicroProfile specs imply REST + CDI and the expansion BOM
if (( FEAT_MP_CONFIG || FEAT_MP_HEALTH || FEAT_MP_FT || FEAT_MP_OPENAPI )); then
    NEEDS_REST=1
    NEEDS_CDI=1
    NEEDS_MICROPROFILE=1
fi

# JMS implies REST + CDI
if (( FEAT_JMS )); then
    NEEDS_REST=1
    NEEDS_CDI=1
fi

# Batch, JPA, JSF imply CDI
if (( FEAT_BATCH || FEAT_JPA || FEAT_JSF )); then
    NEEDS_CDI=1
fi

# EJB implies CDI
if (( FEAT_EJB )); then
    NEEDS_CDI=1
fi

# Explicit flags
if (( FEAT_REST )); then NEEDS_REST=1; fi
if (( FEAT_CDI ));  then NEEDS_CDI=1; fi

# Determine if any feature is active
HAS_ANY_FEATURE=0
if (( FEAT_REST || FEAT_CDI || FEAT_EJB || FEAT_JPA || FEAT_JSF || FEAT_WEBSOCKET \
   || FEAT_JMS || FEAT_BATCH || FEAT_MP_CONFIG || FEAT_MP_HEALTH || FEAT_MP_FT \
   || FEAT_MP_OPENAPI )); then
    HAS_ANY_FEATURE=1
fi

# ---------------------------------------------------------------------------
# Dependency helper — deduplicated accumulation
# ---------------------------------------------------------------------------
declare -A DEPS_SEEN
DEPS_XML=""

add_dep() {
    local gid="$1" aid="$2" scope="$3"
    local key="${gid}:${aid}"
    if [[ -z "${DEPS_SEEN[$key]+x}" ]]; then
        DEPS_SEEN[$key]=1
        DEPS_XML+="        <dependency>
            <groupId>${gid}</groupId>
            <artifactId>${aid}</artifactId>
            <scope>${scope}</scope>
        </dependency>
"
    fi
}

# ---------------------------------------------------------------------------
# Build dependency list
# ---------------------------------------------------------------------------
if (( HAS_ANY_FEATURE == 0 )); then
    add_dep "jakarta.servlet" "jakarta.servlet-api" "provided"
fi

if (( NEEDS_REST )); then
    add_dep "jakarta.ws.rs" "jakarta.ws.rs-api" "provided"
fi

if (( NEEDS_CDI )); then
    add_dep "jakarta.enterprise" "jakarta.enterprise.cdi-api" "provided"
    add_dep "jakarta.annotation" "jakarta.annotation-api" "provided"
fi

if (( FEAT_EJB )); then
    add_dep "jakarta.ejb" "jakarta.ejb-api" "provided"
fi

if (( FEAT_JPA )); then
    add_dep "jakarta.persistence" "jakarta.persistence-api" "provided"
    add_dep "org.hibernate.orm" "hibernate-core" "provided"
fi

if (( FEAT_JSF )); then
    add_dep "jakarta.faces" "jakarta.faces-api" "provided"
fi

if (( FEAT_WEBSOCKET )); then
    add_dep "jakarta.websocket" "jakarta.websocket-api" "provided"
    add_dep "jakarta.websocket" "jakarta.websocket-client-api" "provided"
fi

if (( FEAT_JMS )); then
    add_dep "jakarta.jms" "jakarta.jms-api" "provided"
fi

if (( FEAT_BATCH )); then
    add_dep "jakarta.batch" "jakarta.batch-api" "provided"
fi

if (( FEAT_MP_CONFIG )); then
    add_dep "org.eclipse.microprofile.config" "microprofile-config-api" "provided"
fi

if (( FEAT_MP_HEALTH )); then
    add_dep "org.eclipse.microprofile.health" "microprofile-health-api" "provided"
fi

if (( FEAT_MP_FT )); then
    add_dep "org.eclipse.microprofile.faulttolerance" "microprofile-fault-tolerance-api" "provided"
fi

if (( FEAT_MP_OPENAPI )); then
    add_dep "org.eclipse.microprofile.openapi" "microprofile-openapi-api" "provided"
fi

# Always add JUnit test dependency
DEPS_XML+="        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
"

# ---------------------------------------------------------------------------
# Build optional POM sections
# ---------------------------------------------------------------------------
PROPS_EXTRA=""
BOM_EXTRA=""

if (( NEEDS_MICROPROFILE )); then
    PROPS_EXTRA="        <version.bom.expansion>\${version.server}</version.bom.expansion>"
    BOM_EXTRA="            <dependency>
                <groupId>org.wildfly.bom</groupId>
                <artifactId>wildfly-expansion</artifactId>
                <version>\${version.bom.expansion}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>"
fi

# ---------------------------------------------------------------------------
# Step 1: Maven archetype generation
# ---------------------------------------------------------------------------
info "Generating Maven webapp project skeleton..."
mvn -B archetype:generate \
    -DarchetypeArtifactId=maven-archetype-webapp \
    -DgroupId="${GROUP_ID}" \
    -DartifactId="${ARTIFACT_ID}" \
    -Dversion="${VERSION}" \
    -Dpackage="${PACKAGE}" \
    -DinteractiveMode=false \
    -DoutputDirectory="${OUTPUT_DIR}" \
    || die "Maven archetype generation failed"

info "Archetype generated at ${PROJECT_DIR}"

# ---------------------------------------------------------------------------
# Step 2: Replace pom.xml with WildFly-patterned POM
# ---------------------------------------------------------------------------
info "Configuring WildFly POM..."

cat > "${PROJECT_DIR}/pom.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>${GROUP_ID}</groupId>
    <artifactId>${ARTIFACT_ID}</artifactId>
    <version>${VERSION}</version>
    <packaging>war</packaging>
    <name>${ARTIFACT_ID}</name>

    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <version.server>${WILDFLY_VERSION}</version.server>
        <version.bom.ee>\${version.server}</version.bom.ee>
${PROPS_EXTRA:+${PROPS_EXTRA}
}        <version.plugin.wildfly>${PLUGIN_VERSION}</version.plugin.wildfly>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
${BOM_EXTRA:+${BOM_EXTRA}
}            <dependency>
                <groupId>org.wildfly.bom</groupId>
                <artifactId>wildfly-ee-with-tools</artifactId>
                <version>\${version.bom.ee}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
${DEPS_XML}    </dependencies>

    <build>
        <finalName>\${project.artifactId}</finalName>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.wildfly.plugins</groupId>
                    <artifactId>wildfly-maven-plugin</artifactId>
                    <version>\${version.plugin.wildfly}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

    <profiles>
        <profile>
            <id>provisioned-server</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.wildfly.plugins</groupId>
                        <artifactId>wildfly-maven-plugin</artifactId>
                        <configuration>
                            <discover-provisioning-info>
                                <version>\${version.server}</version>
                            </discover-provisioning-info>
                        </configuration>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>package</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
        <profile>
            <id>openshift</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.wildfly.plugins</groupId>
                        <artifactId>wildfly-maven-plugin</artifactId>
                        <configuration>
                            <discover-provisioning-info>
                                <version>\${version.server}</version>
                                <context>cloud</context>
                            </discover-provisioning-info>
                        </configuration>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>package</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
        <profile>
            <id>integration-testing</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-failsafe-plugin</artifactId>
                        <configuration>
                            <includes>
                                <include>**/*IT</include>
                            </includes>
                        </configuration>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>integration-test</goal>
                                    <goal>verify</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>

    <repositories>
        <repository>
            <id>jboss-public-maven-repository</id>
            <name>JBoss Public Maven Repository</name>
            <url>https://repository.jboss.org/nexus/content/groups/public/</url>
        </repository>
    </repositories>
    <pluginRepositories>
        <pluginRepository>
            <id>jboss-public-maven-repository</id>
            <name>JBoss Public Maven Repository</name>
            <url>https://repository.jboss.org/nexus/content/groups/public/</url>
        </pluginRepository>
    </pluginRepositories>
</project>
EOF

# ---------------------------------------------------------------------------
# Step 3: Clean up archetype artifacts
# ---------------------------------------------------------------------------
info "Cleaning up archetype artifacts..."
rm -f "${PROJECT_DIR}/src/main/webapp/index.jsp"
rm -f "${PROJECT_DIR}/src/main/webapp/WEB-INF/web.xml"

# ---------------------------------------------------------------------------
# Step 4: Generate Java source files
# ---------------------------------------------------------------------------
info "Generating source files..."

SRC_DIR="${PROJECT_DIR}/src/main/java/${PACKAGE_PATH}"
mkdir -p "${SRC_DIR}"

# -- Default: Hello World servlet (only when no features selected) ----------
if (( HAS_ANY_FEATURE == 0 )); then
    cat > "${SRC_DIR}/HelloWorldServlet.java" <<JAVA
package ${PACKAGE};

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/HelloWorld")
public class HelloWorldServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter writer = resp.getWriter();
        writer.println("<html><head><title>${ARTIFACT_ID}</title></head><body>");
        writer.println("<h1>Hello World!</h1>");
        writer.println("</body></html>");
        writer.close();
    }
}
JAVA
fi

# -- REST: JakartaRESTActivator + HelloResource ----------------------------
if (( NEEDS_REST )); then
    cat > "${SRC_DIR}/JakartaRESTActivator.java" <<JAVA
package ${PACKAGE};

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("rest")
public class JakartaRESTActivator extends Application {
}
JAVA
fi

if (( FEAT_REST )); then
    cat > "${SRC_DIR}/HelloResource.java" <<JAVA
package ${PACKAGE};

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello World!";
    }
}
JAVA
fi

# -- CDI: GreeterService ---------------------------------------------------
if (( FEAT_CDI )); then
    cat > "${SRC_DIR}/GreeterService.java" <<JAVA
package ${PACKAGE};

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreeterService {

    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
JAVA
fi

# -- EJB: HelloBean --------------------------------------------------------
if (( FEAT_EJB )); then
    cat > "${SRC_DIR}/HelloBean.java" <<JAVA
package ${PACKAGE};

import jakarta.ejb.Stateless;

@Stateless
public class HelloBean {

    public String sayHello(String name) {
        return "Hello, " + name + "!";
    }
}
JAVA
fi

# -- JPA: SampleEntity -----------------------------------------------------
if (( FEAT_JPA )); then
    cat > "${SRC_DIR}/SampleEntity.java" <<JAVA
package ${PACKAGE};

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class SampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
JAVA
fi

# -- JSF: HelloJsfBean -----------------------------------------------------
if (( FEAT_JSF )); then
    cat > "${SRC_DIR}/HelloJsfBean.java" <<JAVA
package ${PACKAGE};

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named
@RequestScoped
public class HelloJsfBean {

    private String name;
    private String greeting;

    public void greet() {
        greeting = "Hello, " + name + "!";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGreeting() {
        return greeting;
    }
}
JAVA
fi

# -- WebSocket: HelloWebSocket ---------------------------------------------
if (( FEAT_WEBSOCKET )); then
    cat > "${SRC_DIR}/HelloWebSocket.java" <<JAVA
package ${PACKAGE};

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/websocket/hello")
public class HelloWebSocket {

    @OnMessage
    public String onMessage(String message) {
        return "Echo: " + message;
    }

    @OnOpen
    public void onOpen(Session session) {
    }

    @OnClose
    public void onClose() {
    }
}
JAVA
fi

# -- Batch: HelloBatchlet --------------------------------------------------
if (( FEAT_BATCH )); then
    cat > "${SRC_DIR}/HelloBatchlet.java" <<JAVA
package ${PACKAGE};

import jakarta.batch.api.AbstractBatchlet;
import jakarta.inject.Named;

@Named
public class HelloBatchlet extends AbstractBatchlet {

    @Override
    public String process() throws Exception {
        System.out.println("Hello from batch processing!");
        return "COMPLETED";
    }
}
JAVA
fi

# -- MicroProfile Config: ConfigResource ------------------------------------
if (( FEAT_MP_CONFIG )); then
    cat > "${SRC_DIR}/ConfigResource.java" <<JAVA
package ${PACKAGE};

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/config")
@ApplicationScoped
public class ConfigResource {

    @Inject
    @ConfigProperty(name = "app.greeting", defaultValue = "Hello from MicroProfile Config!")
    private String greeting;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getGreeting() {
        return greeting;
    }
}
JAVA
fi

# -- MicroProfile Health: LivenessCheck -------------------------------------
if (( FEAT_MP_HEALTH )); then
    cat > "${SRC_DIR}/LivenessCheck.java" <<JAVA
package ${PACKAGE};

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class LivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("${ARTIFACT_ID}-liveness").up().build();
    }
}
JAVA
fi

# -- MicroProfile Fault Tolerance: ResilientResource ------------------------
if (( FEAT_MP_FT )); then
    cat > "${SRC_DIR}/ResilientResource.java" <<JAVA
package ${PACKAGE};

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

@Path("/resilient")
@ApplicationScoped
public class ResilientResource {

    @GET
    @Retry(maxRetries = 3)
    @Fallback(fallbackMethod = "fallbackHello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from resilient endpoint!";
    }

    public String fallbackHello() {
        return "Fallback: Service is currently unavailable.";
    }
}
JAVA
fi

# -- MicroProfile OpenAPI: OpenApiResource ----------------------------------
if (( FEAT_MP_OPENAPI )); then
    cat > "${SRC_DIR}/OpenApiResource.java" <<JAVA
package ${PACKAGE};

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api")
public class OpenApiResource {

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Say hello", description = "Returns a hello message")
    @APIResponse(responseCode = "200", description = "Successful response")
    public String hello() {
        return "Hello from OpenAPI-annotated endpoint!";
    }
}
JAVA
fi

# ---------------------------------------------------------------------------
# Step 5: Generate resource files
# ---------------------------------------------------------------------------
info "Generating resource files..."

# -- beans.xml (CDI, JSF, or MicroProfile) ----------------------------------
if (( NEEDS_CDI || FEAT_JSF )); then
    mkdir -p "${PROJECT_DIR}/src/main/webapp/WEB-INF"
    cat > "${PROJECT_DIR}/src/main/webapp/WEB-INF/beans.xml" <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd"
       bean-discovery-mode="all" version="4.0">
</beans>
XML
fi

# -- persistence.xml (JPA) -------------------------------------------------
if (( FEAT_JPA )); then
    mkdir -p "${PROJECT_DIR}/src/main/resources/META-INF"
    cat > "${PROJECT_DIR}/src/main/resources/META-INF/persistence.xml" <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<persistence version="3.0"
             xmlns="https://jakarta.ee/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd">
    <persistence-unit name="primary">
        <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>
        <properties>
            <property name="hibernate.hbm2ddl.auto" value="create-drop"/>
            <property name="hibernate.show_sql" value="false"/>
        </properties>
    </persistence-unit>
</persistence>
XML
fi

# -- faces-config.xml (JSF) ------------------------------------------------
if (( FEAT_JSF )); then
    mkdir -p "${PROJECT_DIR}/src/main/webapp/WEB-INF"
    cat > "${PROJECT_DIR}/src/main/webapp/WEB-INF/faces-config.xml" <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<faces-config xmlns="https://jakarta.ee/xml/ns/jakartaee"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_4_0.xsd"
              version="4.0">
</faces-config>
XML
fi

# -- hello.xhtml (JSF) -----------------------------------------------------
if (( FEAT_JSF )); then
    cat > "${PROJECT_DIR}/src/main/webapp/hello.xhtml" <<'XHTML'
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="jakarta.faces.html">
<h:head>
    <title>Hello JSF</title>
</h:head>
<h:body>
    <h:form>
        <h:outputLabel for="name" value="Name: "/>
        <h:inputText id="name" value="#{helloJsfBean.name}"/>
        <h:commandButton value="Greet" action="#{helloJsfBean.greet}"/>
        <br/>
        <h:outputText value="#{helloJsfBean.greeting}" rendered="#{not empty helloJsfBean.greeting}"/>
    </h:form>
</h:body>
</html>
XHTML
fi

# -- batch job XML (Batch) -------------------------------------------------
if (( FEAT_BATCH )); then
    mkdir -p "${PROJECT_DIR}/src/main/resources/META-INF/batch-jobs"
    cat > "${PROJECT_DIR}/src/main/resources/META-INF/batch-jobs/hello-job.xml" <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<job id="hello-job" xmlns="https://jakarta.ee/xml/ns/jakartaee" version="2.0">
    <step id="hello-step">
        <batchlet ref="helloBatchlet"/>
    </step>
</job>
XML
fi

# -- microprofile-config.properties (MP Config) -----------------------------
if (( FEAT_MP_CONFIG )); then
    mkdir -p "${PROJECT_DIR}/src/main/resources/META-INF"
    cat > "${PROJECT_DIR}/src/main/resources/META-INF/microprofile-config.properties" <<'PROPS'
app.greeting=Hello from MicroProfile Config!
PROPS
fi

# ---------------------------------------------------------------------------
# Step 6: Generate index.html
# ---------------------------------------------------------------------------
info "Generating index.html..."

# Remove WEB-INF directory if empty (no beans.xml etc.)
rmdir "${PROJECT_DIR}/src/main/webapp/WEB-INF" 2>/dev/null || true

# Determine redirect target
REDIRECT_TARGET=""
if (( FEAT_JSF )); then
    REDIRECT_TARGET="hello.xhtml"
elif (( FEAT_REST )); then
    REDIRECT_TARGET="rest/hello"
elif (( FEAT_MP_CONFIG )); then
    REDIRECT_TARGET="rest/config"
elif (( FEAT_MP_OPENAPI )); then
    REDIRECT_TARGET="rest/api/hello"
elif (( FEAT_MP_FT )); then
    REDIRECT_TARGET="rest/resilient"
elif (( FEAT_MP_HEALTH )); then
    REDIRECT_TARGET="health"
fi

if (( FEAT_WEBSOCKET )) && [[ -z "${REDIRECT_TARGET}" ]]; then
    # Generate an interactive WebSocket HTML page
    cat > "${PROJECT_DIR}/src/main/webapp/index.html" <<HTML
<html>
<head>
    <title>${ARTIFACT_ID}</title>
</head>
<body>
    <h1>WebSocket Echo</h1>
    <div>
        <input id="message" type="text" placeholder="Type a message..."/>
        <button onclick="sendMessage()">Send</button>
    </div>
    <pre id="output"></pre>
    <script>
        var ws = new WebSocket('ws://' + window.location.host + '/${ARTIFACT_ID}/websocket/hello');
        var output = document.getElementById('output');
        ws.onmessage = function(event) {
            output.textContent += event.data + '\\n';
        };
        function sendMessage() {
            var msg = document.getElementById('message').value;
            ws.send(msg);
            document.getElementById('message').value = '';
        }
    </script>
</body>
</html>
HTML
elif [[ -n "${REDIRECT_TARGET}" ]]; then
    cat > "${PROJECT_DIR}/src/main/webapp/index.html" <<HTML
<html>
<head>
    <meta http-equiv="Refresh" content="0; URL=${REDIRECT_TARGET}">
</head>
</html>
HTML
else
    # Default: redirect to HelloWorld servlet
    cat > "${PROJECT_DIR}/src/main/webapp/index.html" <<HTML
<html>
<head>
    <meta http-equiv="Refresh" content="0; URL=HelloWorld">
</head>
</html>
HTML
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
info "Project '${ARTIFACT_ID}' created successfully at ${PROJECT_DIR}"
echo ""

# List active features
FEATURES=""
(( FEAT_REST ))       && FEATURES+="REST "
(( FEAT_CDI ))        && FEATURES+="CDI "
(( FEAT_EJB ))        && FEATURES+="EJB "
(( FEAT_JPA ))        && FEATURES+="JPA "
(( FEAT_JSF ))        && FEATURES+="JSF "
(( FEAT_WEBSOCKET ))  && FEATURES+="WebSocket "
(( FEAT_JMS ))        && FEATURES+="JMS "
(( FEAT_BATCH ))      && FEATURES+="Batch "
(( FEAT_MP_CONFIG ))  && FEATURES+="MP-Config "
(( FEAT_MP_HEALTH ))  && FEATURES+="MP-Health "
(( FEAT_MP_FT ))      && FEATURES+="MP-FaultTolerance "
(( FEAT_MP_OPENAPI )) && FEATURES+="MP-OpenAPI "

if [[ -n "${FEATURES}" ]]; then
    info "Features: ${FEATURES}"
else
    info "Features: Hello World servlet (default)"
fi

echo ""
info "Next steps:"
echo "  cd ${PROJECT_DIR}"
echo "  mvn clean package -Pprovisioned-server    # build with provisioned server"
echo "  mvn wildfly:dev -Pprovisioned-server      # run in dev mode"
echo ""
