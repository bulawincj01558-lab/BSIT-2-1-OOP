

public class ATMService {



    public void deposit(Account account, double amount) {
        account.deposit(amount);
        System.out.printf("Deposited PHP %.2f%n", amount);
    }

    public void deposit(Account account, double amount, String note) {

    }



    public double depositAll(Account account, double... amounts) {
        double total = 0;

        return total;
    }



    public void tryToReplace(Account account) {
        account = new SavingsAccount("XX-000", "Ghost Account", 0, 0);
        System.out.println("Inside the method  : " + account);

    }

    public void addBonus(Account account, double bonus) {
        account.deposit(bonus);

    }

    /

    public void transfer(Account from, Account to, double amount)
            throws InsufficientFundsException {

    }
}