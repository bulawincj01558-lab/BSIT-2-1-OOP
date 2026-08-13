public class motorcycle extends vehicle {
    private boolean hasSidecar;

    public motorcycle(String brand, int year, boolean hasSidecar) {
        super(brand, year);
        this.hasSidecar = hasSidecar;
    }

    @Override
    public void displayInfo() {
        String sidecarText = hasSidecar ? "with sidecar" : "no sidecar";
        System.out.println("Motorcycle: " + brand + " (" + year + ") - " + sidecarText);
    }
}