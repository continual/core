#!/bin/sh
echo "Pulling job config from ${FC_CONFIG_URL}..."
curl --fail-with-body -v -o ${FC_CONFIG_FILE} "${FC_CONFIG_URL}"
if [ $? -ne 0 ]; then
	echo "Error: curl call failed."
	exit 1
fi
echo
echo
cat ${FC_CONFIG_FILE}
echo
