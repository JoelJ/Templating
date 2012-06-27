package com.attask.jenkins.scaffolding;

import com.attask.jenkins.BuildWrapperUtils;
import com.attask.jenkins.templates.ImplementationBuildWrapper;
import com.attask.jenkins.templates.TemplateBuildWrapper;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.RootAction;
import hudson.model.TopLevelItem;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Joel Johnson
 * Date: 6/19/12
 * Time: 2:41 PM
 */
@Extension
public class ScaffoldAction implements RootAction {
    private ScaffoldCache scaffoldCache = new ScaffoldCache();

    public ScaffoldCache getAllScaffolding() {
        return scaffoldCache;
    }

    public void doFindVariablesForJob(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        response.setContentType("application/json");

        Map<String, Object> result = new HashMap<String, Object>();

        if (!"get".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(405);
            result.put("error", "GET expected, but was " + request.getMethod());
        } else {
            String[] jobNames = request.getParameterValues("names");
            Set<String> variables = new TreeSet<String>();
            if (jobNames != null) {
                variables.addAll(getVariablesForJob(jobNames));
            }
            result.put("result", variables);
        }

        ServletOutputStream outputStream = response.getOutputStream();
        JSON json = JSONSerializer.toJSON(result);
        outputStream.print(json.toString());
        outputStream.flush();
    }

    private Set<String> getVariablesForJob(String... jobNames) throws IOException {
        Set<String> variables = new TreeSet<String>();
        for (String jobName : jobNames) {
            AbstractProject nearest = Project.findNearest(jobName);
            String configFile = nearest.getConfigFile().asString();

            Pattern pattern = Pattern.compile("\\$\\$([\\w\\d_]+)\\b");
            Matcher matcher = pattern.matcher(configFile);

            while (matcher.find()) {
                String variableName = matcher.group(1); //Group one doesn't include the $$ or the \\b
                variables.add(variableName);
            }
        }
        return variables;
    }

    public void doCreateScaffold(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        String name = request.getParameter("name");
        String[] jobNames = request.getParameterValues("jobNames");
        String[] variableNames = request.getParameterValues("variables");
        if (name != null && jobNames != null) {
            if (variableNames == null) {
                variableNames = new String[]{};
            }
            Scaffold scaffold = new Scaffold(name, Arrays.asList(jobNames), Arrays.asList(variableNames));
            scaffoldCache.put(scaffold);
            scaffold.save();
            response.forward(this, "index", request);
        } else {
            response.forwardToPreviousPage(request);
        }
    }

    public void doDeleteScaffold(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        String name = request.getParameter("name");
        scaffoldCache.remove(name);
        Scaffold.delete(name);

        response.forward(this, "index", request);
    }

    public void doStandUpScaffold(StaplerRequest request, StaplerResponse response) throws IOException, ServletException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (!"post".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(405);
            return;
        }
        String scaffoldName = request.getParameter("scaffoldName");
        String jobNameAppend = request.getParameter("jobNameAppend");
        Scaffold scaffold = scaffoldCache.get(scaffoldName);
        List<String> variables = scaffold.getVariables();
        Map<String, String> variableValues = new HashMap<String, String>();
        for (String variable : variables) {
            String value = request.getParameter(variable);
            variableValues.put(variable, value);
        }
        List<String> jobNames = scaffold.getJobNames();
        for (String jobName : jobNames) {
            AbstractProject jobToClone = Project.findNearest(jobName);
			String newName = jobName + jobNameAppend;
			TemplateBuildWrapper templateBuildWrapper = BuildWrapperUtils.findBuildWrapper(TemplateBuildWrapper.class, jobToClone);
			if(templateBuildWrapper != null) {
				if (jobToClone instanceof TopLevelItem) {
					Class<? extends TopLevelItem> jobClass = ((TopLevelItem) jobToClone).getClass();
					TopLevelItem newJob;
					try {
						newJob = Jenkins.getInstance().createProject(jobClass, newName);
					} catch (IllegalArgumentException e) {
						response.forwardToPreviousPage(request);
						return;
					}

					if (newJob instanceof BuildableItemWithBuildWrappers) {
						// If the target job (jobToClone) is actually a template, let's implement it rather than just clone it.
						if(templateBuildWrapper != null) {
							BuildableItemWithBuildWrappers buildable = (BuildableItemWithBuildWrappers) newJob;
							DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappersList = buildable.getBuildWrappersList();
							TemplateBuildWrapper toRemove = BuildWrapperUtils.findBuildWrapper(TemplateBuildWrapper.class, buildable);
							buildWrappersList.remove(toRemove);

							String variablesAsPropertiesFile = squashVariables(variableValues);
							ImplementationBuildWrapper implementationBuildWrapper = new ImplementationBuildWrapper(jobToClone.getName(), newJob.getName(), variablesAsPropertiesFile);
							buildWrappersList.add(implementationBuildWrapper);
							newJob.save();
						}
					}
				}
			} else {
				//clone
				Jenkins.getInstance().copy(jobToClone, newName);
			}


        }
        response.forward(this, "index", request);
    }

    public String squashVariables(Map<String, String> variables) {
        StringBuilder sb = new StringBuilder("# Generated By Scaffolding\n");
        for (Map.Entry<String, String> var : variables.entrySet()) {
            sb.append(var.getKey()).append("=").append(var.getValue()).append("\n");
        }
        return sb.toString();
    }

    public String getIconFileName() {
        return "/plugin/Templating/blueprint.png";
    }

    public String getDisplayName() {
        return "Scaffolding";
    }

    public String getUrlName() {
        return "scaffolding";
    }
}
