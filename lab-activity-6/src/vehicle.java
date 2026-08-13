public class vehicle {
    protected String brand;
    protected int year;

    public vehicle(String brand, int year) {
        this.brand = brand;
        this.year = year;
    }

    public void displayInfo() {
        System.out.println("Vehicle: " + brand + " (" + year + ")");
    }
}