package hudson.plugins.clover;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Represents <tt>x/y</tt> where x={@link #numerator} and y={@link #denominator}.
 */
final public class Ratio implements Serializable, CoverageBarProvider {
    
    final float numerator;
    final float denominator;

    private static final NumberFormat PC_WIDTH_FORMAT = NumberFormat.getInstance(Locale.US);
    static {
        PC_WIDTH_FORMAT.setMaximumFractionDigits(1);
    }

    private Ratio(float numerator, float denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Gets "x/y" representation.
     */
    public String toString() {
        return print(numerator)+"/"+print(denominator);
    }

    private String print(float f) {
        int i = (int) f;
        if(i==f)
            return String.valueOf(i);
        else
            return String.valueOf(f);
    }

    /**
     * Gets the percentage in integer.
     */
    public String getPercentage1d() {
        return PC_WIDTH_FORMAT.format(getPercentageFloat());
    }

    public String getPercentageStr() {
        return denominator > 0 ? PC_WIDTH_FORMAT.format(getPercentageFloat()) + "%" : "-";
    }


    private String pcFormat(float pc) {
        return Ratio.PC_WIDTH_FORMAT.format(pc) + "%";
    }

    public String getPcWidth() {
        return pcFormat(getPercentageFloat());
    }


    public String getPcUncovered() {
        float pcUncovered = 100.0f - getPercentageFloat();
        return pcFormat(pcUncovered);
    }

    public String getPcCovered() {
        return getPercentageStr();
    }

    public String getHasData() {
        return "" + (denominator > 0);
    }


    /**
     * Gets the percentage in integer.
     */
    public int getPercentage() {
        return Math.round(getPercentageFloat());
    }

    /**
     * Gets the percentage in float.
     */
    public float getPercentageFloat() {
        if (denominator == 0.0f)
            return 0.0f; // fix the 0/0 case without being as big a hack!
        if (Float.compare(denominator, numerator) == 0)
            return 100; 
        return 100*numerator/denominator;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ratio ratio = (Ratio) o;

        return Float.compare(ratio.denominator, denominator)==0
            && Float.compare(ratio.numerator, numerator)==0;

    }

    public int hashCode() {
        int result;
        result = numerator != +0.0f ? Float.floatToIntBits(numerator) : 0;
        result = 31 * result + denominator != +0.0f ? Float.floatToIntBits(denominator) : 0;
        return result;
    }

    private static final long serialVersionUID = 1L;

//
// fly-weight patterns for common Ratio instances (x/y) where x<y
// and x,y are integers.
//
    private static final Ratio[] COMMON_INSTANCES = new Ratio[256];

    /**
     * Creates a new instance of {@link Ratio}.
     */
    public static Ratio create(float x, float y) {
        int xx= (int) x;
        int yy= (int) y;

        if(xx==x && yy==y) {
            int idx = yy * (yy + 1) / 2 + xx;
            if(0<=idx && idx<COMMON_INSTANCES.length) {
                Ratio r = COMMON_INSTANCES[idx];
                if(r==null)
                    COMMON_INSTANCES[idx] = r = new Ratio(x,y);
                return r;
            }
        }

        return new Ratio(x,y);
    }
}
