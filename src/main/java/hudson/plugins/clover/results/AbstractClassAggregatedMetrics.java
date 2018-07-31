package hudson.plugins.clover.results;

/**
 * Clover Coverage results for multiple classes.
 */
abstract public class AbstractClassAggregatedMetrics extends AbstractCloverMetrics {

    private int classes;
    private int loc;
    private int ncloc;

    abstract public ClassCoverage findClassCoverage(String name);

    public int getClasses() {
        return classes;
    }

    public void setClasses(int classes) {
        this.classes = classes;
    }

    public int getLoc() {
        return loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public int getNcloc() {
        return ncloc;
    }

    public void setNcloc(int ncloc) {
        this.ncloc = ncloc;
    }

}
