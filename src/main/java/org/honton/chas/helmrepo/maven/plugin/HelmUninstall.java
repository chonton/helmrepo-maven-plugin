package org.honton.chas.helmrepo.maven.plugin;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.dsl.AnyNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/** Uninstall helm release(s) */
@Mojo(name = "uninstall", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, threadSafe = true)
public class HelmUninstall extends HelmRelease {

  /** Location for pod logs */
  @Parameter(defaultValue = "${project.build.directory}/pod-logs")
  File podLogs;

  @Override
  protected void initializeGlobalValuesFiles() {
    pwd = Path.of("").toAbsolutePath();
  }

  @Override
  public void addSubCommand(List<String> commandLine) {
    commandLine.add("uninstall");
  }

  @Override
  public String chartReference(ReleaseInfo info) {
    return null;
  }

  @Override
  public Path releaseValues(String valuesFileName) {
    return null;
  }

  @Override
  public Iterable<ReleaseInfo> getIterable(LinkedList<ReleaseInfo> inOrder) {
    return inOrder::descendingIterator;
  }

  @Override
  protected void preHelmCommand(ReleaseInfo release, String namespace) {
    List<String> logs = release.logs;
    if (logs != null) {
      logs.forEach(ps -> saveLogs(PodSelector.of(ps, namespace)));
    } else {
      getLog().info("No Logs specified");
    }
  }

  private void saveLogs(PodSelector selector) {
    long count = query(selector).filter(this::savePodLogs).count();
    if (count == 0) {
      getLog().warn("No pods for " + selector);
    }
  }

  private boolean savePodLogs(PodResource podResource) {
    ObjectMeta meta = podResource.item().getMetadata();
    Path logDir = podLogs.toPath().resolve(meta.getNamespace());
    Path logPath = logDir.resolve(meta.getName() + ".log");
    try {
      Files.createDirectories(logDir);
      Files.copy(podResource.getLogInputStream(), logPath, StandardCopyOption.REPLACE_EXISTING);
      getLog().info("Saved " + pwd.relativize(logPath));
      return true;
    } catch (IOException ioException) {
      getLog().error("Failed save to " + pwd.relativize(logPath), ioException);
      return false;
    }
  }

  private Stream<PodResource> query(PodSelector selector) {
    MixedOperation<Pod, PodList, PodResource> pods = getKubernetesClient().pods();
    AnyNamespaceOperation<Pod, PodList, PodResource> ans =
        selector.getNamespace() != null
            ? pods.inNamespace(selector.getNamespace())
            : pods.inAnyNamespace();
    return ans.resources()
        .filter(pr -> pr.item().getMetadata().getName().startsWith(selector.getPod()));
  }
}
