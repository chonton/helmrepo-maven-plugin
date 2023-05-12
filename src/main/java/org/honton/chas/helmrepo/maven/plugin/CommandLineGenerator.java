package org.honton.chas.helmrepo.maven.plugin;

public class CommandLineGenerator {

  private StringBuilder sb = new StringBuilder("helm");

  public CommandLineGenerator upgrade() {
    sb.append(" upgrade --install ");
    return this;
  }

  public CommandLineGenerator appendRelease(Release release) {
    sb.append(release.getName()).append(' ').append(release.getChart());
    return this;
  }

  public String getCommand() {
    return sb.toString();
  }

  public CommandLineGenerator appendGlobalReleaseOptions(GlobalReleaseOptions options) {
    if (options != null) {
      appendKubernetes(options.getKubernetes());
      appendValues(options.getValueYaml());
    }
    return this;
  }

  private CommandLineGenerator appendKubernetes(Kubernetes kubernetes) {
    if (kubernetes != null) {
      String context = kubernetes.getContext();
      if (context != null) {
        sb.append(" --kube-context ").append(context);
      }
    }
    return this;
  }

  private void appendValues(String valueYaml) {}
}
