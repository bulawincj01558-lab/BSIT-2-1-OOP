

public class InsufficientFundsException extends Exception {

    private final double shortfall;

    public InsufficientFundsException(double shortfall) {
        super("Insufficient funds. You are short by PHP "
                + String.format("%.2f", shortfall));

        /
    }


}