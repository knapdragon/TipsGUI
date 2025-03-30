package TipsGUI;
import java.util.ArrayList;

public class Employee {
    private String name;
    /**
     * A list representing the 12 months.
     * Each element in the array is the number of hours the employee has worked.
     */
    private final ArrayList<Float> hoursWorkedPerMonth;
    private int monthNumber;

    public Employee(String nameInput) {
        this.name = nameInput;
        hoursWorkedPerMonth = new ArrayList<Float>(12);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the list of hours worked in each month of the year.
     * @return The list.
     */
    public ArrayList<?> getHoursWorkedPerMonth() {
        return hoursWorkedPerMonth;
    }

    /**
     * Get the hours worked in a specific month.
     * @return The hours as a float.
     */
    public float getMonthlyHours(int monthNumber) {
        return hoursWorkedPerMonth.get(monthNumber - 1);
    }

    /**
     * Set the hours worked by an employee for a given month.
     * @param hoursWorkedPerMonth The employee's list of hours per month.
     * @param monthNumber The month to set the hours worked.
     * @param hours The hours worked.
     */
    public void setMonthlyHours(ArrayList<?> hoursWorkedPerMonth, int monthNumber, Float hours) {
        this.hoursWorkedPerMonth.set(monthNumber - 1, hours);
    }

    /**
     *  Converts a month in string to its respective number.
     * @param month The month to convert to an integer reflecting the month number.
     * @return The month number.
     */
    public int monthStringToInt(String month) {
        int monthNumber = 0;
        switch (month.toLowerCase()) {
            case "january":
                monthNumber = 1;
            case "february":
                monthNumber = 2;
            case "march":
                monthNumber = 3;
            case "april":
                monthNumber = 4;
            case "may":
                monthNumber = 5;
            case "june":
                monthNumber = 6;
            case "july":
                monthNumber = 7;
            case "august":
                monthNumber = 8;
            case "september":
                monthNumber = 9;
            case "october":
                monthNumber = 10;
            case "november":
                monthNumber = 11;
            case "december":
                monthNumber = 12;
            default:
                System.out.println("Invalid month");
        }
        return monthNumber;
    }
}

