#!/bin/bash

# Parameters
SERVICE_NAME=$1
IMAGE=$2
PORT=$3
ENVIRONMENT=$4
NAMESPACE=$5
ACCOUNTID=$6


# Debug: Print input parameters
echo "Input Parameters:"
echo "Service Name: ${SERVICE_NAME}"
echo "Environment: ${ENVIRONMENT}"
echo "Image: ${IMAGE}"
echo "Namespace: ${NAMESPACE}"
echo "Account ID: ${ACCOUNTID}"
echo "Port: ${PORT}"

# Directory containing the templates
TEMPLATES_DIR="./eks/templates"

# Determine the correct YAML template based on SERVICE_NAME
TEMPLATE="${TEMPLATES_DIR}/eks-manifests.yaml"

# Debug: Print the selected template file
echo "Using template: $TEMPLATE"

# Debug: Check if the template file exists and if we have read permissions
if [ ! -f "$TEMPLATE" ]; then
    echo "Error: Template file $TEMPLATE does not exist!"
    exit 1
elif [ ! -r "$TEMPLATE" ]; then
    echo "Error: Template file $TEMPLATE exists but cannot be read!"
    exit 1
fi

# Set replicaset based on the environment
if [ "$ENVIRONMENT" == "alpha" ]; then
    REPLICASET=3
else
    REPLICASET=1 
fi

# Output file to store the replaced template
OUTPUT_FILE="${TEMPLATES_DIR}/updated.yaml"

# Debug: Check if we have write permissions to the output directory
if [ ! -w "$TEMPLATES_DIR" ]; then
    echo "Error: Cannot write to directory $TEMPLATES_DIR"
    exit 1
fi

# Process the template file
echo "Processing $TEMPLATE..."
sed -e "s|{{ServiceName}}|${SERVICE_NAME}|g" \
    -e "s|{{Image}}|${IMAGE}|g" \
    -e "s|{{Port}}|${PORT}|g" \
    -e "s|{{Environment}}|${ENVIRONMENT}|g" \
    -e "s|{{Namespace}}|${NAMESPACE}|g" \
    -e "s|{{Caccount}}|${ACCOUNTID}|g" \
    -e "s|{{Replicaset}}|${REPLICASET}|g" \
    "$TEMPLATE" > "$OUTPUT_FILE" || {
    echo "Error: sed command failed!"
    exit 1
}

# Check if the output file was successfully created
if [ -f "$OUTPUT_FILE" ]; then
    echo "Created $OUTPUT_FILE"

    # Print the contents of the updated file
    echo "Contents of $OUTPUT_FILE:"
    cat "$OUTPUT_FILE"
else
    echo "Error: Failed to create $OUTPUT_FILE"
    exit 1
fi