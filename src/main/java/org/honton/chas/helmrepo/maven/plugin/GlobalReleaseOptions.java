package org.honton.chas.helmrepo.maven.plugin;


/**
 * Options to be applied to all releases.
 */
public interface GlobalReleaseOptions {
  /**
   * Values to be applied during upgrade. This is formatted as yaml.
   */
  String getValueYaml();

  /**
   * Information about the kubernetes cluster
   */
  KubernetesInfo getKubernetes();
}
