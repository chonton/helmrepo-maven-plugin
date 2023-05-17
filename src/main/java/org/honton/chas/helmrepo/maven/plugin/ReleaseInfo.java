package org.honton.chas.helmrepo.maven.plugin;

import java.util.List;
import lombok.Data;

/** Information about a helm release */
@Data
public class ReleaseInfo {
  /** The name of the release. Defaults to the unversioned chart name. */
  private String name;

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
  private String chart;

  /** Values to be applied during upgrade. This is formatted as yaml. */
  private String valueYaml;

  /** A comma separated list of releases that must be deployed before this release. */
  private String requires;

  /**
   * Mapping service and port name to maven property name. The maven property will be set to the
   * corresponding kubernetes service exposed port.
   */
  private List<PortSelector> nodePorts;

  /** Number of seconds to wait for successful deployment. Defaults to 300 secs (5 minutes) */
  private long wait;
}
