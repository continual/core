
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

# package a specific container
packageContainer containerDir containerName:
	@echo "packaging {{containerName}}"
	cd {{containerDir}} && docker buildx build --platform=linux/amd64 --build-arg PKG_VERSION={{BUILD_VERSION}} -f Dockerfile -t {{containerName}}:{{BUILD_VERSION}} .

# create a container image without building first
packageOnly: (packageContainer "eventFlows/processor/continualProcessor" "continualproc")
	@echo "Packaging container(s)"

# build java and create container image
package: java packageOnly

tagContainer containerName:
	@echo "tagging {{containerName}}"
	docker tag {{containerName}}:{{BUILD_VERSION}} registry.digitalocean.com/rathravane/{{containerName}}:{{BUILD_VERSION}}

pushContainer containerName: (tagContainer containerName)
	@echo "pushing {{containerName}}"
	docker push registry.digitalocean.com/rathravane/{{containerName}}:{{BUILD_VERSION}}

# build and push the package to our container repo
push: package (pushContainer "continualproc")
