package org.honton.chas.helmrepo.maven.plugin;

import io.fabric8.kubernetes.api.model.ServicePort;
import lombok.NonNull;
import lombok.Value;

/** Selector for service ports */
@Value
public class Selector {
  /** The name of service. */
  @NonNull String service;

  /** The name of port. Optional if only one port is defined for the service */
  String port;

  public static Selector of(String serviceAndPort) {
    int idx = serviceAndPort.indexOf(':');
    return idx < 0
        ? new Selector(serviceAndPort, null)
        : new Selector(serviceAndPort.substring(0, idx), serviceAndPort.substring(idx + 1));
  }

  boolean matchesPort(ServicePort servicePort) {
    if (servicePort.getName() == null && port == null) {
      // port must be sole port for service
      return true;
    }
    if (servicePort.getName().equals(port)) {
      return true;
    }
    return port != null && servicePort.getPort().toString().equals(port);
  }
}
