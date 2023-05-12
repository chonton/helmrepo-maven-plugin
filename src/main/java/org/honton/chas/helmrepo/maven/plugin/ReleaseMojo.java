package org.honton.chas.helmrepo.maven.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.Getter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class ReleaseMojo extends AbstractMojo implements GlobalReleaseOptions {

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  MavenSession session;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  MavenProject mavenProject;

  /** Skip upgrade */
  @Parameter(property = "helm.skip", defaultValue = "false")
  boolean skip;

  /** List of releases to upgrade */
  @Parameter List<Release> releases;

  /** Values to be applied during upgrade. This is formatted as yaml. */
  @Parameter @Getter String valueYaml;

  /** Information about the kubernetes cluster */
  @Parameter @Getter Kubernetes kubernetes;

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

  private static Set<String> asSet(Release release) {
    String commaSeparated = release.getRequires();
    return commaSeparated != null
        ? new HashSet<>(Arrays.asList(commaSeparated.split("\\w*,\\w*")))
        : Set.of();
  }

  public final void execute() throws MojoFailureException, MojoExecutionException {
    if (skip) {
      getLog().info("skipping helm");
    } else {
      for (Release release : getReleasesInRequiredOrder()) {
        CommandLineGenerator commandLineGenerator = getCommandLineGenerator(release);
        commandLineGenerator.appendRelease(release);
        commandLineGenerator.appendGlobalReleaseOptions(this);
        executeHelmCommand(commandLineGenerator.getCommand());
      }
    }
  }

  public abstract CommandLineGenerator getCommandLineGenerator(Release release);

 void pumpLog(InputStream is, Consumer<String> lineConsumer) {
   try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
     reader.lines().forEach(lineConsumer);
   } catch (IOException e) {
     lineConsumer.accept(e.getMessage());
   }
 }

  private void executeHelmCommand(String command) throws MojoExecutionException {
    try {
      Process process = new ProcessBuilder(command).start();

      ForkJoinPool pool = ForkJoinPool.commonPool();
      pool.execute( () -> pumpLog(process.getInputStream(), getLog()::info) );
      pool.execute( () -> pumpLog(process.getErrorStream(), getLog()::error) );

      if (process.waitFor() != 0) {
        throw new MojoExecutionException(command);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException(e);
    } catch (IOException e) {
      throw new MojoExecutionException(e);
    }
  }

  private List<Release> getReleasesInRequiredOrder() {
    Map<String, ReleaseRequirements> releaseToRequirements = new HashMap<>();

    releases.forEach(
        release -> {
          canonicalize(release);
          ReleaseRequirements requirements = new ReleaseRequirements(release, asSet(release));
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
                      ReleaseRequirements requirements = releaseToRequirements.get(require);
                      if (requirements == null) {
                        throw new IllegalArgumentException(
                            "Missing definition for require " + require + " on release " + name);
                      }
                      requirements.addDependent(require, value);
                    }));

    List<Release> releaseOrder = new ArrayList<>();

    for (; ; ) {
      List<ReleaseRequirements> solved =
          releaseToRequirements.values().stream()
              .filter(ReleaseRequirements::isSolved)
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

  /** Each Release must have proper release name and chart */
  private Release canonicalize(Release release) {
    String chart = release.getChart();
    if (chart == null) {
      throw new IllegalArgumentException("Release must have chart information");
    }
    release.setChart(replaceMavenArtifactWithLocalFile(chart));
    if (release.getName() == null) {
      release.setName(unversionedName(chart));
    }
    return release;
  }

  private String replaceMavenArtifactWithLocalFile(String chart) {
    String[] parts = chart.split(":");
    if (parts.length == 2) {
      // maven artifact
      return localArtifact(chart);
    } else if (parts.length > 2) {
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
  private String localArtifact(String chart) {
    // TODO !!!!!
    return chart;
  }
}
