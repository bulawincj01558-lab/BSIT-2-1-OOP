package atm;

public class SavingsAccount extends Account {

    public static final double MAINTAINING_BALANCE = 500.0;

    private double interestRate;   // example: 0.02 means 2% per year

    public SavingsAccount(String accountNumber, String ownerName,
                          double openingBalance, double interestRate) {
        // TODO 1: call the parent constructor FIRST
        //   super(accountNumber, ownerName, openingBalance);
        // TODO 2: then store interestRate in this object
    }

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        // TODO 3: compute what the balance WOULD become:
        //   double remaining = getBalance() - amount;
        // If remaining < MAINTAINING_BALANCE, throw a new
        // InsufficientFundsException with the shortfall as the argument.
        // Otherwise reuse the parent logic:
        //   super.withdraw(amount);
    }

    public double monthlyInterest() {
        // TODO 4: return getBalance() * interestRate / 12;
        return 0;
    }

    // THINK ABOUT IT (answer as comments):
    // Why is getAccountType() abstract in Account instead of returning "ACCOUNT"?
    // Why can this class call super.withdraw() and applyWithdrawal(),
    // but not read the private balance field by name?
}