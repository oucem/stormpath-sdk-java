package com.stormpath.sdk.convert;

/**
 * A utility method class that enhances working with and constructing {@link Conversion} rules/chains.
 *
 * @since 1.3.0
 */
public final class Conversions {

    //prevent public instantiation:
    private Conversions() {
    }

    /**
     * Returns a new {@code Conversion} that is disabled.
     *
     * @return a new {@code Conversion} that is disabled.
     */
    public static Conversion disabled() {
        return new Conversion().setEnabled(false);
    }

    /**
     * Returns a new {@code Conversion} instance that contains a field that should be converted according to the
     * specified {@code conversion} argument.  That is, the specified argument applies to the object's named field,
     * not the object itself.
     *
     * @param name the name of a field on an object to convert
     * @param c    the conversion rules to apply when encountering the named field's value
     * @return a new {@code Conversion} instance that contains a field that should be converted according to the
     * specified {@code conversion} argument.
     */
    public static Conversion withField(String name, Conversion c) {
        return new Conversion().withField(name, c);
    }

    /**
     * Returns a new {@code Conversion} instance that uses the specified strategy.
     *
     * @param name the name of the strategy to use when converting an input object.
     * @return a new {@code Conversion} instance that uses the specified strategy.
     */
    public static Conversion withStrategy(ConversionStrategyName name) {
        return new Conversion().setStrategy(name);
    }

    /**
     * Returns a new {@code ElementsConversion} instance where {@link ElementsConversion#each each} element will
     * be converted according to the specified {@code conversion} argument.
     *
     * @param conversion a new {@code ElementsConversion} instance where {@link ElementsConversion#each each} element
     *                   will be converted according to the specified {@code conversion} argument.
     * @return a new {@code ElementsConversion} instance where {@link ElementsConversion#each each} element will
     * be converted according to the specified {@code conversion} argument.
     */
    public static ElementsConversion each(Conversion conversion) {
        return new ElementsConversion().setEach(conversion);
    }

}
