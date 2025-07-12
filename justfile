
set dotenv-load

# extract the build version number from the pom file
BUILD_VERSION := shell("
	VERLINE=$(grep '<version>' \"pom.xml\" | grep 'build_version' | head -n 1)
	if [[ -z \"$VERLINE\" ]]; then
		VERLINE=$(grep '<version>' \"pom.xml\" | head -n 1)
	fi
	if [[ -z \"$VERLINE\" ]]; then
		echo \"No <version> information here.\"
		exit 1
	fi
	echo $VERLINE | sed -e 's/.*version>\\(.*\\)<\\/version.*/\\1/'
")

# show available commands
default:
	@just --list

# === BUILD COMMANDS ===

# clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	mvn clean

# compile without running tests
compile:
	@echo "Compiling sources..."
	mvn compile

# full build with tests
build:
	@echo "Running full build with tests..."
	mvn clean install

# legacy alias for build
java: build

# fast build without tests
fast:
	@echo "Fast build (skipping tests)..."
	mvn clean install -DskipTests

# === TEST COMMANDS ===

# run all tests
test:
	@echo "Running all tests..."
	mvn test

# run tests for specific module
test-module MODULE:
	@echo "Running tests for module: {{MODULE}}"
	mvn test -pl {{MODULE}}

# run tests with coverage report
coverage:
	@echo "Running tests with coverage..."
	mvn clean test jacoco:report

# === MODULE COMMANDS ===

# build specific module with dependencies
build-module MODULE:
	@echo "Building module {{MODULE}} with dependencies..."
	mvn install -pl {{MODULE}} -am

# list all modules
modules:
	@echo "Available modules:"
	@find . -name "pom.xml" -not -path "./target/*" -exec dirname {} \; | grep -v "^\.$" | sort

# === VERSION MANAGEMENT ===

# update version across all modules
version:
	mvn versions:set -DgenerateBackupPoms=false
	mvn versions:update-child-modules -DgenerateBackupPoms=false

# show current version
show-version:
	@echo "Current version: {{BUILD_VERSION}}"

# === RELEASE COMMANDS ===

# create release build
release:
	@echo "Running maven for release..."
	mvn clean deploy -DperformRelease=true

# prepare for release (validate everything is ready)
release-check:
	@echo "Checking release readiness..."
	mvn clean verify -DperformRelease=true -DskipDeploy=true

# === DEVELOPMENT COMMANDS ===

# format code (if spotless is configured)
format:
	@echo "Formatting code..."
	-mvn spotless:apply || echo "Spotless not configured, skipping..."

# run dependency analysis
deps:
	@echo "Analyzing dependencies..."
	mvn dependency:tree

# check for outdated dependencies
deps-check:
	@echo "Checking for dependency updates..."
	mvn versions:display-dependency-updates

# === UTILITY COMMANDS ===

# start a service (example: just run continualIamApiService)
run SERVICE:
	@echo "Starting service: {{SERVICE}}"
	@if [ -d "{{SERVICE}}" ]; then \
		cd {{SERVICE}} && \
		if [ -f "target/{{SERVICE}}-{{BUILD_VERSION}}-jar-with-dependencies.jar" ]; then \
			java -jar target/{{SERVICE}}-{{BUILD_VERSION}}-jar-with-dependencies.jar; \
		else \
			echo "Service jar not found. Run 'just build-module {{SERVICE}}' first."; \
		fi \
	else \
		echo "Service directory {{SERVICE}} not found."; \
	fi

# show project info
info:
	@echo "Project: Continual Core ({{BUILD_VERSION}})"
	@echo "Java Version: $(mvn help:evaluate -Dexpression=java.version -q -DforceStdout)"
	@echo "Maven Version: $(mvn --version | head -1)"
	@echo "Modules: $(find . -name "pom.xml" -not -path "./target/*" | wc -l | tr -d ' ') total"

# clean everything including logs
deep-clean:
	@echo "Deep cleaning..."
	mvn clean
	find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
	find . -name "*.log" -type f -delete 2>/dev/null || true
	@echo "Deep clean complete"
