

public class CheckingAccount extends Account {

    private double overdraftLimit;   // example: 1000.0

    public CheckingAccount(String accountNumber, String ownerName,
                           double openingBalance, double overdraftLimit) {

    }

    @Override
    public String getAccountType() {

        return "";
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundsException {

    }
}