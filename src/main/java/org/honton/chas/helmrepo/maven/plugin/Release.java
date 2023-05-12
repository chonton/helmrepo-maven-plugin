package org.honton.chas.helmrepo.maven.plugin;

import lombok.Data;

/** Information about a helm release */
@Data
public class Release {
  /** The name of the release. Defaults to the unversioned chart name. */
  String name;

  /** The namespace for un-scoped kubernetes resources */
  String namespace;

  /**
   * The chart for this release. This can be one of
   * <ul>
   *   <li>A chart reference: repository/chartname
   *   <li>A path to a packaged chart: superfantastic-44.12.3.tgz
   *   <li>A path to an unpacked chart directory: src/helm/superfantastic
   *   <li>An absolute URL:
   *       https://repo.maven.apache.org/maven2/org/honton/chas/test-reports/1.3.4/test-reports-1.3.4.tgz
   *   <li>An OCI registries: oci://example.com/charts/nginx
   * </ul>
   */
  String chart;

  /**
   * Values to be applied during upgrade. This is formatted as yaml.
   */
  String valueYaml;

  /**
   * A comma separated list of releases that must be deployed before this release.
   */
  String requires;

  /**
   * Number of seconds to wait for successful deployment.  Defaults to 300 secs (5 minutes)
   */
  long wait;
}
