package hudson.plugins.clover;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RatioTest {

    private void assertRatio(Ratio r, float numerator, float denominator) {
        assertEquals(numerator, r.numerator, 1e-6);
        assertEquals(denominator, r.denominator, 1e-6);
    }

    @Test
    public void testParseValue() throws Exception {
        assertRatio(Ratio.create(1,2), 1.0f, 2.0f);
    }
}