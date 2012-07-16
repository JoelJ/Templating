package com.attask.jenkins.templates;

import java.io.IOException;

/**
 * User: Joel Johnson
 * Date: 6/26/12
 * Time: 8:06 PM
 */
public interface Syncable {
	public void sync() throws IOException;
	public String getProjectName();
	public boolean showSynced();
}
