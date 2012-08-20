package hudson.plugins.clover.results;

import hudson.model.AbstractBuild;
import hudson.plugins.clover.CloverBuildAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Clover Coverage results for a specific package.
 * @author Stephen Connolly
 */
public class PackageCoverage extends AbstractFileAggregatedMetrics {

    private List<FileCoverage> fileCoverages = new ArrayList<FileCoverage>();

    public List<FileCoverage> getChildren() {
        return getFileCoverages();
    }

    public boolean addFileCoverage(FileCoverage result) {
        return fileCoverages.add(result);
    }

    public List<FileCoverage> getFileCoverages() {
        return fileCoverages;
    }

    public FileCoverage findFileCoverage(String name) {
        for (FileCoverage i : fileCoverages) {
            if (name.equals(i.getName())) return i;
        }
        return null;
    }

    public ClassCoverage findClassCoverage(String name) {
        for (FileCoverage i : fileCoverages) {
            ClassCoverage j = i.findClassCoverage(name);
            if (j != null) return j;
        }
        return null;
    }

    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) throws IOException {
        boolean isPath = false;
        for (FileCoverage i : fileCoverages) {
            if (i.getName().equals(token)) return i;
            if (i.getName().startsWith(token)) {
                isPath = true;
                break;
            }
        }
        if (isPath) {
            return new FilePathMapper(token + '/');
        }
        return null;
    }

    public AbstractCloverMetrics getPreviousResult() {
        CloverBuildAction action = getPreviousCloverBuildAction();
        if (action == null) {
            return null;
        }
        return action.findPackageCoverage(getName());
    }

    public void setOwner(AbstractBuild owner) {
        super.setOwner(owner);    //To change body of overridden methods use File | Settings | File Templates.
        for (FileCoverage fileCoverage : fileCoverages) {
            fileCoverage.setOwner(owner);
        }
    }

    public class FilePathMapper {
        private final String pathSoFar;

        public FilePathMapper(String pathSoFar) {
            this.pathSoFar = pathSoFar;
        }

        public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) throws IOException {
            final String testPath = pathSoFar + token;
            boolean isPath = false;
            for (FileCoverage i : fileCoverages) {
                if (i.getName().equals(testPath)) return i;
                if (i.getName().startsWith(testPath)) {
                    isPath = true;
                    break;
                }
            }
            if (isPath) {
                return new FilePathMapper(testPath + '/');
            }
            return null;
        }

    }
}
