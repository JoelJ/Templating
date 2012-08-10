package com.attask.jenkins.templates;

import com.attask.jenkins.BuildWrapperUtils;
import com.attask.jenkins.ReflectionUtils;
import com.attask.jenkins.UnixUtils;
import com.google.common.collect.ImmutableMap;
import hudson.Extension;
import hudson.Launcher;
import hudson.XmlFile;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.CopyOnWriteList;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * User: Joel Johnson
 * Date: 6/18/12
 * Time: 8:30 PM
 */
@ExportedBean
public class ImplementationBuildWrapper extends BuildWrapper implements Syncable {
	private final String templateName;
	private final String implementationName;
	private String variables;
    /**
     * Needed for backward compatibility with systems that used an older version of this plugin
     */
    public transient boolean synced;

	@DataBoundConstructor
	public ImplementationBuildWrapper(String templateName, String implementationName, String variables) {
		this.templateName = templateName;
		this.implementationName = implementationName;
		this.variables = variables;
	}

	public void sync() throws IOException {
		AbstractProject template = null;
		AbstractProject implementation = null;
		try {
			template = Project.findNearest(templateName);
			implementation = Project.findNearest(implementationName);
		} catch (NullPointerException ignore) {
			//unfortunately, on jenkins load we get null pointer exceptions here
		}
		if(template != null && implementation != null) {
			syncFromTemplate(template, implementation);
		}
	}

	public String getProjectName() {
		return implementationName;
	}

	void syncFromTemplate(AbstractProject template, AbstractProject implementation) throws IOException {
		if(
				implementation == null ||
				!(implementation instanceof BuildableItemWithBuildWrappers) ||
				!(implementation instanceof Describable) ||
				template == null ||
				!(template instanceof BuildableItemWithBuildWrappers) ||
				!(template instanceof Describable)
			) {
			return;
		}

		ImplementationBuildWrapper implementationBuildWrapper = this;
		TemplateBuildWrapper templateBuildWrapper = BuildWrapperUtils.findBuildWrapper(TemplateBuildWrapper.class, template);
		if(templateBuildWrapper == null) {
			return;
		}

		Map<Pattern, String> propertiesMap = getPropertiesMap(template, implementation, implementationBuildWrapper);

		String oldDescription = implementation.getDescription();
		boolean oldDisabled = implementation.isDisabled();
        Map<TriggerDescriptor, Trigger> oldTriggers = implementation.getTriggers();

        XmlFile implementationXmlFile = replaceConfig(template, implementation, propertiesMap);
		refreshAndSave(template, implementationBuildWrapper, implementationXmlFile, oldDescription, oldDisabled, oldTriggers);
	}

	private static Map<Pattern, String> getPropertiesMap(AbstractProject template, AbstractProject implementation, ImplementationBuildWrapper implementationBuildWrapper) {
		Map<String, String> variables = expandToMap(implementationBuildWrapper.getVariables());

		ImmutableMap.Builder<Pattern, String> patternPairsBuilder = ImmutableMap.builder();
		patternPairsBuilder.put(Pattern.compile(template.getClass().getCanonicalName() + ">"), implementation.getClass().getCanonicalName() + ">");

		for (Map.Entry<String, String> variable : variables.entrySet()) {
			patternPairsBuilder.put(Pattern.compile("\\$\\$" + variable.getKey()), variable.getValue());
		}

		return patternPairsBuilder.build();
	}

	private static XmlFile replaceConfig(AbstractProject template, AbstractProject implementation, Map<Pattern, String> propertiesMap) throws IOException {
		XmlFile implementationXmlFile = implementation.getConfigFile();
		File implementationFile = implementationXmlFile.getFile();

		assert template.getConfigFile() != null : "template config file shouldn't be null";

		InputStream templateFileStream = new FileInputStream(template.getConfigFile().getFile());
		try {
			OutputStream outputStream = new FileOutputStream(implementationFile);
			try {
				UnixUtils.sed(templateFileStream, outputStream, propertiesMap);
			} finally {
				outputStream.flush();
				outputStream.close();
			}
		} finally {
			templateFileStream.close();
		}
		return implementationXmlFile;
	}

