package org.honton.chas.helmrepo.maven.plugin;

import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

/** Information about the kubernetes cluster */
@Data
public class KubernetesInfo {
  /** The name of the kubectl context to use */
  @Parameter private String context;

  /** Namespace to use if no release namespace */
  @Parameter private String namespace;

  /** Create namespace if namespace not present */
  @Parameter(defaultValue = "true")
  private Boolean createNamespace;
}
