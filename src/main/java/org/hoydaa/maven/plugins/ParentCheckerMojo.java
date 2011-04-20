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

import java.util.ArrayList;
import java.util.List;

/**
 * Checks whether there is a newer parent POM for the project.
 *
 * @goal check
 * @phase process-sources
 * @requiresOnline true
 * @requiresProject true
 * @requiresDirectInvocation false
 */
public class ParentCheckerMojo extends AbstractMojo {

    /**
     * @parameter
     * @required
     */
    private List<org.hoydaa.maven.plugins.Artifact> checkArtifacts;

    /**
     * Enforce upgrade
     *
     * @parameter expression="${enforce.upgrade}"
     */
    private boolean enforceUpgrade;

    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     * @since 1.0-alpha-1
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @since 1.0-alpha-3
     */
    protected List<ArtifactRepository> remoteArtifactRepositories;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     * @since 1.0-alpha-1
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * @parameter expression="${localRepository}"
     * @readonly
     * @since 1.0-alpha-1
     */
    protected ArtifactRepository localRepository;

    /**
     * @component
     * @since 1.0-alpha-1
     */
    protected ArtifactFactory artifactFactory;

    public void execute() throws MojoExecutionException {
        Artifact parentArtifact = project.getParentArtifact();
        if (null == parentArtifact || checkArtifacts == null || !hasValidParent()) {
            getLog().debug("Parent " + parentArtifact + " is not in the list " + checkArtifacts + ", skipping...");

            return;
        }

        try {
            ArtifactVersion currentVersion = project.getParentArtifact().getSelectedVersion();
            List<ArtifactVersion> availableVersions = artifactMetadataSource.retrieveAvailableVersions(
                    artifactFactory.createParentArtifact("org.apache.maven.plugins",
                            "maven-parent-checker-plugin-parent", "1.0-SNAPSHOT"),
                    localRepository, remoteArtifactRepositories);
            List<ArtifactVersion> newVersions = getNewerVersions(currentVersion, availableVersions);
            if (newVersions.size() > 0) {
                getLog().warn("New versions available: ");
                for (ArtifactVersion version : newVersions) {
                    getLog().warn("                        " + version.toString());
                }

                if (enforceUpgrade) {
                    throw new MojoExecutionException(getWarningText(newVersions));
                } else {
                    getLog().info(getWarningText(newVersions));
                }
            }
        } catch (ArtifactMetadataRetrievalException e) {
            e.printStackTrace();
        } catch (OverConstrainedVersionException e) {
            e.printStackTrace();
        }
    }

    private String getWarningText(List<ArtifactVersion> newVersions) {
        return "Your parent POM " + project.getParentArtifact().toString() + " is " + newVersions.size()
                + " versions behind, you have to upgrade it to " + newVersions.get(newVersions.size() - 1) + ".";
    }

    private boolean hasValidParent() {
        for (org.hoydaa.maven.plugins.Artifact artifact : checkArtifacts) {
            if (artifact.getGroupId().equals(project.getParentArtifact().getGroupId())
                    && artifact.getArtifactId().equals(project.getParentArtifact().getArtifactId())) {
                return true;
            }
        }

        return false;
    }

    private List<ArtifactVersion> getNewerVersions(ArtifactVersion current, List<ArtifactVersion> availableVersions) {
        List<ArtifactVersion> newVersions = new ArrayList<ArtifactVersion>();
        for (ArtifactVersion version : availableVersions) {
            if (version.compareTo(current) > 0) {
                newVersions.add(version);
            }
        }

        return newVersions;
    }

    public void setCheckArtifacts(List<org.hoydaa.maven.plugins.Artifact> checkArtifacts) {
        this.checkArtifacts = checkArtifacts;
    }

    public void setEnforceUpgrade(boolean enforceUpgrade) {
        this.enforceUpgrade = enforceUpgrade;
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

}
