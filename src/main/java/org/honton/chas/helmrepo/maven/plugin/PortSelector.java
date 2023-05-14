package org.honton.chas.helmrepo.maven.plugin;

import lombok.Data;

/** Selector for service ports */
@Data
public class PortSelector {
  /** The name of service. */
  private String serviceName;

  /** The name of port. Optional if only one port is defined for the service */
  private String portName;

  /** The name of property to receive nodePort. */
  private String propertyName;
}
