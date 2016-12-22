package com.stormpath.sdk.convert

import org.testng.annotations.Test

import static org.testng.Assert.*

/**
 * @since 1.3.0
 */
class ConversionTest {

    @Test
    void testCoverage() {
        new Conversions(); //only for code coverage
    }

    @Test
    void testName() {
        String name = 'foo'
        def c = new Conversion(name: name)
        assertEquals c.getName(), name
    }

    @Test
    void testStrategyString() {
        String s = 'all'
        def c = new Conversion()
        assertSame c.getStrategy(), ConversionStrategyName.SCALARS //default
        c.setStrategy(s)
        assertSame c.getStrategy(), ConversionStrategyName.ALL
    }

    @Test(expectedExceptions = [IllegalArgumentException])
    void testInvalidStrategyString() {
        new Conversion().setStrategy('foo')
    }

    @Test
    void testSetFields() {
        def fields = ['foo': new Conversion()]
        def c = new Conversion(fields: fields)
        assertSame c.getFields(), fields
    }

    @Test
    void testNullFieldsSetsEmptyMap() {
        def c = new Conversion(fields: null)
        assertNotNull c.getFields()
        assertTrue c.getFields().isEmpty()
    }
}
