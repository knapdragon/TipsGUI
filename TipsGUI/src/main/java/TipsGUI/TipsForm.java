package TipsGUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TipsForm implements ActionListener {
    private final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
    private final double APPLICATION_SIZE_FRACTION = 0.8;

    private final JFrame frame;
    private JPanel contentPane;
    private final GridBagLayout layout;
    private final GridBagConstraints gbc;

    private JLabel tipsLabel;
    private JButton tipsSetButton;
    private Float tips = 0.00f;

    private JLabel acceptedFormatLabel;
    private final JScrollPane scrollPane;
    private DefaultTableModel employeeTableModel;
    private JTable employeeTable;
    private JButton addEmployeeButton;

    private JButton calculateDividedTips;
    private JButton testButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TipsForm::new);
    }

    /**
     * Adds a new component with grid bag constraints.
     * @param component The component to add
     * @param container What is being added to
     * @param layout A specified GridBagLayout
     * @param gbc Any pre-existing GridBagConstraints
     * @param gridX A specified gridX
     * @param gridY A specified gridY
     * @param gridWidth A specified gridWidth
     * @param gridHeight A specified gridHeight
     */
    public void addWithConstraints(Component component, Container container, GridBagLayout layout, GridBagConstraints gbc, int gridX, int gridY, int gridWidth, int gridHeight) {
        gbc.gridx = gridX;
        gbc.gridy = gridY;

        gbc.gridwidth = gridWidth;
        gbc.gridheight = gridHeight;

        layout.setConstraints(component, gbc);
        container.add(component);
    }

    /**
     * Creates a table with 13 columns and 0 rows
     * New rows are added to the table via addEmployeeButton
     */
    public void makeTable() {
        screenResolution.width = (int) (screenResolution.width * APPLICATION_SIZE_FRACTION);
        screenResolution.height = (int) (screenResolution.height * APPLICATION_SIZE_FRACTION);

        employeeTableModel = new DefaultTableModel();

        // Add column for employee name and each month
        // Each column represents the number of hours worked in that month
        String[] columnNames = {"Name", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December", "Tip Share"};
        for (String column : columnNames) {
            employeeTableModel.addColumn(column);
        }

        employeeTable = new JTable(employeeTableModel);
        employeeTable.setPreferredScrollableViewportSize(screenResolution);
        employeeTable.setFillsViewportHeight(true);

        // Create a listener for the table, converting input values into hours and minutes
        employeeTableModel.addTableModelListener(e -> {
            if (employeeTable.isEditing()) {
                int currentRow = employeeTable.getSelectedRow();
                int currentColumn = employeeTable.getSelectedColumn();
                Object value = employeeTable.getValueAt(currentRow, currentColumn);

                // Leave name fields alone
                if (employeeTable.getSelectedColumn() != 0 && employeeTable.getSelectedColumn() != 13) {
                    // Check if input value follows format
                    if (String.valueOf(value).matches("\\d+[hH]\\s\\d{0,2}[mM]")) {
                        // Get the value of the input work hours as a float so we can properly turn it into minutes and hours as Duration
                        Duration newCellValue = getDuration(value);
                        System.out.println(newCellValue);   // TODO: make this usable in the table

                        //employeeTable.setValueAt(newCellValue, currentRow, currentColumn);
                    } else {
                        String msg = "New value must match the accepted format! e.g. 128 hours and 31 minutes = 128h 31m";
                        JOptionPane.showMessageDialog(null, msg);
                    }
                }
            }
        });
    }

    /**
     * Get the value of a cell in employeeTable as a Duration
     * @param value The cell's value
     * @return The value as a Duration
     */
    private static Duration getDuration(Object value) {
        String valueString = String.valueOf(value).toLowerCase();

        // Extract the hours and minutes from the value
        long hours = Integer.parseInt(
                valueString.substring(0, valueString.indexOf('h'))
        );

        long minutes = Integer.parseInt(
                valueString.substring(
                        valueString.indexOf('h') + 2, valueString.indexOf('m')
                )
        );

        return Duration.ofHours(hours).plusMinutes(minutes);
    }

    /**
     * Open an input dialog asking the user to input the range of months to calculate from.
     * Converts said months into integers for use in calculateTips().
     */
    public void openMonthSelectDialog() {
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};;
        // Dropdown menus for months; set default to January to avoid NullPointerException
        JComboBox fromMonthField = new JComboBox(months);
        JComboBox toMonthField = new JComboBox(months);
        fromMonthField.setSelectedItem(months[0]);
        toMonthField.setSelectedItem(months[0]);

        JPanel calcPanel = new JPanel();
        calcPanel.add(new JLabel("From:"));
        calcPanel.add(fromMonthField);
        calcPanel.add(Box.createHorizontalStrut(15)); // layout
        calcPanel.add(new JLabel("To:"));
        calcPanel.add(toMonthField);

        int result = JOptionPane.showConfirmDialog(null, calcPanel,"Select the months to calculate tips for:", JOptionPane.OK_CANCEL_OPTION);
        String startingMonth;
        String endingMonth;
        if (result == JOptionPane.OK_OPTION) {
            // Get months as first three characters for use in parsing
            startingMonth = fromMonthField.getSelectedItem().toString().substring(0, 3);
            endingMonth = toMonthField.getSelectedItem().toString().substring(0, 3);

            // Create a case-insensitive parser to turn months into
            DateTimeFormatter parser = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("MMM")
                    .toFormatter(Locale.ENGLISH);

            // Get the starting month as integer (e.g. February = 2)
            TemporalAccessor startAccessor = parser.parse(startingMonth);
            int startingMonthIndex = startAccessor.get(ChronoField.MONTH_OF_YEAR);

            // Get the ending month as integer
            TemporalAccessor endAccessor = parser.parse(endingMonth);
            int endingMonthIndex = endAccessor.get(ChronoField.MONTH_OF_YEAR);

            calculateTips(startingMonthIndex, endingMonthIndex);
        }
    }

    /**
     * TODO: finish this method
     * Populate the 'Tips share' column of each row with the amount that person is owed of the available tips.
     * @param startingMonthIndex The index of the month to start calculating from
     * @param endingMonthIndex The index of the month to end calculating at
     */
    public void calculateTips(int startingMonthIndex, int endingMonthIndex) {
        // TODO: get sum total of all hours in specified columns, then divide by tips to get ratio
        // Then multiply by a specific person's hours
        Map<String, Duration> peopleTotalHours = new HashMap<>();

        // Go through each employee, then record their name and a running total of their hours
        for (int row = 0; row < employeeTable.getRowCount(); row++) {
            String employeeName = String.valueOf(employeeTable.getValueAt(row, 0));
            Duration currentTotal = Duration.ofHours(0L);

            // Total hours in the given month range
            if (startingMonthIndex < endingMonthIndex) {
                // e.g. March (3) to September (9)
                // TODO: fix this method. With January - March as selections, causes StringIndexOutOfBounds
                for (int col = startingMonthIndex; col < endingMonthIndex; col++) {
                    String currentCell = String.valueOf(employeeTable.getValueAt(row, col));
                    Duration currentHours;
                    if (currentCell.matches("\\d+[hH]\\s\\d{0,2}[mM]")) {
                        currentHours = getDuration(employeeTable.getValueAt(row, col));
                    } else {
                        currentHours = getDuration(0);
                    }
                    currentTotal = currentTotal.plus(currentHours);
                }
            } else {
                // TODO: solve this
                // e.g. October (10) to February (2)
                int monthDifference = (int) ChronoUnit.MONTHS.between(
                    YearMonth.from(LocalDate.of(2025, endingMonthIndex, 1)),
                    YearMonth.from(LocalDate.of(2025, startingMonthIndex, 1))
                );
                // 'Roll over' the array, somehow?
                for (int col = 0; col < monthDifference % 12; col++) {
                    Duration currentCell = getDuration(employeeTable.getValueAt(row, col));
                    currentTotal = currentTotal.plus(currentCell);
                }
            }

            // Add this person's total hours to hashmap
            peopleTotalHours.put(employeeName, currentTotal);
        }
        System.out.println(peopleTotalHours);
    }

    public TipsForm() {
        screenResolution.width = (int) (screenResolution.width * APPLICATION_SIZE_FRACTION);
        screenResolution.height = (int) (screenResolution.height * APPLICATION_SIZE_FRACTION);
        contentPane = new JPanel();

        // Set up frame
        frame = new JFrame("Tips Calculator");
        frame.setVisible(true);
        frame.add(contentPane, BorderLayout.CENTER);
        frame.setSize(screenResolution);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set up JPanel and its layout
        layout = new GridBagLayout();
        contentPane.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
        contentPane.setLayout(layout);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 50;
        gbc.weighty = 100;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;

        // Create label indicating the tips available, should change when set via 'Set' button
        String label = String.format("Tips available: %s", tips.toString());
        tipsLabel = new JLabel(label);

        // Listen for user action and perform respective command
        tipsSetButton = new JButton("Set");
        tipsSetButton.setActionCommand("SET_TIPS_AMOUNT");
        tipsSetButton.addActionListener(this);

        // Show accepted table cell format
        acceptedFormatLabel = new JLabel("An example of accepted format for monthly hours: 128h 00m");

        // Establish table
        makeTable();
        scrollPane = new JScrollPane(employeeTable);

        // Listen for user action and perform respective command
        addEmployeeButton = new JButton("Add employee");
        addEmployeeButton.setActionCommand("ADD_NEW_EMPLOYEE");
        addEmployeeButton.addActionListener(this);

        // Add button to calculate tips
        calculateDividedTips = new JButton("Calculate tips according to hours worked");
        calculateDividedTips.setActionCommand("CALCULATE_TIPS");
        calculateDividedTips.addActionListener(this);

        // test table content editing works correctly
        testButton = new JButton("Test Button, Please Ignore");
        testButton.setActionCommand("TEST_BTN");
        testButton.addActionListener(this);

        // Add components to appropriate panels
        JPanel topPane = new JPanel();
        topPane.setLayout(new FlowLayout());
        this.addWithConstraints(tipsSetButton, topPane, layout, gbc, 0, 0, 1, 1);
        this.addWithConstraints(tipsLabel, topPane, layout, gbc, 1, 0, 3, 1);

        JPanel tablePane = new JPanel();
        this.addWithConstraints(acceptedFormatLabel, tablePane, layout, gbc, 0, 1, 3, 1);
        this.addWithConstraints(scrollPane, tablePane, layout, gbc, 0, 0, 0, 0);

        JPanel bottomPane = new JPanel();
        this.addWithConstraints(addEmployeeButton, bottomPane, layout, gbc, 3, 0, 2, 1);
        this.addWithConstraints(calculateDividedTips, bottomPane, layout, gbc, 3, 0, 2, 1);
        this.addWithConstraints(testButton, bottomPane, layout, gbc, 3, 0, 2, 1);

        frame.add(topPane, BorderLayout.NORTH);
        this.addWithConstraints(tablePane, contentPane, layout, gbc, 0, 0, 5, 1);
        frame.add(bottomPane, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand().toUpperCase().trim();

        switch (command) {
            case "SET_TIPS_AMOUNT":
                // Set 'tips' value to the input, as a Float
                this.tips = Float.valueOf(JOptionPane.showInputDialog("Tips available:"));
                tipsLabel.setText(String.format("Tips available: %s", this.tips));
                break;
            case "ADD_NEW_EMPLOYEE":
                // Ask for name of new addition
                String employeeName = JOptionPane.showInputDialog("Enter employee name:");
                Employee employee = new Employee(employeeName);
                // Add a row with the provided name as the input for the first column; remaining cells in that row set to 0
                Object[] newRow = new Object[]{
                        employee.getName(), "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m"
                };
                employeeTableModel.addRow(newRow);
                break;
            case "CALCULATE_TIPS":
                openMonthSelectDialog();
                break;
            case "TEST_BTN":
                // Currently, prints content of the first row as an array
                ArrayList<Object> testRow = new ArrayList<Object>();
                for (int col = 1; col < employeeTable.getColumnCount(); col++) {
                    Object colData = employeeTableModel.getValueAt(0, col);
                    testRow.add(colData);
                }
                System.out.println(testRow);
                break;
            default:
                System.out.println("Unknown command");
                break;
        }
    }
}
