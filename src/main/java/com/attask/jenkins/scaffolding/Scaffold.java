package com.attask.jenkins.scaffolding;

import com.thoughtworks.xstream.XStream;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Items;
import hudson.model.Jobs;
import hudson.model.Saveable;
import hudson.model.TopLevelItem;
import hudson.model.listeners.SaveableListener;
import hudson.util.XStream2;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * User: Joel Johnson
 * Date: 6/20/12
 * Time: 9:45 AM
 */
@ExportedBean
public class Scaffold extends AbstractDescribableImpl<Scaffold> implements Saveable {
    private String name;
    private List<String> jobNames;
    private List<String> variables;
    private Map<String,List<String>> childJobs;
    private static final XStream XSTREAM = new XStream2();

    public Scaffold(String name, Collection<String> jobNames, Collection<String> variables) {
        this.name = name;
        this.jobNames = new ArrayList<String>(jobNames);
        this.variables = new ArrayList<String>(variables);
        this.childJobs = new TreeMap<String, List<String>>();

    }

    private Scaffold(String name) {
        this(name, Collections.<String>emptyList(), Collections.<String>emptyList());
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
        return variables;
    }

    @Exported
    public Map<String,List<String>> getChildJobs() {
        return childJobs;
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

    public void addChildJob(String suffix, String name) {

        List<String> jobs = childJobs.get(suffix);
        if (jobs == null) {
            jobs = new ArrayList<String>();
        }
        jobs.add(name);

        childJobs.put(suffix, jobs);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Scaffold> {
        @Override
        public String getDisplayName() {
            return "Scaffolding";
        }
    }
}
