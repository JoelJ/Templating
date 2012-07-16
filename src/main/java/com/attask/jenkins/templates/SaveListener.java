package com.attask.jenkins.templates;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;

import java.io.IOException;

/**
 * User: Joel Johnson
 * Date: 6/19/12
 * Time: 7:54 AM
 */
//@Extension
//public class SaveListener extends SaveableListener {
//	@Override
//	public void onChange(Saveable o, XmlFile file) {
//		if(o instanceof BuildableItemWithBuildWrappers) {
//			DescribableList<BuildWrapper,Descriptor<BuildWrapper>> buildWrappersList = ((BuildableItemWithBuildWrappers) o).getBuildWrappersList();
//			for (BuildWrapper buildWrapper : buildWrappersList) {
//				if(buildWrapper instanceof TemplateBuildWrapper) {
//					((TemplateBuildWrapper) buildWrapper).setSynced(false);
//				} else if(buildWrapper instanceof ImplementationBuildWrapper) {
//					((ImplementationBuildWrapper) buildWrapper).setSynced(false);
//				}
//			}
//		}
//	}
//}
