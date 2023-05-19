# helmrepo-maven-plugin

Use maven repository to store helm charts. This has three goals:

1. Package helm chart and add as a secondary artifact for build
2. Upgrade (install) helm chart release(s)
3. Uninstall helm chart release(s)

# Rationale

Deploy your containers using helm charts to [docker desktop](https://docs.docker.com/desktop/kubernetes/) or
[minikube](https://minikube.sigs.k8s.io/docs/) for integration testing. Build the containers using
[docker-maven-plugin](https://dmp.fabric8.io/), [jib](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin),
or [buildpacks](https://github.com/paketo-buildpacks/maven). Start k8s pods/deployments/services
during **pre-integration-test** phase. Use [failsafe](https://maven.apache.org/surefire/maven-failsafe-plugin/) to run
integration tests during **integration-test** phase. Stop k8s pods/deployments/services during **post-integration-test**
phase.

# Plugin

Plugin reports available at [plugin info](https://chonton.github.io/helmrepo-maven-plugin/0.0.3/plugin-info.html).

## Upgrade Goal

The [upgrade](https://chonton.github.io/helmrepo-maven-plugin/0.0.3/upgrade.html) goal binds by default to the
**pre-integration-test** phase. This goal will execute `helm upgrade --install` for each release. If the release name is
not specified, the name will be derived from the chart name.

### Global Configuration

|                  Parameter |          Default          | Description                                                   |
|---------------------------:|:-------------------------:|:--------------------------------------------------------------|
|         kubernetes.context |     *kubectl default*     | Name of the kubectl context to use                            |
| kubernetes.createNamespace |           true            | Create namespaces if not present                              |
|       kubernetes.namespace | *kubectl context default* | Namespace for un-scoped kubernetes resources                  |
|                  valueYaml |             -             | Global values to be applied during upgrade, formatted as yaml |

### Per-Release Configuration

| Parameter |          Default          | Description                                                                |
|----------:|:-------------------------:|:---------------------------------------------------------------------------|
|     chart |        *required*         | Name of the chart                                                          |
|      name | *Un-versioned chart name* | Name of the release                                                        |
|  requires |             -             | Comma separated list of releases that must be deployed before this release |
| namespace |    *global namespace*     | Namespace for un-scoped kubernetes resources                               |
| nodePorts |             -             | Set Maven property from specified K8s service/port NodePort                |
| valueYaml |             -             | Values to be applied to release, formatted as yaml                         |
|      wait |            300            | Number of seconds to wait for successful deployment                        |

#### Chart Name

A chart name can be any of the following:

1. Maven GAV coordinate: *org.honton.chas:test-reports:1.3.4*
2. URL: *https://&ZeroWidthSpace;repo.maven.apache.org/maven2/org/honton/chas/test-reports/1.3.4/test-reports-1.3.4.tgz*
3. Path to an unpacked chart directory: *src/helm/superfantastic*
4. Path to a packaged chart: *superfantastic-44.12.3.tgz*
5. Chart reference: *repository/chartname*
6. OCI registry URL: *oci://example.com/charts/nginx*

#### Service NodePorts

Kubernetes has a network mapping of host ports to pod ports. During integration test, we need to use the host port
instead of the target port. With NodePort and LoadBalancer service types, the nodePort is unlikely to equal targetPort
value. After each release is upgraded, the **upgrade** goal will set specified maven properties to the assigned nodePort
values.

## Uninstall Goal

The [uninstall](https://chonton.github.io/helmrepo-maven-plugin/0.0.3/uninstall.html) goal binds by default to the
**post-integration-test** phase. This goal will execute `helm uninstall` for each release. Configuration is similar to
the **install** goal; the **valueYaml** and **servicePorts** parameters are ignored.

## Template Goal

The [template](https://chonton.github.io/helmrepo-maven-plugin/0.0.3/template.html) goal binds by default to the
**verify** phase. This goal will expand the chart source. Content will be
[filtered](https://maven.apache.org/plugins/maven-resources-plugin/examples/filter.html) using maven properties.
Parameters are same as for the `upgrade` goal with the addition of the following:

|   Parameter |             Default             | Description                   |
|------------:|:-------------------------------:|:------------------------------|
| templateDir | ${project.build.directory}/helm | Directory for expanded charts |

## Package Goal

The [package](https://chonton.github.io/helmrepo-maven-plugin/0.0.3/package.html) goal binds by default to the
**package** phase. This goal will create a helm package from the chart source. Content will be
[filtered](https://maven.apache.org/plugins/maven-resources-plugin/examples/filter.html) using maven properties.
Resulting *.tgz* artifact is attached as a secondary artifact for the build. The helm package will be installed in the
local maven repository during the **install** phase and deployed to the remote maven repository during the **deploy**
phase.

### Configuration

| Parameter | Default  | Description                                                     |
|----------:|:--------:|:----------------------------------------------------------------|
|    attach |   true   | Attach helm package as a secondary artifact of the build        |
|    filter |   true   | Interpolate chart contents, expanding *${variable}* expressions |
|   helmDir | src/helm | Directory holding an unpacked chart named ${project.artifactId} |

### Helm Chart Name Limitations
The chart within helmDir must equal ${project.artifactId}.

## tgz Packaging Extension

This plugin can also be used as an extension that packages, installs, and deploys the **tgz** packaging type. Just set
the top level **<packaging>** element to **tgz** and
[register](https://maven.apache.org/guides/mini/guide-using-extensions.html) this plugin as an extension.

# Examples

## Typical Use

```xml

<build>
  <pluginManagement>
    <plugins>
      <plugin>
        <groupId>org.honton.chas</groupId>
        <artifactId>helmrepo-maven-plugin</artifactId>
        <version>0.0.3</version>
      </plugin>
    </plugins>
  </pluginManagement>

  <plugins>
    <plugin>
      <groupId>org.honton.chas</groupId>
      <artifactId>helmrepo-maven-plugin</artifactId>
      <executions>
        <execution>
          <goals>
            <goal>package</goal>
            <goal>upgrade</goal>
            <goal>uninstall</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <valueYaml><![CDATA[
name: globalValue
]]>
        </valueYaml>
        <releases combine.children="append">
          <release>
            <chart>org.honton.chas:test-reports:1.3.4</chart>
            <valueYaml><![CDATA[
name: releaseValue
nested:
  list:
  - one
  - two
]]>
            </valueYaml>
            <nodePorts>
              <nodePort>
                <portName>http</portName>
                <propertyName>report.port</propertyName>
                <serviceName>test-reports</serviceName>
              </nodePort>
            </nodePorts>
          </release>
          <release>
            <name>report-job</name>
            <namespace>report</namespace>
            <requires>test-reports</requires>
            <chart>src/helm/${project.artifactId}</chart>
          </release>
        </releases>
      </configuration>
    </plugin>
  </plugins>
</build>
```

## Use as a packaging extension

```xml

<project>
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.example.helm</groupId>
  <artifactId>chart</artifactId>
  <packaging>tgz</packaging>

  <build>
    <extensions>
      <extension>
        <groupId>org.honton.chas</groupId>
        <artifactId>helmrepo-maven-plugin</artifactId>
        <version>0.0.2</version>
      </extension>
    </extensions>
  </build>

</project>
```

# Common Errors

## Incorrectly Named Chart

If the **package** goal fails with the following error, your chart directory is probably empty.

```text
[ERROR] Failed to execute goal org.honton.chas:helmrepo-maven-plugin:0.0.3:package (tutorial) on project tutorial: Execution tutorial of goal org.honton.chas:helmrepo-maven-plugin:0.0.3:package failed: archive cannot be empty -> [Help 1]
```
