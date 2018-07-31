package hudson.plugins.clover;

import junit.framework.TestCase;

/**
 * JUnit test for {@link Ratio}
 */
public class RatioTest extends TestCase {

    private final void assertRatio(Ratio r, float numerator, float denominator) {
        assertEquals(numerator, r.numerator);
        assertEquals(denominator, r.denominator);
    }

    public void testParseValue() throws Exception {
        assertRatio(Ratio.create(1,2), 1.0f, 2.0f);
    }
}