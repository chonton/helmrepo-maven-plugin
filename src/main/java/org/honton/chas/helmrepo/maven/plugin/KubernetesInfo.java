package org.honton.chas.helmrepo.maven.plugin;

import lombok.Value;

/** Information about the kubernetes cluster */
@Value
public class KubernetesInfo {
  /** The name of the kubectl context to use */
  String context;

  /** The namespace for un-scoped kubernetes resources */
  String namespace;

  /** Create namespace if not present */
  Boolean createNamespace;
}
