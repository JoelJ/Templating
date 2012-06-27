package com.attask.jenkins.templates;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Project;
import hudson.model.Run;
import hudson.util.RunList;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * User: Joel Johnson
 * Date: 6/26/12
 * Time: 7:58 PM
 */
public class TemplateSyncAction implements Action {
	private final Syncable syncable;

	public TemplateSyncAction(Syncable syncable) {
		this.syncable = syncable;
	}

	public void doSync(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
		syncable.sync();
		response.sendRedirect("..");
	}

	public String getIconFileName() {
		if(getDisplayName() == null) {
			return null;
		}
		return "/plugin/Templating/sync.png";
	}

	public AbstractProject getProject() {
		return Project.findNearest(syncable.getProjectName());
	}

	public boolean getProjectHasRunningJobs() {
		RunList<Run> builds = getProject().getBuilds();
		for (Run build : builds) {
			if(build.isBuilding()) {
				return true;
			}
		}
		return false;
	}

	public String getDisplayName() {
		if(!syncable.isSynced()) {
			return "Sync";
		}
		return null;
	}

	public String getUrlName() {
		return "sync";
	}
}
