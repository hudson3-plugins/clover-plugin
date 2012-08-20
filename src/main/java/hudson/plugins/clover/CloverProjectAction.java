package hudson.plugins.clover;

import hudson.FilePath;
import hudson.model.Project;
import hudson.model.ProminentProjectAction;
import hudson.model.Build;
import hudson.model.Result;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Actionable;
import hudson.util.Graph;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

/**
 * Project level action.
 *
 * TODO: refactor this action in a similar manner to JavadocArchiver and BaseJavadocAction etc to avoid duplication.
 *
 * @author Stephen Connolly
 */
public class CloverProjectAction extends Actionable implements ProminentProjectAction {

    static final String ICON = "/plugin/clover/clover_48x48.png";
    
    private final Project<?, ?> project;

    public CloverProjectAction(Project project) {
        this.project = project;
    }

    public String getIconFileName() {

        final File reportDir = getLastBuildReportDir();
        if (reportDir != null &&
             (new File(reportDir, "index.html").exists()
           || new File(reportDir, "clover.pdf").exists()
           || new File(reportDir, "clover.xml").exists())) {
            return ICON;
        } else {
            return null;
        }
    }

    private File getLastBuildReportDir() {
        if (project.getLastBuild() == null) {
            // no clover report links, until there is at least one build
            return null;
        }
        final File reportDir = project.getLastBuild().getRootDir();
        return reportDir;
    }

    public String getDisplayName() {
        final File reportDir = getLastBuildReportDir();

        if (reportDir == null) return null;
        if (new File(reportDir, "index.html").exists()) return Messages.CloverProjectAction_HTML_DisplayName();
        if (new File(reportDir, "clover.pdf").exists()) return Messages.CloverProjectAction_PDF_DisplayName();
        if (new File(reportDir, "clover.xml").exists()) return Messages.CloverProjectAction_XML_DisplayName();

        return null;

    }

    public String getUrlName() {
        return "clover";
    }

    /**
     * Returns the last Result that was successful.
     *
     * WARNING: this method is invoked dynamically from CloverProjectAction/floatingBox.jelly
     * @return the last successful build result
     */
    public CloverBuildAction getLastSuccessfulResult() {
        for (Build<?, ?> b = project.getLastBuild(); b != null; b = b.getPreviousBuild()) {
            if (b.getResult() == Result.FAILURE)
                continue;
            CloverBuildAction r = b.getAction(CloverBuildAction.class);
            if (r != null)
                return r;
        }
        return null;
    }

    public Graph getTrendGraph() {
        CloverBuildAction action= getLastSuccessfulResult();
        if (action != null)
            return action.getResult().getTrendGraph();
        return null;
    }

    public DirectoryBrowserSupport doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException,
            InterruptedException {

        // there is a report if there was a build already, and there is a report
        if (project.getLastBuild() != null && getDisplayName() != null) {
            return new DirectoryBrowserSupport(this,
                    new FilePath(project.getLastBuild().getRootDir()),"Clover Html Report",  "/clover/clover.gif", false);

        } else {
            return null;
        }        
    }

    public String getSearchUrl() {
        return getUrlName();
    }
}
