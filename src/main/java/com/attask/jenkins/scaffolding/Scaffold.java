package com.attask.jenkins.scaffolding;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: Joel Johnson
 * Date: 6/20/12
 * Time: 9:45 AM
 */
@ExportedBean
public class Scaffold extends AbstractDescribableImpl<Scaffold> {
	private String name;
	private List<String> jobNames;
	private List<String> variables;

	public Scaffold(String name, Collection<String> jobNames, Collection<String> variables) {
		this.name = name;
		this.jobNames = new ArrayList<String>(jobNames);
		this.variables = new ArrayList<String>(variables);
	}

	@Exported
	public String getName() {
		return name;
	}

	@Exported
	public List<String> getJobNames() {
		return jobNames;
	}

	@Exported
	public List<String> getVariables() {
		return variables;
	}

	@Override
	public String toString() {
		return "Scaffold{" +
				"name='" + name + '\'' +
				", jobNames=" + jobNames +
				", variables=" + variables +
				'}';
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<Scaffold> {
		@Override
		public String getDisplayName() {
			return "Scaffolding";
		}
	}
}
