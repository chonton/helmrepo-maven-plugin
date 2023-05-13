package org.honton.chas.helmrepo.maven.plugin;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class HelmRelease extends HelmGoal implements GlobalReleaseOptions {

  /**
   * List of releases to upgrade
   */
  @Parameter
  List<ReleaseInfo> releases;

  /**
   * Values to be applied during upgrade. This is formatted as yaml.
   */
  @Parameter
  @Getter
  String valueYaml;

  /**
   * Information about the kubernetes cluster
   */
  @Parameter
  @Getter
  KubernetesInfo kubernetes;

  /**
   * The entry point to Maven Artifact Resolver, i.e. the component doing all the work.
   */
  @Component
  RepositorySystem repoSystem;

  /**
   * The current repository/network configuration of Maven.
   */
  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
  RepositorySystemSession repoSession;

  /**
   * The project's remote repositories to use for the resolution.
   */
  @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
  List<RemoteRepository> remoteRepos;

  private static String unversionedName(String chart) {
    if (chart.endsWith(".tgz")) {
      int endIdx = chart.lastIndexOf('-');
      if (endIdx < 0) {
        throw new IllegalArgumentException(chart + " is not a proper versioned chart");
      }
      int firstIdx = chart.lastIndexOf('/');
      return chart.substring(firstIdx + 1, endIdx);
    }
    return chart;
  }

  private static Set<String> asSet(ReleaseInfo release) {
    String commaSeparated = release.getRequires();
    return commaSeparated != null
        ? new HashSet<>(Arrays.asList(commaSeparated.split("\\w*,\\w*")))
        : Set.of();
  }

  protected final void doExecute() throws MojoFailureException, MojoExecutionException {
    for (ReleaseInfo release : getIterable(getReleasesInRequiredOrder())) {
      CommandLineGenerator commandLineGenerator = getCommandLineGenerator(release);
      commandLineGenerator.appendRelease(release);
      commandLineGenerator.appendGlobalReleaseOptions(this);
      executeHelmCommand(commandLineGenerator.getCommand());
    }
  }

  protected abstract Iterable<ReleaseInfo> getIterable(LinkedList<ReleaseInfo> inOrder);

  protected abstract CommandLineGenerator getCommandLineGenerator(ReleaseInfo release);

  void pumpLog(InputStream is, Consumer<String> lineConsumer) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      reader.lines().forEach(lineConsumer);
    } catch (IOException e) {
      lineConsumer.accept(e.getMessage());
    }
  }

  private void executeHelmCommand(List<String> command) throws MojoExecutionException {
    try {
      Process process = new ProcessBuilder(command).start();

      ForkJoinPool pool = ForkJoinPool.commonPool();
      pool.execute(() -> pumpLog(process.getInputStream(), getLog()::info));
      pool.execute(() -> pumpLog(process.getErrorStream(), getLog()::error));

      if (process.waitFor() != 0) {
        throw new MojoExecutionException('`' + String.join(" ", command) + '`');
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException(e);
    } catch (IOException e) {
      throw new MojoExecutionException(e);
    }
  }

  LinkedList<ReleaseInfo> getReleasesInRequiredOrder() {
    if (releases == null) {
      return new LinkedList<>();
    }

    Map<String, ReleaseState> releaseToRequirements = new HashMap<>();

    releases.forEach(
        release -> {
          canonicalize(release);
          ReleaseState requirements = new ReleaseState(release, asSet(release));
          if (releaseToRequirements.put(release.getName(), requirements) != null) {
            throw new IllegalStateException("duplicate definition of " + release.getName());
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
                    releaseToRequirements.remove(requirement.getRelease().getName());
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
   * Each Release must have proper release name and chart
   */
  private void canonicalize(ReleaseInfo release) {
    String chart = release.getChart();
    if (chart == null) {
      throw new IllegalArgumentException("Release must have chart information");
    }
    release.setChart(replaceMavenArtifactWithLocalFile(chart));
    if (release.getName() == null) {
      release.setName(unversionedName(chart));
    }
  }

  private String replaceMavenArtifactWithLocalFile(String chart) {
    String[] parts = chart.split(":");
    if (parts.length == 3) {
      // maven artifact
      return localArtifact(chart);
    } else if (parts.length > 3) {
      throw new IllegalArgumentException(chart + " is not a valid chart specification");
    }
    return chart;
  }

  /**
   * Fetch the artifact and return the local location
   *
   * @param chart
   * @return The local file location
   */
  @SneakyThrows
  private String localArtifact(String chart) {
    ArtifactRequest request = new ArtifactRequest();
    request.setArtifact(new DefaultArtifact(chart));
    request.setRepositories(remoteRepos);

    return repoSystem.resolveArtifact(repoSession, request).getArtifact().getFile().getAbsolutePath();
  }
}
