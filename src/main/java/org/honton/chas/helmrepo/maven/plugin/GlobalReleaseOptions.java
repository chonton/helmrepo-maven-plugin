package org.honton.chas.helmrepo.maven.plugin;

import java.nio.file.Path;

/** Options to be applied to all releases. */
public interface GlobalReleaseOptions {
  /** Values to be applied during upgrade. This is formatted as yaml. */
  Path getGlobalValuePath();

  /** Information about the kubernetes cluster */
  KubernetesInfo getKubernetes();
}
