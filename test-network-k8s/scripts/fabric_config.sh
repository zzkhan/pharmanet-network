#!/bin/bash
#
# Copyright IBM Corp All Rights Reserved
#
# SPDX-License-Identifier: Apache-2.0
#

function init_namespace() {
  push_fn "Creating namespace \"$NS\""

  kubectl create namespace $NS || true

  pop_fn
}

function delete_namespace() {
  push_fn "Deleting namespace \"$NS\""

  kubectl delete namespace $NS || true

  pop_fn
}

function init_storage_volumes() {
  push_fn "Provisioning volume storage"

  # Both KIND and k3s use the Rancher local-path provider.  In KIND, this is installed
  # as the 'standard' storage class, and in Rancher as the 'local-path' storage class.
  if [ "${CLUSTER_RUNTIME}" == "kind" ]; then
    export STORAGE_CLASS="standard"

  elif [ "${CLUSTER_RUNTIME}" == "k3s" ]; then
    export STORAGE_CLASS="local-path"

  else
    echo "Unknown CLUSTER_RUNTIME ${CLUSTER_RUNTIME}"
    exit 1
  fi

  cat kube/pvc-fabric-org0.yaml | envsubst | kubectl -n $NS create -f - || true
  cat kube/pvc-fabric-org1.yaml | envsubst | kubectl -n $NS create -f - || true
  cat kube/pvc-fabric-org2.yaml | envsubst | kubectl -n $NS create -f - || true

  pop_fn
}

function load_org_config() {
  push_fn "Creating fabric config maps"

  kubectl -n $NS delete configmap org0-config || true
  kubectl -n $NS delete configmap org1-config || true
  kubectl -n $NS delete configmap org2-config || true

  kubectl -n $NS create configmap org0-config --from-file=config/org0
  kubectl -n $NS create configmap org1-config --from-file=config/org1
  kubectl -n $NS create configmap org2-config --from-file=config/org2

  pop_fn
}