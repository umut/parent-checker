Plugin to force parent POM updates for using projects.

Releasing and having the using projects upgrade to the new version of a parent POM is always a burden. You have send
an email to the developers of the projects requesting an upgrade on the parent POM but it simply does not reach it's
destination, goes to junk or simply no one cares :)

As developers we are using the same build tool (maven) and we can use our build tool to pass the communiqué.

The plugin by default attaches itself to validate lifecycle phase to check parent POM updates if the user does not
explicitly specifies an execution in the plugin configuration. What the plugin does it simply;
 - Checks whether the current building project has a parent POM
 - If so checks whether it is one the parent POMs that we want to check for updates
 - If so checks whether there is a newer version
 - If so depending on the plugin configuration, it makes the build fail or print a warning message to let the developer be aware of it

###Configuration
<plugin>
    <groupId>org.hoydaa.maven.plugins</groupId>
    <artifactId>parent-checker-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <!--<executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>-->
    <configuration>
        <!-- If true makes the build fail when there is a newer version for the parent POM -->
        <forceUpgrade>false</forceUpgrade>
        <!-- The parent POM artifacts to check for update, you can make the plugin check for more than one parent POM -->
        <checkArtifacts>
            <artifact>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-parent-checker-plugin-parent</artifactId>
            </artifact>
        </checkArtifacts>
    </configuration>
</plugin>

###Usage

As said the plugin attaches itself to the validate lifecycle phase, meaning; it is enough that you define the plugin
within your parent POM. Bu you can also the the plugin explicitly by executing the following line.

mvn org.hoydaa.maven.plugins:parent-checker-maven-plugin:check

Or you can add org.hoydaa.maven.plugins as a pluginGroup configuration in your local maven settings.xml and use the
plugin prefix to run it which is less verbose.

    <pluginGroups>
        <pluginGroup>org.hoydaa.maven.plugins</pluginGroup>
    </pluginGroups>

    mvn parent-checker:check