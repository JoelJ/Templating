package com.attask.jenkins.scaffolding;

import com.attask.jenkins.BuildWrapperUtils;
import com.attask.jenkins.CollectionUtils;
import com.attask.jenkins.templates.ImplementationBuildWrapper;
import com.attask.jenkins.templates.TemplateBuildWrapper;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import org.kohsuke.stapler.QueryParameter;
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

    public void doSyncScaffold(StaplerRequest request, StaplerResponse response, @QueryParameter(required = true) String scaffoldName, @QueryParameter(required = true) String childJobName) throws IOException {
        Scaffold scaffold = scaffoldCache.get(scaffoldName);
        scaffold.sync(childJobName);

        String rootUrl = Jenkins.getInstance().getRootUrl() == null ? "/" : Jenkins.getInstance().getRootUrl();
        response.sendRedirect(rootUrl + getUrlName());
    }

    public void doEditScaffoldVariables(StaplerRequest request, StaplerResponse response, @QueryParameter(required = true) String scaffoldName, @QueryParameter(required = true) String childJobName) throws IOException {
        Scaffold scaffold = findScaffoldByName(scaffoldName);
        List<String> variables = scaffold.getVariables();
        Map<String, String> variableValues = new HashMap<String, String>();
        for (String variable : variables) {
            String value = request.getParameter(variable);
            variableValues.put(variable, value);
        }
        ScaffoldImplementation scaffoldImplementation= scaffold.getScaffoldImplementations().get(childJobName);
        scaffoldImplementation.getVariables().putAll(variableValues);
        List<String> jobNames = scaffoldImplementation.getJobNames();
        for (String jobName : jobNames) {
            AbstractProject project = Project.findNearest(jobName);
            ImplementationBuildWrapper buildWrapper = BuildWrapperUtils.findBuildWrapper(ImplementationBuildWrapper.class, project);
            getVariablesForImplementation(jobName).putAll(variableValues);
            buildWrapper.setVariables(squashVariables(variableValues));
            project.save();
        }
        String rootUrl = Jenkins.getInstance().getRootUrl() == null ? "/" : Jenkins.getInstance().getRootUrl();
        response.sendRedirect(rootUrl + getUrlName());
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

    public Map<String, String> getVariablesForImplementation(String jobName) {
        AbstractProject nearest = Project.findNearest(jobName);
        ImplementationBuildWrapper buildWrapper = BuildWrapperUtils.findBuildWrapper(ImplementationBuildWrapper.class, nearest);
        if(buildWrapper != null) {
            String variables = buildWrapper.getVariables();
            return CollectionUtils.expandToMap(variables);
        }

        return null;
    }


    public static Set<String> getVariablesForJob(String... jobs) {
        if(jobs == null) {
            return Collections.emptySet();
        }

        Collection<String> jobNames = Arrays.asList(jobs);
        return getVariablesForJob(jobNames);
    }

    public static Set<String> getVariablesForJob(Collection<String> jobNames) {
        if(jobNames.size() <= 0) {
            return Collections.emptySet();
        }
        Set<String> variables = new TreeSet<String>();
        for (String jobName : jobNames) {
            AbstractProject nearest = Project.findNearest(jobName);
            String configFile = null;
            try {
                configFile = nearest.getConfigFile().asString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Pattern pattern = Pattern.compile("\\$\\$([\\w\\d_]+)\\b");
            Matcher matcher = pattern.matcher(configFile);

            while (matcher.find()) {
                String variableName = matcher.group(1); //Group one doesn't include the $$ or the \\b
                variables.add(variableName);
            }
        }
        return variables;
    }

    /**
     * Creates or updates a scaffold
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    public void doCreateScaffold(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        String name = request.getParameter("name");
        String[] jobNames = request.getParameterValues("jobNames");
        Set<String> variableNames = getVariablesForJob(jobNames);
        if (name != null && jobNames != null) {
            List<String> newJobNames = Arrays.asList(jobNames);
            Scaffold oldScaffold=scaffoldCache.get(name);
            Scaffold scaffold=new Scaffold(name, newJobNames,variableNames);
            if(oldScaffold!=null){
                Map<String, ScaffoldImplementation> scaffoldImplementations = oldScaffold.getScaffoldImplementations();
                Set<String> suffixes = scaffoldImplementations.keySet();
                for (String suffix: suffixes) {
                    ScaffoldImplementation scaffoldImplementation = scaffoldImplementations.get(suffix);
                    Map<String, String> variables = scaffoldImplementation.getVariables();
                    List<String> implJobNames = scaffoldImplementation.getJobNames();

                    for (String jobName : scaffold.getJobNames()) {
                        if(!oldScaffold.getJobNames().contains(jobName)){
                            AbstractProject jobToClone = Project.findNearest(jobName);
                            String newName = jobName + suffix;
                            if(!implJobNames.contains(newName)){
                                implJobNames.add(newName);
                            }
                            if(Project.findNearest(newName)==null){
                                cloneJob(jobToClone, newName, variables);
                            }

                        }
                    }
                    ArrayList<String> jobsToKeep = new ArrayList<String>(implJobNames);
                    for (String jobToKeep : jobsToKeep) {
                        if(!scaffold.getJobNames().contains(jobToKeep.split(suffix)[0])){
                            implJobNames.remove(jobToKeep);
                        }
                    }
                    ScaffoldImplementation implementation = new ScaffoldImplementation(suffix, implJobNames, variables);
                    scaffold.getScaffoldImplementations().put(suffix,implementation);
                }
            }
            scaffoldCache.put(scaffold);
            scaffold.save();
        }
        String rootUrl = Jenkins.getInstance().getRootUrl() == null ? "/" : Jenkins.getInstance().getRootUrl();
        response.sendRedirect(rootUrl + getUrlName());
    }

    public void doDeleteScaffold(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        String name = request.getParameter("name");
        scaffoldCache.remove(name);
        Scaffold.delete(name);

        String rootUrl = Jenkins.getInstance().getRootUrl() == null ? "/" : Jenkins.getInstance().getRootUrl();
        response.sendRedirect(rootUrl + getUrlName());
    }

    public void doDeleteJobs(StaplerRequest request, StaplerResponse response) throws IOException, ServletException, InterruptedException {
        String suffix = request.getParameter("suffix");
        String scaffoldName = request.getParameter("scaffoldName");
        Scaffold scaffold = scaffoldCache.get(scaffoldName);
        ScaffoldImplementation implementation = scaffold.getScaffoldImplementations().get(suffix);
            for (String jobName : implementation.getJobNames()) {
                AbstractProject job = Project.findNearest(jobName);
                job.delete();
            }

        String rootUrl = Jenkins.getInstance().getRootUrl() == null ? "/" : Jenkins.getInstance().getRootUrl();
        response.sendRedirect(rootUrl + getUrlName());
    }

    /**
     * Stands up a Scaffold
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public void doStandUpScaffold(StaplerRequest request, StaplerResponse response) throws IOException, ServletException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (!"post".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(405);
            return;
        }
        String scaffoldName = request.getParameter("scaffoldName");
        String jobNameAppend = request.getParameter("jobNameAppend");
        Scaffold scaffold = findScaffoldByName(scaffoldName);
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
            cloneJob(jobToClone, newName, variableValues);

            scaffold.addChildJob(jobNameAppend, newName,variableValues);
        }
        scaffold.save();
        String rootUrl = Jenkins.getInstance().getRootUrl() == null ? "/" : Jenkins.getInstance().getRootUrl();
        response.sendRedirect(rootUrl + getUrlName());
    }

    public static TopLevelItem cloneJob(AbstractProject jobToClone, String newName, Map<String, String> variableValues) {
        try {
        TopLevelItem result = null;

        TemplateBuildWrapper templateBuildWrapper = BuildWrapperUtils.findBuildWrapper(TemplateBuildWrapper.class, jobToClone);
        if (templateBuildWrapper != null) {
            if (jobToClone instanceof TopLevelItem) {
                @SuppressWarnings("RedundantCast") //Need to cast to get the generics to work properly
                        Class<? extends TopLevelItem> jobClass = ((TopLevelItem) jobToClone).getClass();
                TopLevelItem newJob = Jenkins.getInstance().createProject(jobClass, newName);

                if (newJob != null && newJob instanceof BuildableItemWithBuildWrappers) {
                    // If the target job (jobToClone) is actually a template, let's implement it rather than just clone it.

                    BuildableItemWithBuildWrappers buildable = (BuildableItemWithBuildWrappers) newJob;
                    DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappersList = buildable.getBuildWrappersList();
                    TemplateBuildWrapper toRemove = BuildWrapperUtils.findBuildWrapper(TemplateBuildWrapper.class, buildable);
                    buildWrappersList.remove(toRemove);

                    String variablesAsPropertiesFile = squashVariables(variableValues);
                    ImplementationBuildWrapper implementationBuildWrapper = new ImplementationBuildWrapper(jobToClone.getName(), newJob.getName(), variablesAsPropertiesFile);
                    buildWrappersList.add(implementationBuildWrapper);
                    newJob.save();
                    result = newJob;
                    implementationBuildWrapper.sync();

                }
            }
        }

        if (result == null) {
            //clone
            result = (TopLevelItem) Jenkins.getInstance().copy(jobToClone, newName);
        }
        return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Scaffold findScaffoldByName(String scaffoldName) {
        if (scaffoldName == null || scaffoldName.isEmpty()) {
            return null;
        }
        return scaffoldCache.get(scaffoldName);
    }

    public static String squashVariables(Map<String, String> variables) {
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
