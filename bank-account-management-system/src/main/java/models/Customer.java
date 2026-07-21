package models;

/**
 * Abstract base identity type shared by {@code RegularCustomer} and
 * {@code PremiumCustomer}, holding personal details and an auto-generated
 * customer ID.
 */
public abstract class Customer {
    private String customerId;
    private String name;
    private int age;
    private String contact;
    private String address;

    private static int customerCounter = 0;

    /** Creates a customer with an auto-generated customer ID. */
    public Customer(String name, int age, String contact, String address) {
        customerCounter++;
        this.customerId = String.format("CUS%03d", customerCounter);
        this.name = name;
        this.age = age;
        this.contact = contact;
        this.address = address;
    }

    public String getCustomerId() { return customerId; }

    public String getName() { return name; }

    /** @throws IllegalArgumentException if name is null or blank */
    public void setName(String name) {
        requireNonBlank(name, "Name");
        this.name = name;
    }

    public int getAge() { return age; }

    /** @throws IllegalArgumentException if age is negative */
    public void setAge(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative.");
        }
        this.age = age;
    }

    public String getContact() { return contact; }

    /** @throws IllegalArgumentException if contact is null or blank */
    public void setContact(String contact) {
        requireNonBlank(contact, "Contact");
        this.contact = contact;
    }

    public String getAddress() { return address; }

    /** @throws IllegalArgumentException if address is null or blank */
    public void setAddress(String address) {
        requireNonBlank(address, "Address");
        this.address = address;
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty.");
        }
    }

    /** Prints this customer's full details to the console. */
    public abstract void displayCustomerDetails();

    /** @return the customer type name, e.g. "Regular" or "Premium" */
    public abstract String getCustomerType();

    /** @return true if monthly fees are waived for this customer; false by default */
    public boolean hasWaivedFees() {
        return false;
    }
}
