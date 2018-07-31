package hudson.plugins.clover.results;

import hudson.model.AbstractBuild;
import hudson.plugins.clover.CloverBuildAction;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Clover Coverage results for a specific file.
 */
public class FileCoverage extends AbstractClassAggregatedMetrics {

    private List<ClassCoverage> classCoverages = new ArrayList<>();

    public List<ClassCoverage> getChildren() {
        return getClassCoverages();
    }

    public ClassCoverage getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        return findClassCoverage(token);
    }

    public boolean addClassCoverage(ClassCoverage result) {
        return classCoverages.add(result);
    }

    public List<ClassCoverage> getClassCoverages() {
        return classCoverages;
    }

    public ClassCoverage findClassCoverage(String name) {
        for (ClassCoverage i : classCoverages) {
            if (name.equals(i.getName())) return i;
        }
        return null;
    }

    public AbstractCloverMetrics getPreviousResult() {
        CloverBuildAction action = getPreviousCloverBuildAction();
        if (action == null) {
            return null;
        }
        return action.findFileCoverage(getName());
    }

    public void setOwner(AbstractBuild owner) {
        super.setOwner(owner);
        for (ClassCoverage classCoverage : classCoverages) {
            classCoverage.setOwner(owner);
        }
    }
}
