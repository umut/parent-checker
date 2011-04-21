package org.hoydaa.maven.plugins;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks whether there is a newer parent POM for the project.
 *
 * @author Umut Utkan
 * @goal check
 * @phase validate
 * @threadSafe
 * @requiresOnline true
 * @requiresProject true
 * @requiresDirectInvocation false
 */
public class ParentCheckerMojo extends AbstractMojo {

    private static final String FORCE_UPGRADE = "force.upgrade";


    /**
     * Artifacts to be checked
     *
     * @parameter
     * @required
     */
    private List<org.hoydaa.maven.plugins.Artifact> checkArtifacts;

    /**
     * Force upgrade
     *
     * @parameter expression="${force.upgrade}"
     */
    private boolean forceUpgrade;

    /**
     * The Maven Project
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     * @since 1.0-alpha-1
     */
    private MavenProject project;

    /**
     * Remote repositories in use
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @since 1.0-alpha-3
     */
    protected List<ArtifactRepository> remoteArtifactRepositories;

    /**
     * The artifact metadata source to use
     *
     * @component
     * @required
     * @readonly
     * @since 1.0-alpha-1
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * Local repository
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @since 1.0-alpha-1
     */
    protected ArtifactRepository localRepository;

    /**
     * Artifact factory to use
     *
     * @component
     * @since 1.0-alpha-1
     */
    protected ArtifactFactory artifactFactory;

    /**
     * Maven project builder to use
     *
     * @component
     */
    protected MavenProjectBuilder mavenProjectBuilder;


    public void execute() throws MojoExecutionException {
        Artifact parentArtifact = project.getParentArtifact();
        if (null == parentArtifact || checkArtifacts == null || !hasValidParent()) {
            getLog().debug("Parent " + parentArtifact + " is not in the list " + checkArtifacts + ", skipping...");

            return;
        }

        try {
            ArtifactVersion currentVersion = project.getParentArtifact().getSelectedVersion();
            List<ArtifactVersion> availableVersions = artifactMetadataSource.retrieveAvailableVersions(
                    artifactFactory.createParentArtifact(parentArtifact.getGroupId(),
                            parentArtifact.getArtifactId(), parentArtifact.getVersion()),
                    localRepository, remoteArtifactRepositories);
            List<ArtifactVersion> newVersions = getNewerVersions(currentVersion, availableVersions);
            if (newVersions.size() > 0) {
                boolean forcedUpdateExists = false;

                getLog().warn("New versions available for your parent POM '" + parentArtifact.toString() + "'!");
                for (ArtifactVersion version : newVersions) {
                    boolean forced = isForced(version);
                    forcedUpdateExists = forcedUpdateExists || forced;
                    getLog().warn(version.toString() + " (" + (forced ? "FORCED" : "not forced") + ")");
                }

                if (forceUpgrade) {
                    throw new MojoExecutionException(getWarningText(newVersions));
                } else if (forcedUpdateExists) {
                    throw new MojoExecutionException(getWarningText(newVersions) + " You have to upgrade your parent POM to the latest forced update at least!");
                } else {
                    getLog().warn(getWarningText(newVersions));
                }
            }
        } catch (ArtifactMetadataRetrievalException e) {
            e.printStackTrace();
        } catch (OverConstrainedVersionException e) {
            e.printStackTrace();
        } catch (ProjectBuildingException e) {
            e.printStackTrace();
        }
    }

    //returns if artifact is forced for update
    private boolean isForced(ArtifactVersion version) throws ProjectBuildingException {
        return Boolean.parseBoolean((String) getProjectForParent(version).getProperties().get(FORCE_UPGRADE));
    }

    //returns project for the parent artifact
    private MavenProject getProjectForParent(ArtifactVersion version) throws ProjectBuildingException {
        Artifact parentTemp = artifactFactory.createParentArtifact(project.getParentArtifact().getGroupId(),
                project.getParentArtifact().getArtifactId(), version.toString());
        return mavenProjectBuilder.buildFromRepository(parentTemp, remoteArtifactRepositories, localRepository);
    }

    //returns a warning message to display in logs
    private String getWarningText(List<ArtifactVersion> newVersions) {
        return "Your parent POM " + project.getParentArtifact().toString() + " is " + newVersions.size()
                + " versions behind, you have to upgrade it to " + newVersions.get(newVersions.size() - 1) + ".";
    }

    //checks if the parent is in the list of artifacts to check
    private boolean hasValidParent() {
        for (org.hoydaa.maven.plugins.Artifact artifact : checkArtifacts) {
            if (artifact.getGroupId().equals(project.getParentArtifact().getGroupId())
                    && artifact.getArtifactId().equals(project.getParentArtifact().getArtifactId())) {
                return true;
            }
        }

        return false;
    }

    //extracts newer/valid versions from a list of available versions
    private List<ArtifactVersion> getNewerVersions(ArtifactVersion current, List<ArtifactVersion> availableVersions) {
        List<ArtifactVersion> newVersions = new ArrayList<ArtifactVersion>();
        for (ArtifactVersion version : availableVersions) {
            if (StringUtils.isEmpty(version.getQualifier()) && version.compareTo(current) > 0) {
                newVersions.add(version);
            }
        }

        return newVersions;
    }

    public void setCheckArtifacts(List<org.hoydaa.maven.plugins.Artifact> checkArtifacts) {
        this.checkArtifacts = checkArtifacts;
    }

    public void setForceUpgrade(boolean forceUpgrade) {
        this.forceUpgrade = forceUpgrade;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public void setRemoteArtifactRepositories(List<ArtifactRepository> remoteArtifactRepositories) {
        this.remoteArtifactRepositories = remoteArtifactRepositories;
    }

    public void setArtifactMetadataSource(ArtifactMetadataSource artifactMetadataSource) {
        this.artifactMetadataSource = artifactMetadataSource;
    }

    public void setLocalRepository(ArtifactRepository localRepository) {
        this.localRepository = localRepository;
    }

    public void setArtifactFactory(ArtifactFactory artifactFactory) {
        this.artifactFactory = artifactFactory;
    }

    public MavenProjectBuilder getMavenProjectBuilder() {
        return mavenProjectBuilder;
    }

    public void setMavenProjectBuilder(MavenProjectBuilder mavenProjectBuilder) {
        this.mavenProjectBuilder = mavenProjectBuilder;
    }

}
