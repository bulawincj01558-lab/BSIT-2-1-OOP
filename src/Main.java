import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PaymentGateway gateway = new PaymentGateway();
        int nextId = 1001;
        int choice;

        do {
            System.out.println("=================================");
            System.out.println("          LICEO PAY");
            System.out.println("=================================");
            System.out.println("1. Make a payment");
            System.out.println("2. Print all receipts");
            System.out.println("3. Find a payment by ID");
            System.out.println("4. Show total collected");
            System.out.println("5. Refund all refundable payments");
            System.out.println("6. Show service fees");
            System.out.println("7. Exit");
            System.out.print("Choose an option: ");

            while (!sc.hasNextInt()) {
                System.out.print("Please enter a number: ");
                sc.next();
            }
            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1 -> {
                    System.out.println("1. GCash  2. Maya  3. Cash");
                    System.out.print("Payment type: ");
                    int type = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Payer name: ");
                    String name = sc.nextLine();
                    System.out.print("Amount: ");
                    double amount = sc.nextDouble();
                    sc.nextLine();

                    Payment payment;
                    if (type == 1) {
                        System.out.print("Mobile number: ");
                        String mobile = sc.nextLine();
                        payment = new GCashPayment(nextId, name, amount, mobile);
                    } else if (type == 2) {
                        System.out.print("Email: ");
                        String email = sc.nextLine();
                        payment = new MayaPayment(nextId, name, amount, email);
                    } else {
                        payment = new CashPayment(nextId, name, amount);
                    }

                    gateway.add(payment);
                    nextId++;
                    payment.printReceipt();
                    payment.printThankYou();
                }
                case 2 -> gateway.processAll();
                case 3 -> {
                    System.out.print("Enter ID to find: ");
                    int id = sc.nextInt();
                    Payment found = gateway.findById(id);
                    if (found != null) {
                        System.out.println("Found:");
                        found.printReceipt();
                    } else {
                        System.out.println("No payment with ID " + id + ".");
                    }
                }
                case 4 -> System.out.printf("Total collected: PHP %.2f%n", gateway.totalCollected());
                case 5 -> {
                    System.out.println("Refunding every payment that can be refunded:");
                    gateway.refundAll();
                }
                case 6 -> gateway.showServiceFees();
                case 7 -> System.out.println("Goodbye!");
                default -> System.out.println("Invalid option. Try again.");
            }
            System.out.println();
        } while (choice != 7);

        sc.close();
    }
}