package id.saku.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureAndHysteresisTest {

    @Test
    fun nextCompactBarState_emptyList_staysExpanded() {
        assertFalse(
            nextCompactBarState(
                currentlyCompact = false,
                firstVisibleIndex = 0,
                firstVisibleOffsetPx = 0
            )
        )
    }

    @Test
    fun nextCompactBarState_hysteresisDeadband_preventsOscillation() {
        // Below enterOffsetPx (140) stays false
        assertFalse(nextCompactBarState(currentlyCompact = false, firstVisibleIndex = 0, firstVisibleOffsetPx = 139))
        // Reaches enterOffsetPx -> transitions to true
        assertTrue(nextCompactBarState(currentlyCompact = false, firstVisibleIndex = 0, firstVisibleOffsetPx = 140))

        // Inside deadband (49..139) holds true when already compact, false when expanded
        for (offset in 49..139) {
            assertTrue(
                "Expected compact=true to hold at offset=$offset",
                nextCompactBarState(currentlyCompact = true, firstVisibleIndex = 0, firstVisibleOffsetPx = offset)
            )
            assertFalse(
                "Expected compact=false to hold at offset=$offset",
                nextCompactBarState(currentlyCompact = false, firstVisibleIndex = 0, firstVisibleOffsetPx = offset)
            )
        }

        // Drops to <= exitOffsetPx (48) -> restores expanded
        assertFalse(nextCompactBarState(currentlyCompact = true, firstVisibleIndex = 0, firstVisibleOffsetPx = 48))
        assertFalse(nextCompactBarState(currentlyCompact = true, firstVisibleIndex = 0, firstVisibleOffsetPx = 0))

        // Past first item -> always compact regardless of offset
        assertTrue(nextCompactBarState(currentlyCompact = false, firstVisibleIndex = 1, firstVisibleOffsetPx = 0))
        assertTrue(nextCompactBarState(currentlyCompact = true, firstVisibleIndex = 5, firstVisibleOffsetPx = 12))
    }

    @Test
    fun nextCompactBarState_simulatedScrollTraces_noChatter() {
        // 3 items, 7 items, 20 items simulated forward and backward scroll offsets
        val forward = listOf(0 to 0, 0 to 40, 0 to 80, 0 to 139, 0 to 140, 1 to 20, 2 to 60)
        var state = false
        val forwardStates = forward.map { (idx, off) ->
            state = nextCompactBarState(state, idx, off)
            state
        }
        assertEquals(listOf(false, false, false, false, true, true, true), forwardStates)

        val backward = listOf(2 to 60, 1 to 10, 0 to 135, 0 to 90, 0 to 49, 0 to 48, 0 to 0)
        val backwardStates = backward.map { (idx, off) ->
            state = nextCompactBarState(state, idx, off)
            state
        }
        assertEquals(listOf(true, true, true, true, true, false, false), backwardStates)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nextCompactBarState_rejectsInvalidThresholds() {
        nextCompactBarState(
            currentlyCompact = false,
            firstVisibleIndex = 0,
            firstVisibleOffsetPx = 0,
            enterOffsetPx = 40,
            exitOffsetPx = 40
        )
    }

    @Test
    fun classifyScrubDrag_arbitratesTouchSlopAndAxes() {
        assertEquals(ScrubAxis.UNDECIDED, classifyScrubDrag(0f, 0f, 8f))
        assertEquals(ScrubAxis.UNDECIDED, classifyScrubDrag(8f, -8f, 8f))
        assertEquals(ScrubAxis.VERTICAL_SCROLL, classifyScrubDrag(2f, 25f, 8f))
        assertEquals(ScrubAxis.VERTICAL_SCROLL, classifyScrubDrag(-3f, -25f, 8f))
        // Diagonal tie (|dx| == |dy|) favors vertical scroll so LazyColumn is not hijacked
        assertEquals(ScrubAxis.VERTICAL_SCROLL, classifyScrubDrag(20f, 20f, 8f))
        assertEquals(ScrubAxis.HORIZONTAL_SCRUB, classifyScrubDrag(25f, 5f, 8f))
        assertEquals(ScrubAxis.HORIZONTAL_SCRUB, classifyScrubDrag(-25f, -5f, 8f))
    }

    @Test
    fun scrubBarIndex_mapsBoundsAndBins() {
        assertEquals(-1, scrubBarIndex(50f, 100f, 0))
        assertEquals(-1, scrubBarIndex(50f, 0f, 6))
        assertEquals(-1, scrubBarIndex(50f, -10f, 6))

        assertEquals(0, scrubBarIndex(-20f, 600f, 6))
        assertEquals(0, scrubBarIndex(0f, 600f, 6))
        assertEquals(0, scrubBarIndex(99.9f, 600f, 6))
        assertEquals(1, scrubBarIndex(100f, 600f, 6))
        assertEquals(5, scrubBarIndex(599f, 600f, 6))
        assertEquals(5, scrubBarIndex(900f, 600f, 6))

        assertEquals(0, scrubBarIndex(0f, 1200f, 12))
        assertEquals(6, scrubBarIndex(600f, 1200f, 12))
        assertEquals(11, scrubBarIndex(1250f, 1200f, 12))
    }

    @Test
    fun gyroTiltMath_clampsAndHandlesNaN() {
        val (nxZero, nyZero) = normalizeSensorTilt(0f, 0f)
        assertEquals(0f, nxZero, 1e-5f)
        assertEquals(0f, nyZero, 1e-5f)

        val (nxSat, nySat) = normalizeSensorTilt(-2.0f, 2.0f, maxAngleRad = 0.45f)
        assertEquals(1f, nxSat, 1e-5f)
        assertEquals(-1f, nySat, 1e-5f)

        val (nxNan, nyNan) = normalizeSensorTilt(Float.NaN, Float.NaN, maxAngleRad = -1f)
        assertEquals(0f, nxNan, 1e-5f)
        assertEquals(0f, nyNan, 1e-5f)

        val tilt = computeCardTilt(normX = 2.5f, normY = -2.5f, maxRotDeg = 8f, maxParallaxPx = 14f)
        assertEquals(8f, tilt.rotX, 1e-5f)
        assertEquals(8f, tilt.rotY, 1e-5f)
        assertEquals(14f, tilt.parallaxX, 1e-5f)
        assertEquals(-14f, tilt.parallaxY, 1e-5f)

        val nanTilt = computeCardTilt(Float.NaN, Float.NaN)
        assertEquals(0f, nanTilt.rotX, 1e-5f)
        assertEquals(0f, nanTilt.rotY, 1e-5f)
        assertEquals(0f, nanTilt.parallaxX, 1e-5f)
        assertEquals(0f, nanTilt.parallaxY, 1e-5f)
    }

    @Test
    fun isCardBackVisible_handlesFlipBoundariesAndWraparound() {
        assertFalse(isCardBackVisible(Float.NaN))
        assertFalse(isCardBackVisible(0f))
        assertFalse(isCardBackVisible(89.9f))
        assertTrue(isCardBackVisible(90f))
        assertTrue(isCardBackVisible(180f))
        assertTrue(isCardBackVisible(270f))
        assertFalse(isCardBackVisible(270.1f))
        assertFalse(isCardBackVisible(360f))
        assertTrue(isCardBackVisible(-180f))
        assertTrue(isCardBackVisible(450f))
    }
}
