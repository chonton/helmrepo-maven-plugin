package org.honton.chas.helmrepo.maven.plugin;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/** Upgrade helm release(s) */
@Mojo(name = "upgrade", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class HelmUpgrade extends HelmRelease {

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  MavenSession session;

  AtomicReference<KubernetesClient> cachedValue = new AtomicReference<>();

  @Override
  public void addSubCommand(List<String> command) {
    command.add("upgrade");
    command.add("--install");
  }

  @Override
  protected void postRelease(ReleaseInfo release, String namespace) {
    Map<String, String> nodePorts = release.getNodePorts();
    if (nodePorts != null) {
      Map<String, List<Service>> services = getServices(namespace);
      nodePorts.forEach(
          (property, value) -> {
            Selector selector = Selector.of(value);
            Service foundService = findService(property, release.getName(), services, selector);
            if (foundService != null) {
              ServicePort servicePort = findPort(property, foundService, selector);
              if (servicePort != null) {
                Integer nodePort = servicePort.getNodePort();
                if (nodePort == null) {
                  String warning =
                      String.format(
                          "Not setting property %s, nodePort not set for portName %s",
                          property, servicePort.getName());
                  getLog().warn(warning);
                } else {
                  getLog().info("Setting " + property + " to " + nodePort);
                  session.getUserProperties().setProperty(property, nodePort.toString());
                }
              }
            }
          });
    }
  }

  private Service findService(
      String property, String releaseName, Map<String, List<Service>> services, Selector selector) {
    List<Service> foundServices = services.get(selector.getService());
    if (foundServices == null || foundServices.isEmpty()) {
      String warning =
          String.format(
              "Did not set %s; release %s could not find service %s",
              property, releaseName, selector.getService());
      getLog().warn(warning);
      return null;
    }
    Service foundService = foundServices.get(0);
    if (foundServices.size() > 1) {
      String warning =
          String.format(
              "Release %s found multiple services %s; using namespace %s to set %s",
              releaseName,
              selector.getService(),
              foundService.getMetadata().getNamespace(),
              property);
      getLog().warn(warning);
    }
    return foundService;
  }

  private ServicePort findPort(String property, Service foundService, Selector selector) {
    final ServicePort port;
    final String warning;
    List<ServicePort> ports = foundService.getSpec().getPorts();
    if (ports == null || ports.isEmpty()) {
      port = null;
      warning =
          String.format(
              "Not setting property %s, no ports defined on service %s",
              property, foundService.getMetadata().getName());
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
                property, foundService.getMetadata().getName(), port.getName());
      } else {
        Optional<ServicePort> opt = ports.stream().filter(selector::matchesPort).findFirst();
        if (opt.isPresent()) {
          return opt.get();
        }
        port = null;
        warning =
            String.format(
                "Not setting property %s, no port defined on service %s with portName %s",
                property, foundService.getMetadata().getName(), portName);
      }
    }
    getLog().warn(warning);
    return port;
  }

  private Map<String, List<Service>> getServices(String namespace) {
    ServiceList list = getServiceList(namespace);
    Map<String, List<Service>> nameToService = new HashMap<>();
    list.getItems()
        .forEach(
            service ->
                nameToService
                    .computeIfAbsent(service.getMetadata().getName(), n -> new ArrayList<>())
                    .add(service));
    return nameToService;
  }

  private ServiceList getServiceList(String namespace) {
    KubernetesClient client = getKubernetesClient();
    MixedOperation<Service, ServiceList, ServiceResource<Service>> services = client.services();
    if (namespace != null) {
      return services.inNamespace(namespace).list();
    } else {
      return services.list();
    }
  }

  private KubernetesClient getKubernetesClient() {
    KubernetesClient result = cachedValue.get();
    if (result == null) {
      KubernetesClientBuilder clientBuilder = new KubernetesClientBuilder();
      KubernetesInfo kubernetesInfo = getKubernetes();
      if (kubernetesInfo != null) {
        String context = kubernetesInfo.getContext();
        if (context != null) {
          Config config = Config.autoConfigure(context);
          clientBuilder.withConfig(config);
        }
      }
      result = clientBuilder.build();
      if (!cachedValue.compareAndSet(null, result)) {
        return cachedValue.get();
      }
    }
    return result;
  }

  @Override
  public void releaseOptions(ReleaseInfo release, List<String> command) {
    command.add("--wait");
    if (release.getWait() != 0) {
      command.add("--timeout");
      command.add(release.getWait() + "s");
    }
  }

  @Override
  public void createNamespace(List<String> command) {
    if (kubernetes == null || !Boolean.FALSE.equals(kubernetes.getCreateNamespace())) {
      command.add("--create-namespace");
    }
  }
}
