###parent-checker-maven-plugin

Plugin to force for parent POM updates for using projects.

After release and having the using projects upgrade to the new version of their parent `POM`  is always a burden. You
have to send an email to the developers of the projects requesting an upgrade on the parent `POM` but the mail simply
does not reach it's destination, goes to junk or simply no one cares about it :)

As developers we use the same build tool (`maven`) and so we can also use the same tool to pass the *communiqué*.

By default the plugin attaches itself to `validate` life-cycle phase to check parent `POM` updates if the user did not
explicitly specify an `execution` in the plugin configuration. The plugin simply

 - Checks whether the current building project has a parent `POM`
 - If so, checks whether it is one of the parent `POM`s that we care (according to the given checkArtifacts config)
 - If so, checks whether there is a newer version
 - If so, depending on the plugin configuration, it makes the build fail or print a warning message to let the developer be aware of it

###Configuration

    :::xml
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

####Options

- **forceUpgrade**: If true, makes the plugin fail when a newer version for the parent `POM` is available either in the local repository or in one of the remote repositories.
- **checkArtifacts**: Set the parent `POM` artifacts that you want to be checked by the plugin. You can set more than one.

Another option to make the build fail is to have a property named `force.upgrade` within the released parent `POM`. In
this case no matter what is configured in the plugin configuration --regarding forceUpgrade--, if the plugin finds
`force.upgrade=true` within any released `POM` file it will make the build fail since it is assumed that you have a
SPECIFIC VERSION that you want to force for update. So if you want a specific version to force for update, just add a
property named `force.upgrade` in it before releasing like the following example.

    :::xml
    <project>
    ....
        <properties>
            <force.upgrade>true</force.upgrade>
            ....
        </properties>
    ....
    </project>

###Usage

As said, the plugin attaches itself to the `validate` life-cycle phase, meaning; it is enough that you define the plugin
within your parent `POM`. But you can also run the plugin explicitly by executing the following line.

    mvn org.hoydaa.maven.plugins:parent-checker-maven-plugin:check

Or you can define `org.hoydaa.maven.plugins` as a `pluginGroup` in your local maven settings.xml and use the
plugin prefix to run it which is less verbose.

    :::xml
    <pluginGroups>
        <pluginGroup>org.hoydaa.maven.plugins</pluginGroup>
    </pluginGroups>

In this case you just have to use the plugin prefix and the goal to run it, no `groupId` or `artifactId` is needed.

    mvn parent-checker:check

###Outputs

Depending on the situation the plugin behaves differently and produces different outputs.

When the plugin is configured with `force.upgrade=false` and there are two new versions, it just displays a warning log.

    [WARNING] New versions available for your parent POM org.hoydaa.maven.plugins:test-parent:pom:1.1!
    [WARNING] 1.2 (not forced)
    [WARNING] 1.3 (not forced)
    [WARNING] Your parent POM org.hoydaa.maven.plugins:test-parent:pom:1.1 is 2 versions behind, you have to upgrade it to 1.3!

When the plugin is configured with `force.upgrade=true` and there are two new versions, it makes the build fail.

    [WARNING] New versions available for your parent POM org.hoydaa.maven.plugins:test-parent:pom:1.1!
    [WARNING] 1.2 (not forced)
    [WARNING] 1.3 (not forced)
    [INFO] ------------------------------------------------------------------------
    [ERROR] BUILD ERROR
    [INFO] ------------------------------------------------------------------------
    [INFO] Your parent POM org.hoydaa.maven.plugins:test-parent:pom:1.1 is 2 versions behind, you have to upgrade it to 1.3!

When there are three versions one of which is forced (meaning there is a property force.upgrade=true within the released parent POM, it makes the build fail even the forceUpgrade is false for the plugin.

    [WARNING] New versions available for your parent POM org.hoydaa.maven.plugins:test-parent:pom:1.1!
    [WARNING] 1.2 (not forced)
    [WARNING] 1.3 (not forced)
    [WARNING] 1.4 (FORCED)
    [INFO] ------------------------------------------------------------------------
    [ERROR] BUILD ERROR
    [INFO] ------------------------------------------------------------------------
    [INFO] Your parent POM org.hoydaa.maven.plugins:test-parent:pom:1.1 is 3 versions behind, you have to upgrade it to 1.4! You have to upgrade your parent POM to the latest forced update at least!