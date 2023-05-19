package org.honton.chas.helmrepo.maven.plugin;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;

public abstract class HelmRelease extends HelmGoal implements GlobalReleaseOptions, CommandOptions {

  private static final Pattern WARNING =
      Pattern.compile("\\[?(warning)]?:? ?(.+)", Pattern.CASE_INSENSITIVE);

  /** Values to be applied during upgrade. These are inline values formatted as yaml. */
  @Parameter @Getter String valueYaml;

  /** Value file to be applied during upgrade */
  @Parameter File valuesFile;

  /** Information about the kubernetes cluster */
  @Parameter @Getter KubernetesInfo kubernetes;

  /** List of releases to upgrade or uninstall */
  @Parameter List<ReleaseInfo> releases;

  @Parameter(
      defaultValue = "${project.build.directory}/helm-values",
      readonly = true,
      required = true)
  File helmValues;

  @Component RepositorySystem repoSystem;

  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
  RepositorySystemSession repoSession;

  @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
  List<RemoteRepository> remoteRepos;

  // work variables ...
  Path pwd; // current working directory
  Path templatePath;
  @Getter Path globalValuePath; // global values path
  @Getter Path globalValuesFile;
  AtomicReference<KubernetesClient> cachedClient = new AtomicReference<>();

  protected void doExecute() throws MojoExecutionException, IOException {
    if (!validateConfiguration()) {
      throw new MojoExecutionException("Invalid configuration");
    }
    initializeGlobalValuesFiles();

    String globalNamespace = kubernetes != null ? kubernetes.getNamespace() : null;
    for (ReleaseInfo release : getIterable(getReleasesInRequiredOrder())) {
      String namespace = release.namespace;
      if (namespace == null) {
        namespace = globalNamespace;
      }
      preHelmCommand(release, namespace);

      CommandLineGenerator generator =
          new CommandLineGenerator(this)
              .globalOptions(this, namespace)
              .releaseOptions(release, this);

      executeHelmCommand(generator.getCommand());
      postHelmCommand(release, namespace);
    }
  }

  protected void initializeGlobalValuesFiles() throws IOException {
    pwd = Path.of("").toAbsolutePath();

    templatePath = pwd.relativize(Files.createDirectories(helmValues.toPath()));
    if (valueYaml != null) {
      globalValuePath = releaseValues("_.yaml");
      if (globalValuePath != null) {
        Files.writeString(globalValuePath, valueYaml);
      }
    }
    if (valuesFile != null && releaseValues(valuesFile.getName()) != null) {
      if (valuesFile.canRead()) {
        globalValuesFile = pwd.relativize(valuesFile.toPath());
      } else {
        getLog().info("Ignoring unreadable values file " + valuesFile.getAbsolutePath());
      }
    }
  }

  protected boolean validateConfiguration() {
    if (releases == null) {
      return true;
    }
    long nErrors = releases.stream().filter(this::validateRelease).count();
    return nErrors == 0;
  }

  /** Each Release must have proper release name and chart */
  protected boolean validateRelease(ReleaseInfo release) {
    String chart = release.chart;
    if (chart == null) {
      getLog().error("Missing chart in " + release);
      return true;
    }
    if (release.name == null) {
      release.setName(ReleaseHelper.unversionedName(chart));
    }
    if (ReleaseHelper.isMavenArtifact(chart)) {
      try {
        release.setChart(localArtifact(chart));
      } catch (ArtifactResolutionException e) {
        getLog().error("Cannot find artifact " + chart);
        return true;
      }
    }
    return false;
  }

