
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

# menu
default:
	@just --list

# java build
java:
	@echo "Running maven..."
	@mvn clean install
