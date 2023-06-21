#!/bin/bash

# Check if the argument is provided
if [ $# -eq 0 ]; then
    echo "Error: Please provide a message."
    exit 1
fi

# Optional argument: TOPIC
TOPIC=""
if [ -n "$2" ]; then
	TOPIC="$2"
fi

# Optional argument: STREAM
STREAM=""
if [ -n "$3" ]; then
	STREAM="$3"
fi

# check for creds
if [ -z "${CONTINUAL_USER}" ]; then
	echo "Please set CONTINUAL_USER and CONTINUAL_PASSWORD in your environment."
	exit 2;
fi
if [ -z "${CONTINUAL_PASSWORD}" ]; then
	echo "Please set CONTINUAL_USER and CONTINUAL_PASSWORD in your environment."
	exit 3;
fi

# setup the service URL optionally with topic and stream
url="https://rcvr.continual.io/events"
if [ -n "${TOPIC}" ]; then
	url="${url}/${TOPIC}"
	if [ -n "${STREAM}" ]; then
		url="${url}/${STREAM}"
	fi
fi

# JSON payload with the string argument
payload="{ \"message\": \"$1\" }"

# Perform the POST request and capture the HTTP status and response
http_response=$(curl -s -w "\n%{http_code}" -u "${CONTINUAL_USER}:${CONTINUAL_PASSWORD}" -X POST -H "Content-Type: application/json" -d "$payload" "$url")

# Extract the response and HTTP status from the combined output
response_body=$(echo "$http_response" | sed '$d')
http_status=$(echo "$http_response" | tail -n 1)

# Echo the response from the service
echo "$http_status: $response_body"
