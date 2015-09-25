package hu.balygaby.projects.cyclepower.calculation.math_aid;

public class DecimalToFraction {
    private DecimalToFraction() {
    }

    public static NomDenom convert(double decimal) {
        int nominator = 0;
        int denominator = 0;
        for (int i = 1; i < 1025; i++) {
            double potentialNominatorDouble = i * decimal;
            long potentialNominatorLong = Math.round(potentialNominatorDouble);
            if (Math.abs(potentialNominatorDouble - (double) potentialNominatorLong) < 0.000001) {
                nominator = (int) potentialNominatorLong;
                denominator = i;
                break;
            }
        }
        return new NomDenom(nominator, denominator);
    }

    /**
     * Converts a decimal number to nominator-denominator format.
     *
     * @param decimal Decimal number to convert.
     * @return The nominator-denominator format separated with a slash (/).
     */
    public static String convertToString(double decimal) {
        NomDenom nd = convert(decimal);
        if (nd.getDenominator() == 0) return Double.toString(decimal);
        else if (nd.getDenominator() == 1) return Integer.toString(nd.getNominator());
        else return (nd.getNominator() + "/" + nd.getDenominator());
    }
}
