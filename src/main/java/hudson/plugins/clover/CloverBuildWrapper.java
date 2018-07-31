package hudson.plugins.clover;

import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.Launcher;
import hudson.Proc;
import hudson.FilePath;
import hudson.Extension;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import hudson.remoting.Channel;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.Descriptor;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.model.FreeStyleProject;
import hudson.model.Action;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.ArrayList;

import org.eclipse.hudson.api.model.IBaseBuildableProject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import net.sf.json.JSONObject;
import com.atlassian.clover.api.ci.CIOptions;
import com.atlassian.clover.api.ci.Integrator;
import java.util.Collections;

/**
 * A BuildWrapper that decorates the command line just before a build starts with targets and properties that will automatically
 * integrate Clover into the Ant build.
 */
public class CloverBuildWrapper extends BuildWrapper {


    public boolean historical;
    public boolean json;
    public boolean putValuesInQuotes;

    @DataBoundConstructor
    public CloverBuildWrapper(boolean historical, boolean json, boolean putValuesInQuotes) {
        this.historical = historical;
        this.json = json;
        this.putValuesInQuotes = putValuesInQuotes;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException {
        addCloverPublisher(build, listener);
        return new Environment() {};
    }

    private void addCloverPublisher(AbstractBuild build, BuildListener listener) throws IOException {
        final AbstractProject project = build.getProject();
        final DescribableList<Publisher, Descriptor<Publisher>> publishers = project.getPublishersList();
        if (!publishers.contains(CloverPublisher.DESCRIPTOR)) {
            final String reportDir = "clover";
            listener.getLogger().println("Adding Clover Publisher with reportDir: " + reportDir);

            // note: cannot call getPublishersList().add() because it's not being stored back in the project
            if (project instanceof IBaseBuildableProject) {
                final IBaseBuildableProject buildableProject = (IBaseBuildableProject) project;
                buildableProject.addPublisher(new CloverPublisher(reportDir, null));
            }

            // check if addition was successful
            if (!build.getProject().getPublishersList().contains(CloverPublisher.DESCRIPTOR)) {
                listener.getLogger().println("WARNING: Failed to add CloverPublisher to the project configuration. "
                        + "You may need to configure Clover reports publishing manually.");
            }
        }
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject job) {
        // ensure only one project action exists on the project
        if (job.getAction(CloverProjectAction.class) == null) {
            return Collections.singletonList(new CloverProjectAction((Project) job));
        }
        return super.getProjectActions(job);
    }

    @Override
    public Launcher decorateLauncher(AbstractBuild build, Launcher launcher, BuildListener listener) throws Run.RunnerAbortedException {
        final CIOptions.Builder options = new CIOptions.Builder()
                .json(this.json)
                .historical(this.historical)
                .fullClean(true)
                .putValuesInQuotes(this.putValuesInQuotes);

        return new CloverDecoratingLauncher(launcher, options);
    }

    public static final Descriptor<BuildWrapper> DESCRIPTOR = new DescriptorImpl();


    /**
     * Descriptor for {@link CloverPublisher}. Used as a singleton. The class is marked as public so that it can be
     * accessed from views.
     * See <tt>views/hudson/plugins/clover/CloverPublisher/*.jelly</tt> for the actual HTML fragment for the
     * configuration screen.
     */
    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        public DescriptorImpl() {
            super(CloverBuildWrapper.class);
            load();
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return Messages.CloverBuildWrapper_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/clover/help-cloverConfig.html";
        }


        @Override
        public boolean configure(StaplerRequest req, JSONObject json) {
            req.bindParameters(this, "clover.");
            save();
            return true;
        }

        public boolean isApplicable(AbstractProject item) {
            // TODO: is there a better way to detect Ant builds?
            // should only be enabled for Ant projects.
            return (item instanceof FreeStyleProject);

        }


    }

    public static class CloverDecoratingLauncher extends Launcher {
        private final Launcher outer;
        private final CIOptions.Builder options;

        CloverDecoratingLauncher(Launcher outer, CIOptions.Builder options) {
            super(outer);
            this.outer = outer;
            this.options = options;
        }

        @Override
        public Proc launch(ProcStarter starter) throws IOException {

            decorateArgs(starter);
            return outer.launch(starter);
        }

        public void decorateArgs(ProcStarter starter) {

            List<String> userArgs = new LinkedList<>();
            List<String> preSystemArgs = new LinkedList<>();
            List<String> postSystemArgs = new LinkedList<>();

            final List<String>  cmds = new ArrayList<>();
            cmds.addAll(starter.cmds());

            // on windows - the cmds are wrapped of the form:
            // "cmd.exe", "/C", "\"ant.bat clean test.run    &&  exit %%ERRORLEVEL%%\""
            // this hacky code is used to parse out just the user specified args. ie clean test.run
            
            final int numPreSystemCmds = 2; // hack hack hack - there are 2 commands prepended on windows...
            final String sysArgSplitter = "&&";
            
            if (!cmds.isEmpty() && cmds.size() >= numPreSystemCmds && !cmds.get(0).endsWith("ant"))
            {
                preSystemArgs.addAll(cmds.subList(0, numPreSystemCmds));

                // get the index of the "ant.bat 
                String argString = cmds.get(numPreSystemCmds);
                // trim leading and trailing " if they exist...
                argString = argString.replaceAll("\"", "");

                String[] tokens = argString.split(" ");
                preSystemArgs.add(tokens[0]);

                for (int i = 1; i < tokens.length; i++)
                {   // chop the ant.bat
                    String arg = tokens[i];
                    if (sysArgSplitter.equals(arg))
                    {
                        // anything after the &&, break.
                        postSystemArgs.addAll(Arrays.asList(tokens).subList(i, tokens.length));
                        break;
                    }
                    userArgs.add(arg);
                }
            }
            else 
            {
                if (cmds.size() > 0)
                {
                    preSystemArgs.add(cmds.get(0));                    
                }
                if (cmds.size() > 1)
                {
                    userArgs.addAll(cmds.subList(1, cmds.size()));
                }
            }

            if (!userArgs.isEmpty())
            {

                // TODO: full clean needs to be an option. see http://jira.atlassian.com/browse/CLOV-736
                options.fullClean(true);

                Integrator integrator = Integrator.Factory.newAntIntegrator(options.build());
                
                integrator.decorateArguments(userArgs);
                starter.cmds(new ArrayList<String>());

                // re-assemble all commands
                List<String> allCommands = new ArrayList<>();
                allCommands.addAll(preSystemArgs);
                allCommands.addAll(userArgs);
                allCommands.addAll(postSystemArgs);
                starter.cmds(allCommands);
                
                // masks.length must equal cmds.length
                boolean[] masks = new boolean[starter.cmds().size()];
                for (int i = 0; i < starter.masks().length; i++) {
                    masks[i] = starter.masks()[i];
                }
                starter.masks(masks);
            }
        }

        @Override
            public Channel launchChannel(String[] cmd, OutputStream out, FilePath workDir, Map<String, String> envVars) throws IOException, InterruptedException {
            return outer.launchChannel(cmd, out, workDir, envVars);
        }

        @Override
            public void kill(Map<String, String> modelEnvVars) throws IOException, InterruptedException {
            outer.kill(modelEnvVars);
        }

    }
}
