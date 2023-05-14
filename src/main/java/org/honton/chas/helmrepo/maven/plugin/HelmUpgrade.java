package org.honton.chas.helmrepo.maven.plugin;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Upgrade helm release(s)
 */
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
  public String chartReference(ReleaseInfo info) {
    return info.getChart();
  }

  @Override
  public Path releaseValues(String valuesFileName) {
    return targetValuesPath.resolve(valuesFileName);
  }

  @Override
  public Iterable<ReleaseInfo> getIterable(LinkedList<ReleaseInfo> inOrder) {
    return inOrder;
  }

  @Override
  protected boolean validateRelease(ReleaseInfo release) {
    boolean errors = super.validateRelease(release);
    List<PortSelector> nodePorts = release.getNodePorts();
    if (nodePorts != null) {
      Map<String, PortSelector> nodePortMapping = new HashMap<>();
      long count = nodePorts.stream().filter(selector -> {
        String propertyName = selector.getPropertyName();
        if (propertyName == null) {
          getLog().error("Missing propertyName in " + selector);
          return true;
        }
        if (nodePortMapping.putIfAbsent(propertyName, selector) != null) {
          getLog().error("Multiple definitions for property " + propertyName);
          return true;
        }
        if (selector.getServiceName() == null) {
          getLog().error("Missing service name in " + selector);
          return true;
        }
        return false;
      }).count();
      errors = count > 0;
    }
    return errors;
  }

  @Override
  protected void postRelease(ReleaseInfo release) {
    List<PortSelector> portSelectors = release.getNodePorts();
    if (portSelectors != null) {
      Map<String, List<Service>> services = getServices(release);
      portSelectors.forEach(selector -> {
        Service foundService = findService(release.getName(), services, selector);
        if (foundService != null) {
          ServicePort servicePort = findPort(foundService, selector);
          if (servicePort != null) {
            Integer nodePort = servicePort.getNodePort();
            if (nodePort == null) {
              String warning = String.format("Not setting property %s, nodePort not set for portName %s", selector.getPropertyName(), servicePort.getName());
              getLog().warn(warning);
            } else {
              getLog().info("Setting " + selector.getPropertyName() + " to " + nodePort);
              session.getUserProperties().setProperty(selector.getPropertyName(), nodePort.toString());
            }
          }
        }
      });
    }
  }

  private Service findService(String releaseName, Map<String, List<Service>> services, PortSelector selector) {
    List<Service> foundServices = services.get(selector.getServiceName());
    if (foundServices == null || foundServices.isEmpty()) {
      String warning = String.format("Did not set %s; release %s could not find service %s", selector.getPropertyName(), releaseName, selector.getServiceName());
      getLog().warn(warning);
      return null;
    }
    Service foundService = foundServices.get(0);
    if (foundServices.size() > 1) {
      String warning = String.format("Release %s found multiple services %s; using namespace %s to set %s", releaseName, selector.getServiceName(), foundService.getMetadata().getNamespace(), selector.getPropertyName());
      getLog().warn(warning);
    }
    return foundService;
  }

  private ServicePort findPort(Service foundService, PortSelector selector) {
    List<ServicePort> ports = foundService.getSpec().getPorts();
    if (ports == null || ports.isEmpty()) {
      String warning = String.format("Not setting property %s, no ports defined on service %s", selector.getPropertyName(), foundService.getMetadata().getName());
      getLog().warn(warning);
      return null;
    }
    String portName = selector.getPortName();
    if (portName != null) {
      Optional<ServicePort> optionalPort = ports.stream().filter(port -> portName.equals(port.getName())).findFirst();
      if (optionalPort.isEmpty()) {
        String warning = String.format("Not setting property %s, no port defined on service %s with portName %s", selector.getPropertyName(), foundService.getMetadata().getName(), portName);
        getLog().warn(warning);
        return null;
      }
      return optionalPort.get();
    }
    ServicePort port = ports.get(0);
    if (ports.size() != 1) {
      String warning = String.format("Possible incorrect value for %s, multiple ports defined on service %s and no portName specified; using portName %s", selector.getPropertyName(), foundService.getMetadata().getName(), port.getName());
      getLog().warn(warning);
    }
    return port;
  }

  private Map<String, List<Service>> getServices(ReleaseInfo release) {
    ServiceList list = getServiceList(release.getNamespace());
    Map<String, List<Service>> nameToService = new HashMap<>();
    list.getItems().forEach(service -> nameToService.computeIfAbsent(service.getMetadata().getName(), n -> new ArrayList<>()).add(service));
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
      result = new KubernetesClientBuilder().build();
      if (!cachedValue.compareAndSet(null, result)) {
        return cachedValue.get();
      }
    }
    return result;
  }
}
