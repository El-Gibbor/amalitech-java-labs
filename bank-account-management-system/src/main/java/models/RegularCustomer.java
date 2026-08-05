package models;

/** Standard customer type with no benefits beyond the Customer defaults. */
public class RegularCustomer extends Customer {
    /** Creates a regular customer. */
    public RegularCustomer(String name, int age, String contact, String address, String email) {
        super(name, age, contact, address, email);
    }

    /** Reconstructs a regular customer with an identity assigned earlier, for example when loading one previously saved to a file. */
    public RegularCustomer(String customerId, String name, int age, String contact, String address, String email) {
        super(customerId, name, age, contact, address, email);
    }

    @Override
    public void displayCustomerDetails() {
        System.out.println("Customer ID: " + getCustomerId());
        System.out.println("Name: " + getName());
        System.out.println("Age: " + getAge());
        System.out.println("Contact: " + getContact());
        System.out.println("Email: " + getEmail());
        System.out.println("Address: " + getAddress());
        System.out.println("Type: " + getCustomerType());
    }

    @Override
    public String getCustomerType() {
        return "Regular";
    }
}
