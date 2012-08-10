package com.attask.jenkins.scaffolding;

import com.attask.jenkins.BuildWrapperUtils;
import com.attask.jenkins.CollectionUtils;
import com.attask.jenkins.templates.ImplementationBuildWrapper;
import com.thoughtworks.xstream.XStream;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.*;
import hudson.model.listeners.SaveableListener;
import hudson.util.XStream2;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

/**
 * User: Joel Johnson
 * Date: 6/20/12
 * Time: 9:45 AM
 */
@ExportedBean
public class Scaffold extends AbstractDescribableImpl<Scaffold> implements Saveable {
    private String name;
    private List<String> jobNames;
    private transient List<String> variables;
    private transient Map<String,List<String>> childJobs;
    private Map<String,ScaffoldImplementation> scaffoldImplementations;
    private static final XStream XSTREAM = new XStream2();

    public Scaffold(String name, Collection<String> jobNames,Collection<String> variables) {
        this.name = name;
        this.jobNames=new ArrayList<String>(jobNames);
        this.variables = new ArrayList<String>(variables);
        this.childJobs = new TreeMap<String, List<String>>();
        this.scaffoldImplementations= new TreeMap<String, ScaffoldImplementation>();

    }

    private Scaffold(String name) {
        this(name, Collections.<String>emptyList(),Collections.<String>emptyList());
    }

    public void sync(String childJobName) throws IOException {
        ScaffoldImplementation scaffoldImplementation = scaffoldImplementations.get(childJobName);
        List<String> jobs = scaffoldImplementation.getJobNames();
        for(String job: jobs){
            AbstractProject project = Project.findNearest(job);
            try{
                ImplementationBuildWrapper buildWrapper = BuildWrapperUtils.findBuildWrapper(ImplementationBuildWrapper.class, project);

                buildWrapper.sync();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void syncVars(String childJobName){
        ScaffoldImplementation scaffoldImplementation = getScaffoldImplementations().get(childJobName);
        Map<String, String> variables = scaffoldImplementation.getVariables();
        List<String> jobNames = scaffoldImplementation.getJobNames();
        for (String jobName : jobNames) {
            String templateName = jobName.split(scaffoldImplementation.getName())[0];
            Set<String> variablesForJob = ScaffoldAction.getVariablesForJob(templateName);
            for (String variable : variablesForJob) {
                if(!variables.containsKey(variable)) {
                    scaffoldImplementation.putVariable(variable,"");
                }
            }
        }
        for (String jobName : jobNames) {
            AbstractProject project = AbstractProject.findNearest(jobName);
            ImplementationBuildWrapper buildWrapper = BuildWrapperUtils.findBuildWrapper(ImplementationBuildWrapper.class, project);
            buildWrapper.setVariables(ScaffoldAction.squashVariables(variables));
            try {
                project.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Scaffold find(String name) throws IOException {
        Scaffold scaffold = new Scaffold(name);
        if (!scaffold.load()) {
            scaffold = null;
        }
        return scaffold;
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
        return new ArrayList<String>(ScaffoldAction.getVariablesForJob(this.getJobNames()));
    }

    @Exported
    public Map<String, ScaffoldImplementation> getScaffoldImplementations() {
        return scaffoldImplementations;
    }

    @Override
    public String toString() {
        return "Scaffold{" +
                "name='" + name + '\'' +
                ", jobNames=" + jobNames +
                ", variables=" + variables +
                '}';
    }

    public void save() throws IOException {
        getConfigFile().write(this);
        SaveableListener.fireOnChange(this, getConfigFile());
    }

    public synchronized boolean load() throws IOException {
        XmlFile config = getConfigFile();
        if (config != null && config.exists()) {
            config.unmarshal(this);
            return true;
        }
        return false;


    }

    protected final XmlFile getConfigFile() {
        return new XmlFile(XSTREAM, new File(getRootDir(), name + "/config.xml"));
    }

    protected static File getRootDir() {
        return new File(Hudson.getInstance().getRootDir(), "Scaffolding");
    }

    public static Set<String> getAllNames() {
        Set<String> result = new HashSet<String>();
        File rootDir = getRootDir();
        File[] directories = rootDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
		if(directories != null) {
			for (File directory : directories) {
				result.add(directory.getName());
			}
		}
        return result;
    }

    public static void delete(String name) {
        File file = new File(getRootDir(), name);
        if (file.exists() && file.isDirectory()) {
            try {
                FileUtils.forceDelete(file);
            } catch (IOException e) {
                throw new RuntimeException("Could not delete " + name, e); //Todo: make better exception
            }
        }
    }

    public void addChildJob(String suffix, String name, Map<String,String> variables) {

        ScaffoldImplementation imp = scaffoldImplementations.get(suffix);
        List<String> jobs;
        if (imp == null) {
            jobs=new ArrayList<String>();
        }
        else{
            jobs=imp.getJobNames();
        }
        if(variables==null){
            variables=new HashMap<String,String>();
        }
        jobs.add(name);
        ScaffoldImplementation implementation=new ScaffoldImplementation(suffix,jobs,variables);

        scaffoldImplementations.put(suffix, implementation);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Scaffold> {
        @Override
        public String getDisplayName() {
            return "Scaffolding";
        }
    }
}