  void pumpLog(InputStream is, Consumer<String> lineConsumer) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      reader.lines().forEach(lineConsumer);
    } catch (IOException e) {
      lineConsumer.accept(e.getMessage());
    }
  }

  private void executeHelmCommand(List<String> command) throws MojoExecutionException, IOException {
    try {
      getLog().info(String.join(" ", command));
      Process process = new ProcessBuilder(command).start();

      ForkJoinPool pool = ForkJoinPool.commonPool();
      pool.execute(() -> pumpLog(process.getInputStream(), getLog()::info));
      pool.execute(() -> pumpLog(process.getErrorStream(), this::logLine));

      if (process.waitFor() != 0) {
        throw new MojoExecutionException("helm exit value: " + process.exitValue());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void logLine(String s) {
    Matcher matcher = WARNING.matcher(s);
    if (matcher.matches()) {
      getLog().warn(matcher.group(2));
    } else {
      getLog().error(s);
    }
  }

  LinkedList<ReleaseInfo> getReleasesInRequiredOrder() {
    if (releases == null) {
      return new LinkedList<>();
    }

    Map<String, ReleaseState> releaseToRequirements = new HashMap<>();

    releases.forEach(
        release -> {
          ReleaseState requirements =
              new ReleaseState(release, ReleaseHelper.asSet(release.requires));
          if (releaseToRequirements.put(release.name, requirements) != null) {
            throw new IllegalStateException("duplicate definition of " + release.name);
          }
        });

    // fill out dependents
    releaseToRequirements.forEach(
        (name, value) ->
            value
                .getRequires()
                .forEach(
                    require -> {
                      ReleaseState requirements = releaseToRequirements.get(require);
                      if (requirements == null) {
                        throw new IllegalArgumentException(
                            "Missing definition for require " + require + " on release " + name);
                      }
                      requirements.addDependent(require, value);
                    }));

    LinkedList<ReleaseInfo> releaseOrder = new LinkedList<>();

    for (; ; ) {
      List<ReleaseState> solved =
          releaseToRequirements.values().stream()
              .filter(ReleaseState::isSolved)
              .collect(Collectors.toList());
      long count =
          solved.stream()
              .filter(
                  requirement -> {
                    releaseOrder.add(requirement.getRelease());
                    releaseToRequirements.remove(requirement.getRelease().name);
                    return requirement.removeRequiresFromDependents();
                  })
              .count();
      if (count == 0) {
        if (!releaseToRequirements.isEmpty()) {
          throw new IllegalArgumentException(
              "Could not determine ordering for releases: "
                  + String.join(", ", releaseToRequirements.keySet()));
        }
        break;
      }
    }

    return releaseOrder;
  }

  /**
   * Fetch the artifact and return the local location
   *
   * @param chart The artifact to fetch
   * @return The local file location
   */
  private String localArtifact(String chart) throws ArtifactResolutionException {
    DefaultArtifact gav = new DefaultArtifact(chart);
    DefaultArtifact chartArtifact =
        new DefaultArtifact(gav.getGroupId(), gav.getArtifactId(), "tgz", gav.getVersion());
    ArtifactRequest request = new ArtifactRequest(chartArtifact, remoteRepos, null);
    return repoSystem
        .resolveArtifact(repoSession, request)
        .getArtifact()
        .getFile()
        .getAbsolutePath();
  }

  // override point
  protected void preHelmCommand(ReleaseInfo release, String namespace) {}

  // override point
  protected void postHelmCommand(ReleaseInfo release, String namespace) {}

  protected KubernetesClient getKubernetesClient() {
    KubernetesClient result = cachedClient.get();
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
      if (!cachedClient.compareAndSet(null, result)) {
        return cachedClient.get();
      }
    }
    return result;
  }

  @Override
  public String chartReference(ReleaseInfo info) {
    return info.chart;
  }

  @Override
  public void releaseOptions(ReleaseInfo release, List<String> command) throws IOException {}

  @Override
  public Iterable<ReleaseInfo> getIterable(LinkedList<ReleaseInfo> inOrder) {
    return inOrder;
  }

  @Override
  public Path releaseValues(String valuesFileName) {
    return templatePath.resolve(valuesFileName);
  }

  @Override
  public void createNamespace(List<String> command) {}
}
