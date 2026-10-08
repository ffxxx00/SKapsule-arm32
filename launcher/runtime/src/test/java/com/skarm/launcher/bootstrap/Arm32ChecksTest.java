package com.skarm.launcher.bootstrap;

import org.junit.Test;

public class Arm32ChecksTest {
    @Test public void mathSupportsDecoderTablesAndProjection() { Arm32Checks.checkMath(); }
    @Test public void repeatedMathRetainsCorrectResults() { Arm32Checks.checkWarmMath(); }
    @Test public void imageRoundtripPreservesColorAndAlpha() throws Exception { Arm32Checks.checkPixels(); }
    @Test public void textProducesVisiblePixels() { Arm32Checks.checkFonts(); }
}
