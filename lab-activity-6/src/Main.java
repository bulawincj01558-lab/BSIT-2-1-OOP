import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ArrayList<vehicle> vehicles = new ArrayList<>();
        Scanner input = new Scanner(System.in);
        int choice = 0;

        while (choice != 5) {
            System.out.println();
            System.out.println("===== VEHICLE MANAGER =====");
            System.out.println("1 - Add Car");
            System.out.println("2 - Add Motorcycle");
            System.out.println("3 - Remove a Vehicle");
            System.out.println("4 - Display All Vehicles");
            System.out.println("5 - Exit");
            System.out.print("Choose an option: ");
            choice = input.nextInt();
            input.nextLine();

            if (choice == 1) {
                System.out.print("Brand: ");
                String brand = input.nextLine();
                System.out.print("Year: ");
                int year = input.nextInt();
                System.out.print("Number of doors: ");
                int doors = input.nextInt();
                vehicles.add(new car(brand, year, doors));
                System.out.println(">> Car added!");
            }
            else if (choice == 2) {
                System.out.print("Brand: ");
                String brand = input.nextLine();
                System.out.print("Year: ");
                int year = input.nextInt();
                System.out.print("Has sidecar? (true/false): ");
                boolean sidecar = input.nextBoolean();
                vehicles.add(new motorcycle(brand, year, sidecar));
                System.out.println(">> Motorcycle added!");
            }
            else if (choice == 3) {
                System.out.print("Enter the number to remove: ");
                int number = input.nextInt();
                if (number >= 1 && number <= vehicles.size()) {
                    vehicles.remove(number - 1);
                    System.out.println(">> Removed!");
                } else {
                    System.out.println(">> Invalid number.");
                }
            }
            else if (choice == 4) {
                System.out.println("--- All Vehicles ---");
                if (vehicles.isEmpty()) {
                    System.out.println("(none yet)");
                }
                for (int i = 0; i < vehicles.size(); i++) {
                    System.out.print((i + 1) + ". ");
                    vehicles.get(i).displayInfo();
                }
            }
        }

        System.out.println("Goodbye!");
        input.close();
    }
}