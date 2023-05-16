package org.honton.chas.helmrepo.maven.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.tar.TarGZipArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.InterpolatorFilterReader;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.PrefixedValueSourceWrapper;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.yaml.snakeyaml.Yaml;

/** Package a helm chart and attach as secondary artifact */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class HelmPackage extends HelmGoal {
  /** Attach helm chart as a secondary artifact */
  @Parameter(defaultValue = "true")
  boolean attach;

  /** Interpolate chart with values from maven build properties */
  @Parameter(defaultValue = "true")
  boolean filter;

  /** Directory which holds an unpacked chart. Chart must be named ${project.artifactId} */
  @Parameter(defaultValue = "src/helm")
  File helmDir;

  @Parameter(
      defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}.tgz",
      required = true,
      readonly = true)
  File destFile;

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  MavenSession session;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  MavenProject project;

  @Component MavenProjectHelper projectHelper;

  protected final void doExecute() throws IOException {
    if (helmDir.isDirectory()) {
      File chartDir = new File(helmDir, project.getArtifactId());
      if (chartDir.isDirectory()) {
        checkChartFile(chartDir);
        packageChart(helmDir);
        return;
      }
    }
    getLog().info("Helm chart not found, skipping 'package'");
  }

  private void checkChartFile(File chartDir) throws IOException {
    File chartFile = new File(chartDir, "Chart.yaml");
    if (!chartFile.canRead()) {
      String error = "Cannot read " + chartFile;
      getLog().error(error);
      throw new IOException(error);
    }

    InputStream is = new FileInputStream(chartFile);
    if (filter) {
      is = createStream(is);
    }
    Map<String, String> chart = new Yaml().load(is);
    if (!project.getArtifactId().equals(chart.get("name"))) {
      String error = "Chart name does not equal the required value of " + project.getArtifactId();
      getLog().error(error);
      throw new IOException(error);
    }
    SemVer.valueOf(chart.get("version"));
  }

  private void packageChart(File helmDir) throws IOException {
    DefaultFileSet fileSet = DefaultFileSet.fileSet(helmDir);
    fileSet.setIncludes(new String[] {project.getArtifactId() + "/**/*.*"});
    if (filter) {
      fileSet.setStreamTransformer((r, is) -> createStream(is));
    }

    TarGZipArchiver archiver = new TarGZipArchiver();
    archiver.addFileSet(fileSet);
    archiver.setDestFile(destFile);
    archiver.createArchive();

    if ("tgz".equals(project.getPackaging())) {
      project.getArtifact().setFile(destFile);
    } else if (attach) {
      projectHelper.attachArtifact(project, "tgz", destFile);
    }
  }

  private InputStream createStream(InputStream inputStream) {
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    return new ReaderInputStream(
        new InterpolatorFilterReader(reader, createInterpolator()), StandardCharsets.UTF_8);
  }

  private PropertiesBasedValueSource sessionSource() {
    File basedir =
        session.getRepositorySession().getLocalRepositoryManager().getRepository().getBasedir();

    Properties properties = new Properties();
    properties.setProperty("settings.localRepository", basedir.toString());

    properties.putAll(session.getSystemProperties());
    properties.putAll(session.getUserProperties());

    return new PropertiesBasedValueSource(properties);
  }

  private PrefixedValueSourceWrapper envSource() {
    return new PrefixedValueSourceWrapper(
        new AbstractValueSource(false) {
          @Override
          public Object getValue(String expression) {
            return System.getenv(expression);
          }
        },
        "env");
  }

  private PrefixedValueSourceWrapper projectSource() {
    return new PrefixedValueSourceWrapper(new ObjectBasedValueSource(project), "project");
  }

  private PrefixedValueSourceWrapper projectPropertiesSource() {
    return new PrefixedValueSourceWrapper(
        new PropertiesBasedValueSource(project.getProperties()), "project.properties", true);
  }

  private Interpolator createInterpolator() {
    StringSearchInterpolator interpolator = new StringSearchInterpolator();
    interpolator.setEscapeString("\\");
    interpolator.addValueSource(envSource());
    interpolator.addValueSource(sessionSource());
    interpolator.addValueSource(projectSource());
    interpolator.addValueSource(projectPropertiesSource());
    return interpolator;
  }
}
