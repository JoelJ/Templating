package com.attask.jenkins.templates;

import hudson.model.AbstractProject;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;

/**
 * User: Joel Johnson
 * Date: 6/26/12
 * Time: 10:57 AM
 */
public class BuildWrapperUtils {
	public static <T extends BuildWrapper> T findBuildWrapper(Class<T> buildWrapper, AbstractProject project) {
		if(project instanceof BuildableItemWithBuildWrappers) {
			DescribableList<BuildWrapper,Descriptor<BuildWrapper>> buildWrappersList = ((BuildableItemWithBuildWrappers) project).getBuildWrappersList();

			for (BuildWrapper wrapper : buildWrappersList) {
				if(buildWrapper.isAssignableFrom(wrapper.getClass())) {
					return buildWrapper.cast(wrapper);
				}
			}
		}

		return null;
	}
}
