package org.honton.chas.helmrepo.maven.plugin;

import java.util.List;
import java.util.Map;
import lombok.Setter;

/** Information about a helm release */
@Setter
public class ReleaseInfo {

  /** The namespace for un-scoped kubernetes resources */
  String namespace;

  /** The name of the release. Defaults to the unversioned chart name. */
  String name;

  /**
   * The chart for this release. This can be one of
   *
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

  /** Values to be applied during upgrade. This is formatted as yaml. */
  String valueYaml;

  /** A comma separated list of releases that must be deployed before this release. */
  String requires;

  /** Number of seconds to wait for successful deployment. Defaults to 300 secs (5 minutes) */
  long wait;

  /**
   * Maven property names to receive nodePort assignments. The service namespace and name are
   * separated by '/'. The namespace is optional, name is required. The service name and port are
   * separated by ':'. The port is optional if the service only has a single port. The maven
   * property will be set to the corresponding kubernetes service nodePort.
   */
  Map<String, String> nodePorts;

  /** Callect logs from pods. Each pod is specified by optional namespace / name */
  List<String> logs;
}
