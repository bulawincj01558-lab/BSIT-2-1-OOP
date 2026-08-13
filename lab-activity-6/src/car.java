public class car extends vehicle {
    private int numberOfDoors;

    public car(String brand, int year, int numberOfDoors) {
        super(brand, year);
        this.numberOfDoors = numberOfDoors;
    }

    @Override
    public void displayInfo() {
        System.out.println("Car: " + brand + " (" + year + ") - " + numberOfDoors + " doors");
    }
}