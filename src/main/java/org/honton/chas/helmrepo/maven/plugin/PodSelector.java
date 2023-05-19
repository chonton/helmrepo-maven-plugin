package org.honton.chas.helmrepo.maven.plugin;

import lombok.NonNull;
import lombok.Value;

/** Selector for service ports */
@Value
public class PodSelector {
  /** The namespace of service */
  String namespace;

  /** The name of service */
  @NonNull String pod;

  public static PodSelector of(String nsp, String namespace) {
    int slash = nsp.indexOf('/');
    if (slash >= 0) {
      namespace = nsp.substring(0, slash).trim();
    } else if (namespace != null) {
      namespace = namespace.trim();
    }

    return new PodSelector(namespace, nsp.substring(slash + 1).trim());
  }
}