	private static void refreshAndSave(AbstractProject template, ImplementationBuildWrapper implementationBuildWrapper, XmlFile implementationXmlFile, String oldDescription, boolean oldDisabled, Map<TriggerDescriptor, Trigger> oldTriggers) throws IOException {
		TopLevelItem item = (TopLevelItem) Items.load(Jenkins.getInstance(), implementationXmlFile.getFile().getParentFile());
		if(item instanceof AbstractProject) {
			AbstractProject newImplementation = (AbstractProject) item;

			//Use reflection to prevent it from auto-saving
			ReflectionUtils.setField(newImplementation, "description", oldDescription);
			ReflectionUtils.setField(newImplementation, "disabled", oldDisabled);
            Vector triggers = ReflectionUtils.getField(Vector.class, newImplementation, "triggers");
            triggers.clear();
            for (Trigger trigger : oldTriggers.values()) {
                triggers.add(trigger);
            }

			DescribableList<BuildWrapper, Descriptor<BuildWrapper>> implementationBuildWrappers = ((BuildableItemWithBuildWrappers) newImplementation).getBuildWrappersList();
			CopyOnWriteList data = ReflectionUtils.getField(CopyOnWriteList.class, implementationBuildWrappers, "data");

			//strip out any template definitions or implementation definitions copied from the template
			List<BuildWrapper> toRemove = new LinkedList<BuildWrapper>();
			for (BuildWrapper buildWrapper : implementationBuildWrappers) {
				if(buildWrapper instanceof TemplateBuildWrapper) {
					if(template.getName().equals(((TemplateBuildWrapper) buildWrapper).getTemplateName())) {
						toRemove.add(buildWrapper);
					}
				} else if(buildWrapper instanceof ImplementationBuildWrapper) {
					toRemove.add(buildWrapper);
				}
			}
			for (BuildWrapper buildWrapper : toRemove) {
				data.remove(buildWrapper);
			}

			//make sure the implementation definition is still in there
			data.add(implementationBuildWrapper);

			newImplementation.getConfigFile().write(newImplementation); //don't call save() because it calls the event handlers.
			item = (TopLevelItem) Items.load(Jenkins.getInstance(), implementationXmlFile.getFile().getParentFile());

			putItemInJenkins(Jenkins.getInstance(), item);
		}
	}

	/**
	 * Updates Jenkin's cache with the given TopLevelItem
	 * @param jenkins
	 * @param item
	 */
	private static void putItemInJenkins(Jenkins jenkins, TopLevelItem item) {
		Map map = ReflectionUtils.getField(Map.class, jenkins, "items");
		map.put(item.getName(), item);
	}

	public static Map<String, String> expandToMap(String parameters) {
		Map<String, String> result = new HashMap<String, String>();
		String[] split = parameters.split("\n");
		for (String s : split) {
			if (s.contains("#")) {
				s = s.substring(0, s.indexOf("#")).trim();
			}
			String[] keyValue = s.split("=", 2);
			if (keyValue.length == 2) {
				result.put(keyValue[0], keyValue[1]);
			}
		}
		return result;
	}

	@Exported
	public String getTemplateName() {
		return templateName;
	}

	@Exported
	public String getImplementationName() {
		return implementationName;
	}

	@Exported
	public String getVariables() {
		return variables;
	}


    public void setVariables(String variables){
        this.variables=variables;
    }

	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
				return true;
			}
		};
	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {
		/**
		 * Verifies that the template name both exists and is a TemplateProject
		 * @param value The value to validate
		 * @return FormValidation.ok() if everything checks out.
		 * FormValidation.error(...) if the given value is neither a project or a template.
		 */
		public FormValidation doCheckTemplateName(@QueryParameter String value) {
			if (value == null || value.trim().isEmpty()) {
				return FormValidation.error("Template is a required field.");
			}

			AbstractProject nearest = Project.findNearest(value);
			if(nearest == null || !value.equals(nearest.getName())) {
				return FormValidation.error("Project must exist.");
			}

			if(!(nearest instanceof BuildableItemWithBuildWrappers)) {
				return FormValidation.error("Project must explicitly be defined as a template.");
			}

			TemplateBuildWrapper wrapper = null;
			for (BuildWrapper buildWrapper : ((BuildableItemWithBuildWrappers) nearest).getBuildWrappersList()) {
				if(buildWrapper instanceof TemplateBuildWrapper) {
					wrapper = (TemplateBuildWrapper) buildWrapper;
					break;
				}
			}
			if(wrapper == null) {
				return FormValidation.error("Project must explicitly be defined as a template.");
			}

			return FormValidation.ok();
		}

		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return item instanceof Describable && item instanceof BuildableItemWithBuildWrappers;
		}

		@Override
		public String getDisplayName() {
			return "Implement Template";
		}
	}
}
