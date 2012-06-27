package com.attask.jenkins.templates;

import com.attask.jenkins.BuildWrapperUtils;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;

import java.util.*;

/**
 * User: Joel Johnson
 * Date: 6/26/12
 * Time: 7:11 PM
 */
@Extension
public class SyncProjectAction extends TransientProjectActionFactory {
	@Override
	public Collection<? extends Action> createFor(AbstractProject target) {
		List<Action> actions = new ArrayList<Action>(2);
		TemplateBuildWrapper templateBuildWrapper = BuildWrapperUtils.findBuildWrapper(TemplateBuildWrapper.class, target);
		if(templateBuildWrapper != null) {
			actions.add(new TemplateSyncAction(templateBuildWrapper));
		}
		ImplementationBuildWrapper implementationBuildWrapper = BuildWrapperUtils.findBuildWrapper(ImplementationBuildWrapper.class, target);
		if(implementationBuildWrapper != null) {
			actions.add(new TemplateSyncAction(implementationBuildWrapper));
		}
		return actions;
	}
}
