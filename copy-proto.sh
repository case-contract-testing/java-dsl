#!/bin/bash
set -eu

# Copies proto files from the main repo, assumed to be ../contract-case

cp ../contract-case/packages/case-connector/proto/contract_case_stream.proto src/main/proto
