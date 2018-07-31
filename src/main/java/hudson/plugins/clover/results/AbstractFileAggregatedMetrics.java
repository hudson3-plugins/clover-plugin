package hudson.plugins.clover.results;

/**
 * Clover Coverage results for multiple files.
 */
abstract public class AbstractFileAggregatedMetrics extends AbstractClassAggregatedMetrics {
    private int files;

    public abstract FileCoverage findFileCoverage(String name);

    public int getFiles() {
        return files;
    }

    public void setFiles(int files) {
        this.files = files;
    }
}
