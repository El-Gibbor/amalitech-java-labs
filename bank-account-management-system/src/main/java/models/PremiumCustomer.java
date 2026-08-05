package models;

/**
 * A premium customer with enhanced benefits: a required minimum balance
 * and waived monthly fees.
 */
public class PremiumCustomer extends Customer {
    private double minimumBalance;

    /** Creates a premium customer with the standard minimum balance requirement. */
    public PremiumCustomer(String name, int age, String contact, String address) {
        super(name, age, contact, address);
        this.minimumBalance = 10000.0;
    }

    /** Reconstructs a premium customer with an identity assigned earlier, for example when loading one previously saved to a file. */
    public PremiumCustomer(String customerId, String name, int age, String contact, String address) {
        super(customerId, name, age, contact, address);
        this.minimumBalance = 10000.0;
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

    /** @return true; monthly fees are always waived for premium customers */
    @Override
    public boolean hasWaivedFees() {
        return true;
    }
}
