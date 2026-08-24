

public abstract class Account {


    String accountNumber;
    String ownerName;
    double balance;

    public Account(String accountNumber, String ownerName, double openingBalance) {

        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = openingBalance;
    }



    public void deposit(double amount) {

    }

    public void withdraw(double amount) throws InsufficientFundsException {

    }


    public abstract String getAccountType();


    protected void applyWithdrawal(double amount) {
        // balance = balance - amount;
    }

    @Override
    public String toString() {
        return getAccountType() + " " + accountNumber + " (" + ownerName + ")";
    }
}