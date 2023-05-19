package org.honton.chas.helmrepo.maven.plugin;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.dsl.AnyNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/** Upgrade helm release(s) */
@Mojo(name = "upgrade", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class HelmUpgrade extends HelmRelease {

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;

  @Override
  public void addSubCommand(List<String> command) {
    command.add("upgrade");
    command.add("--install");
  }

  @Override
  protected void postHelmCommand(ReleaseInfo release, String namespace) {
    Map<String, String> nodePorts = release.nodePorts;
    if (nodePorts != null) {
      nodePorts.forEach(
          (property, nodePort) -> setProperty(property, ServiceSelector.of(nodePort, namespace)));
    }
  }

  private void setProperty(String property, ServiceSelector selector) {
    long count =
        findServices(selector).filter(service -> setProperty(service, property, selector)).count();
    if (count == 0) {
      String warning =
          String.format("Not setting property %s, no matching service port %s", property, selector);
      getLog().warn(warning);
    }
  }

  private Stream<Service> findServices(ServiceSelector selector) {
    MixedOperation<Service, ServiceList, ServiceResource<Service>> services =
        getKubernetesClient().services();
    AnyNamespaceOperation<Service, ServiceList, ServiceResource<Service>> ans =
        selector.getNamespace() != null
            ? services.inNamespace(selector.getNamespace())
            : services.inAnyNamespace();
    return ans.resources()
        .filter(pr -> pr.item().getMetadata().getName().equals(selector.getService()))
        .map(Resource::item);
  }

  private boolean setProperty(Service service, String property, ServiceSelector selector) {
    ServicePort servicePort = findPort(service, property, selector);
    if (servicePort == null) {
      return false;
    }
    Integer nodePort = servicePort.getNodePort();
    if (nodePort == null) {
      String warning =
          String.format(
              "Not setting property %s, nodePort not set for portName %s",
              property, servicePort.getName());
      getLog().warn(warning);
      return true;
    }
    getLog().info("Setting " + property + " to " + nodePort);
    session.getUserProperties().setProperty(property, nodePort.toString());
    return true;
  }

  private ServicePort findPort(Service service, String property, ServiceSelector selector) {
    final ServicePort port;
    final String warning;
    List<ServicePort> ports = service.getSpec().getPorts();
    if (ports == null || ports.isEmpty()) {
      port = null;
      warning =
          String.format(
              "Not setting property %s, no ports defined on service %s",
              property, service.getMetadata().getName());
    } else {
      String portName = selector.getPort();
      if (portName == null) {
        port = ports.get(0);
        if (ports.size() == 1) {
          return port;
        }
        warning =
            String.format(
                "Possible incorrect value for %s, multiple ports defined on service %s and no portName specified; using portName %s",
                property, service.getMetadata().getName(), port.getName());
      } else {
        Optional<ServicePort> opt = ports.stream().filter(selector::matchesPort).findFirst();
        if (opt.isPresent()) {
          return opt.get();
        }
        port = null;
        warning =
            String.format(
                "Not setting property %s, no port defined on service %s with portName %s",
                property, service.getMetadata().getName(), portName);
      }
    }
    getLog().warn(warning);
    return port;
  }

  @Override
  public void releaseOptions(ReleaseInfo release, List<String> command) {
    command.add("--wait");
    if (release.wait != 0) {
      command.add("--timeout");
      command.add(release.wait + "s");
    }
  }

  @Override
  public void createNamespace(List<String> command) {
    if (kubernetes == null || !Boolean.FALSE.equals(kubernetes.getCreateNamespace())) {
      command.add("--create-namespace");
    }
  }
}
