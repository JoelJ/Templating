package com.attask.jenkins.scaffolding;


import com.attask.jenkins.BuildWrapperUtils;
import com.attask.jenkins.templates.ImplementationBuildWrapper;
import com.attask.jenkins.templates.TemplateBuildWrapper;
import hudson.model.AbstractProject;
import hudson.model.Project;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: brianmondido
 * Date: 8/8/12
 * Time: 1:49 PM
 * To change this template use File | Settings | File Templates.
 */

@ExportedBean
public class ScaffoldImplementation {
    private String name;
    private List<String> jobNames;
    private Map<String, String> variables;

    /**
     *
     * @param jobNames
     * @param variables
     */
    public ScaffoldImplementation(String name, List<String>jobNames, Map<String,String> variables){
        this.name=name;
        this.jobNames=jobNames;
        this.variables=variables;
    }

    /**
     * Gets a ScaffoldImplementation's variables Map&lt;variableName,variableValue&gt;
     * @return
     */
    @Exported
    public Map<String, String> getVariables() {
        return variables;
    }

    public void putVariable(String key, String value){
        variables.put(key,value);
    }



    /**
     * Gets a ScaffoldImplementation's childJob List
     * @return
     */
    @Exported
    public List<String> getJobNames() {
        return jobNames;
    }

    @Exported
    public String getName(){
        return name;
    }

    @Override
    public String toString() {
        return "ScaffoldImplementation{" +
                "name='" + name + '\'' +
                ", jobNames=" + jobNames +
                ", variables=" + variables +
                '}';
    }
}
