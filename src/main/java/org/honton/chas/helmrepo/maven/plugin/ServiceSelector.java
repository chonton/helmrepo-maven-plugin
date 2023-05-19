package org.honton.chas.helmrepo.maven.plugin;

import io.fabric8.kubernetes.api.model.ServicePort;
import lombok.NonNull;
import lombok.Value;

/** Selector for service ports */
@Value
public class ServiceSelector {
  /** The namespace of service */
  String namespace;

  /** The name of service */
  @NonNull String service;

  /** The name of port. Optional if only one port is defined for the service */
  String port;

  public static ServiceSelector of(String nsp, String namespace) {
    int slash = nsp.indexOf('/');
    if (slash >= 0) {
      namespace = nsp.substring(0, slash).trim();
    } else if (namespace != null) {
      namespace = namespace.trim();
    }

    int idx = nsp.indexOf(':', ++slash);
    return idx < 0
        ? new ServiceSelector(namespace, nsp.substring(slash).trim(), null)
        : new ServiceSelector(
            namespace, nsp.substring(slash, idx).trim(), nsp.substring(idx + 1).trim());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (namespace != null) {
      sb.append(namespace).append('/');
    }
    sb.append(service);
    if (port != null) {
      sb.append(':').append(port);
    }
    return sb.toString();
  }

  public boolean matchesPort(ServicePort servicePort) {
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
