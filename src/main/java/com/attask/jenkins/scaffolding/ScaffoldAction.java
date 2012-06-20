package com.attask.jenkins.scaffolding;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Executor;
import hudson.model.Project;
import hudson.model.RootAction;
import jenkins.model.Jenkins;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
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

		if(!"get".equalsIgnoreCase(request.getMethod())) {
			response.setStatus(405);
			result.put("error", "GET expected, but was " + request.getMethod());
		} else {
			String jobName = request.getParameter("name");
			List<String> variables = getVariablesForJob(jobName);
			result.put("result", variables);
		}

		ServletOutputStream outputStream = response.getOutputStream();
		JSON json = JSONSerializer.toJSON(result);
		outputStream.print(json.toString());
		outputStream.flush();
	}

	private List<String> getVariablesForJob(String jobName) throws IOException {
		AbstractProject nearest = Project.findNearest(jobName);
		String configFile = nearest.getConfigFile().asString();

		Pattern pattern = Pattern.compile("\\$\\$([\\w\\d_]+)\\b");
		Matcher matcher = pattern.matcher(configFile);
		List<String> variables = new ArrayList<String>();
		while(matcher.find()) {
			String variableName = matcher.group(1); //Group one doesn't include the $$ or the \\b
			variables.add(variableName);
		}
		return variables;
	}

	public void doCreateScaffold(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
		String name = request.getParameter("name");
		String[] jobNames = request.getParameterValues("jobs");
		String[] variableNames = request.getParameterValues("variables");

		Scaffold scaffold = new Scaffold(name, Arrays.asList(jobNames), Arrays.asList(variableNames));
        scaffoldCache.put(scaffold);

		scaffold.save();

		response.forwardToPreviousPage(request);
	}

	public void doDeleteScaffold(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
		String name = request.getParameter("name");
        scaffoldCache.remove(name);
        Scaffold.delete(name);

		response.forwardToPreviousPage(request);
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
