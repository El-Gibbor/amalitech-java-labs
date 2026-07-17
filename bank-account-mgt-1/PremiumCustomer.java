public class PremiumCustomer extends Customer {
    private double minimumBalance = 10000.0;

    public PremiumCustomer(String name, int age, String contact, String address) {
        super(name, age, contact, address);
    }

    public void setMinimumBalance(double minimumBalance) {
       this.minimumBalance = minimumBalance;
    }

    public double getMinimumBalance() {
        return minimumBalance;
    }

    @Override
    public void displayCustomerDetails() {
        System.out.println("Customer ID: " + getCustomerId());
        System.out.println("Name: " + getName());
        System.out.println("Age: " + getAge());
        System.out.println("Contact: " + getContact());
        System.out.println("Address: " + getAddress());
        System.out.println("Type: " + getCustomerType());
        System.out.println("Minimum Balance: " + getMinimumBalance());
        System.out.println("Waived Fees: " + hasWaivedFees());
    }

    @Override
    public String getCustomerType() {
        return "Premium";
    }

    @Override
    public boolean hasWaivedFees() {
        return true;
    }
}