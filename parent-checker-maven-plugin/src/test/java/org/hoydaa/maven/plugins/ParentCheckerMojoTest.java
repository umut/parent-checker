package org.hoydaa.maven.plugins;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Umut Utkan
 */
public class ParentCheckerMojoTest {

    private ParentCheckerMojo mojo;

    private Artifact parent;

    private MavenProject project;

    private ArtifactFactory artifactFactory;

    @Before
    public void setUp() throws OverConstrainedVersionException, ProjectBuildingException {
        mojo = new ParentCheckerMojo();

        parent = EasyMock.createMock(Artifact.class);
        EasyMock.expect(parent.getGroupId()).andReturn("com.foo").anyTimes();
        EasyMock.expect(parent.getArtifactId()).andReturn("parent").anyTimes();
        EasyMock.expect(parent.getSelectedVersion()).andReturn(new DefaultArtifactVersion("1.0")).anyTimes();
        EasyMock.expect(parent.getVersion()).andReturn("1.0").anyTimes();
        EasyMock.replay(parent);
        project = new MavenProject();
        project.setParentArtifact(parent);

        artifactFactory = EasyMock.createMock(ArtifactFactory.class);
        EasyMock.expect(artifactFactory.createParentArtifact(EasyMock.isA(String.class), EasyMock.isA(String.class), EasyMock.isA(String.class))).andAnswer(
                new IAnswer<Artifact>() {
                    public Artifact answer() {
                        Object[] args = EasyMock.getCurrentArguments();
                        Artifact artifact = EasyMock.createMock(Artifact.class);
                        EasyMock.expect(artifact.getGroupId()).andReturn(args[0].toString()).anyTimes();
                        EasyMock.expect(artifact.getArtifactId()).andReturn(args[1].toString()).anyTimes();
                        EasyMock.expect(artifact.getVersion()).andReturn(args[2].toString()).anyTimes();
                        EasyMock.replay(artifact);

                        return artifact;
                    }
                }
        ).anyTimes();
        EasyMock.replay(artifactFactory);

        mojo.setProject(project);
        mojo.setArtifactFactory(artifactFactory);
    }

    public void tearDown() {
        EasyMock.verify(parent);
        EasyMock.verify(artifactFactory);
    }

    @Test
    public void shouldDoNothingIfParentIsNotInTheList() throws MojoExecutionException {
        mojo.execute();
        List check = new ArrayList();
        check.add(new org.hoydaa.maven.plugins.Artifact("com.foo", "does-not-matter"));
        mojo.setCheckArtifacts(check);
        mojo.execute();
    }

    @Test
    public void shouldFailBuildIfForceUpgradeSetTrue() throws ArtifactMetadataRetrievalException, ProjectBuildingException {
        List list = new ArrayList();
        list.add(new DefaultArtifactVersion("2.0"));
        ArtifactMetadataSource artifactMetadataSource = EasyMock.createMock(ArtifactMetadataSource.class);
        EasyMock.expect(artifactMetadataSource.retrieveAvailableVersions(EasyMock.isA(Artifact.class),
                EasyMock.isA(ArtifactRepository.class), EasyMock.isA(List.class))).andReturn(list).anyTimes();
        EasyMock.replay(artifactMetadataSource);

        List check = new ArrayList();
        check.add(new org.hoydaa.maven.plugins.Artifact("com.foo", "parent"));

        MavenProjectBuilder builder = EasyMock.createMock(MavenProjectBuilder.class);
        EasyMock.expect(builder.buildFromRepository(EasyMock.isA(Artifact.class), EasyMock.isA(List.class),
                EasyMock.isA(ArtifactRepository.class))).andReturn(new MavenProject()).anyTimes();
        EasyMock.replay(builder);

        mojo.setMavenProjectBuilder(builder);
        mojo.setCheckArtifacts(check);
        mojo.setForceUpgrade(true);
        mojo.setLocalRepository(EasyMock.createMock(ArtifactRepository.class));
        mojo.setRemoteArtifactRepositories(EasyMock.createMock(List.class));
        mojo.setArtifactMetadataSource(artifactMetadataSource);
        try {
            mojo.execute();
            Assert.fail("Should have thrown MojoExecutionException!");
        } catch (MojoExecutionException e) {

        }

        EasyMock.verify(artifactMetadataSource, builder);
    }

    @Test
    public void shouldFailBuildWhenThereIsForcedUpdate() throws ArtifactMetadataRetrievalException, ProjectBuildingException {
        List list = new ArrayList();
        list.add(new DefaultArtifactVersion("2.0"));
        ArtifactMetadataSource artifactMetadataSource = EasyMock.createMock(ArtifactMetadataSource.class);
        EasyMock.expect(artifactMetadataSource.retrieveAvailableVersions(EasyMock.isA(Artifact.class),
                EasyMock.isA(ArtifactRepository.class), EasyMock.isA(List.class))).andReturn(list).anyTimes();
        EasyMock.replay(artifactMetadataSource);

        List check = new ArrayList();
        check.add(new org.hoydaa.maven.plugins.Artifact("com.foo", "parent"));

        MavenProject parentProject = new MavenProject();
        parentProject.getProperties().put("force.upgrade", "true");
        MavenProjectBuilder builder = EasyMock.createMock(MavenProjectBuilder.class);
        EasyMock.expect(builder.buildFromRepository(EasyMock.isA(Artifact.class), EasyMock.isA(List.class),
                EasyMock.isA(ArtifactRepository.class))).andReturn(parentProject).anyTimes();
        EasyMock.replay(builder);

        mojo.setMavenProjectBuilder(builder);
        mojo.setCheckArtifacts(check);
        mojo.setForceUpgrade(false);
        mojo.setLocalRepository(EasyMock.createMock(ArtifactRepository.class));
        mojo.setRemoteArtifactRepositories(EasyMock.createMock(List.class));
        mojo.setArtifactMetadataSource(artifactMetadataSource);
        try {
            mojo.execute();
            Assert.fail("Should have thrown MojoExecutionException!");
        } catch (MojoExecutionException e) {

        }

        EasyMock.verify(artifactMetadataSource, builder);
    }

}
