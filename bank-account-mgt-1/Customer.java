public abstract class Customer {
    private String customerId;
    private String name;
    private int age;
    private String contact;
    private String address;

    private static int customerCounter = 0;

    public Customer(String name, int age, String contact, String address) {
        customerCounter++;
        this.customerId = String.format("CUS%03d", customerCounter);
        this.name = name;
        this.age = age;
        this.contact = contact;
        this.address = address;
    }

    // getters & setters for customer fields
    public String getCustomerId() { return customerId; }

    public String getName() { return name; }

    public void setName(String name) {
        if ( name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        this.name = name;
    }

    public int getAge() { return age; }

    public void setAge(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative.");
        }
        this.age = age;
    }

    public String getContact() { return contact; }

    public void setContact(String contact) {
        if ( contact == null || contact.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact cannot be null or empty.");
        }
        this.contact = contact;
    }

    public String getAddress() { return address; }

    public void setAddress(String address) {
        if ( address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be null or empty.");
        }
        this.address = address;
    }

    public abstract void displayCustomerDetails();
    public abstract String getCustomerType();

    public boolean hasWaivedFees() {
        return false; // Default, but overridden by subclasses
    }
}

