package org.hoydaa.maven.plugins

import org.junit.Test
import org.junit.Before
import org.apache.maven.project.MavenProject
import org.apache.maven.artifact.Artifact
import org.easymock.EasyMock

/**
 * @author Umut Utkan
 */
class ParentCheckerMojoTest {

    ParentCheckerMojo mojo

    @Before
    def void setUp() {
        mojo = new ParentCheckerMojo()
    }

    @Test
    def void shouldDoNothingIfParentIsNotInTheList() {
        Artifact artifact = EasyMock.createMock(Artifact.class)
        EasyMock.expect(artifact.getGroupId()).andReturn("com.foo").anyTimes()
        EasyMock.expect(artifact.getArtifactId()).andReturn("does-not-matter").anyTimes()
        MavenProject project = new MavenProject()
        project.setParentArtifact artifact
        EasyMock.replay artifact

        mojo.project = project
        mojo.execute()
        mojo.checkArtifacts = [new org.hoydaa.maven.plugins.Artifact("com.foo", "parent")]
        mojo.execute()

        EasyMock.verify artifact
    }

    def void shouldFailBuildIfFailBuildSet() {

    }

    def void shouldFailBuildWhenThereIsEnforcedUpdate() {

    }

}
