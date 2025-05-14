package TipsGUI;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBeanBuilder;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.List;

public class TipsForm implements ActionListener {
    // Screen dimensions
    private final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
    private final double APPLICATION_SIZE_FRACTION = 0.8;

    // Main application + layout
    private final JFrame frame;
    private JPanel contentPane;
    private final GridBagLayout layout;
    private final GridBagConstraints gbc;

    // Part of topPanel
    private JLabel tipsLabel;
    private JButton tipsSetButton;
    private Float tips = 0.00f;
    private JButton importReplacingButton;
    private JButton importAddingButton;

    // Centre panel
    private final JScrollPane scrollPane;
    private DefaultTableModel employeeTableModel;
    private JTable employeeTable;

    // Bottom panel
    private JButton addEmployeeButton;
    private JButton calculateDividedTips;
    private JButton testButton;
    private JButton exportButton;

    public static class CsvEmployee {
        @CsvBindByName(column = "Name")
        private String name;

        @CsvBindByName(column = "January")
        private String januaryDuration;

        @CsvBindByName(column = "February")
        private String februaryDuration;

        @CsvBindByName(column = "March")
        private String marchDuration;

        @CsvBindByName(column = "April")
        private String aprilDuration;

        @CsvBindByName(column = "May")
        private String mayDuration;

        @CsvBindByName(column = "June")
        private String juneDuration;

        @CsvBindByName(column = "July")
        private String julyDuration;

        @CsvBindByName(column = "August")
        private String augustDuration;

        @CsvBindByName(column = "September")
        private String septemberDuration;

        @CsvBindByName(column = "October")
        private String octoberDuration;

        @CsvBindByName(column = "November")
        private String novemberDuration;

        @CsvBindByName(column = "December")
        private String decemberDuration;

        @CsvBindByName(column = "Tip Share")
        private String tipShare;


    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TipsForm::new);
    }

    /**
     * Creates a table with 13 columns and 0 rows
     * New rows are added to the table via addEmployeeButton
     */
    private void makeTable() {
        screenResolution.width = (int) (screenResolution.width * APPLICATION_SIZE_FRACTION);
        screenResolution.height = (int) (screenResolution.height * APPLICATION_SIZE_FRACTION);

        employeeTableModel = new DefaultTableModel();

        /*
        * Add column for employee name and each month
        * Each column represents the number of hours worked in that month
        */
        String[] columnNames = {"Name", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December", "Tip Share"};
        for (String column : columnNames) {
            employeeTableModel.addColumn(column);
        }

        employeeTable = new JTable(employeeTableModel);
        employeeTable.setPreferredScrollableViewportSize(screenResolution);
        employeeTable.setFillsViewportHeight(true);

        // Create a listener for the table to prevent incorrect formats
        Action checkCellFormat = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TableCellListener tcl = (TableCellListener) e.getSource();

                JTable table = tcl.getTable();
                int row = tcl.getRow();
                int col = tcl.getColumn();
                Object oldValue = tcl.getOldValue();
                Object newValue = tcl.getNewValue();

                // Ignore "Name" and "Tip share" columns
                if (col != 0 && col != 13) {
                    if (!(String.valueOf(newValue).matches("\\d+[hH]\\s\\d{0,2}[mM]"))) {
                        // Alert if value doesn't match accepted format
                        JOptionPane.showMessageDialog(
                                null,
                                "New value must match the accepted format! e.g. 128 hours and 31 minutes = 128h 31m",
                                "Format error",
                                JOptionPane.ERROR_MESSAGE);
                        // Set cell value back to what it was before edit
                        table.setValueAt(oldValue, row, col);
                    }
                }
            }
        };

        TableCellListener listener = new TableCellListener(employeeTable, checkCellFormat);
        employeeTableModel.addTableModelListener(listener.getTable());
    }

    /**
     * Asks the user for the employee's name, then creates a new row
     * in the table with the input as the value for the first column.
     * Takes no arguments: the method handles the input string.
     */
    private void newEmployee() {
        // Ask for name of new addition
        String employeeName = JOptionPane.showInputDialog("Enter employee name:");
        if (employeeName != null && !(employeeName.isEmpty())) {
            Employee employee = new Employee(employeeName);

            // Add a row with the provided name as the input for the first column; remaining cells in that row set to 0h 00m
            Object[] newRow = new Object[]{
                    employee.getName(), "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "£0.00"
            };
            employeeTableModel.addRow(newRow);
        } else if (employeeName != null) {
            // Alert if the name field is empty
            JOptionPane.showMessageDialog(
                    null,
                    "Employee name must not be empty!",
                    "Field error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Get the value of a cell in employeeTable as a Duration.
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
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

        // Dropdown menus for months; set default to January to avoid NullPointerException
        JComboBox<String> fromMonthField = new JComboBox<>(months);
        JComboBox<String> toMonthField = new JComboBox<>(months);
        fromMonthField.setSelectedItem(months[0]);
        toMonthField.setSelectedItem(months[11]);

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

            calculateHours(startingMonthIndex, endingMonthIndex);
        }
    }

    /**
     * Calculate the total and individual hours of each employee, for use in calculating their share of tips.
     * @param startingMonthIndex The index of the month to start calculating from
     * @param endingMonthIndex The index of the month to end calculating at
     */
    public void calculateHours(int startingMonthIndex, int endingMonthIndex) {
        // A list of all employees and their total time worked
        Map<String, Duration> individualTotalWorked = Collections.synchronizedMap(new LinkedHashMap<>());

        // Go through each employee, then record their name and a running total of their hours
        for (int row = 0; row < employeeTable.getRowCount(); row++) {
            String employeeName = String.valueOf(employeeTable.getValueAt(row, 0));
            Duration currentTotal = Duration.ofHours(0L);

            // Gather the total of hours in the given month range
            if (startingMonthIndex == endingMonthIndex) { /* e.g. January to January, that is, only get one month's hours */
                String currentCell = String.valueOf(employeeTable.getValueAt(row, startingMonthIndex));
                Duration currentHours;
                if (currentCell.matches("\\d+[hH]\\s\\d{0,2}[mM]")) {
                    currentHours = getDuration(employeeTable.getValueAt(row, startingMonthIndex));
                } else {
                    currentHours = getDuration(0);
                }
                currentTotal = currentTotal.plus(currentHours);
            } else if (startingMonthIndex < endingMonthIndex) { /* e.g. March (3) to September (9) */
                for (int col = startingMonthIndex; col <= endingMonthIndex; col++) {
                    String currentCell = String.valueOf(employeeTable.getValueAt(row, col));
                    Duration currentHours;
                    if (currentCell.matches("\\d+[hH]\\s\\d{0,2}[mM]")) {
                        currentHours = getDuration(employeeTable.getValueAt(row, col));
                    } else {
                        currentHours = getDuration(0);
                    }
                    currentTotal = currentTotal.plus(currentHours);
                }
            } else { /* e.g. October (10) to February (2) */
                // Modulo operator 'wraps around' the array
                for (int col = startingMonthIndex; col != endingMonthIndex + 1; col = (col + 1) % 12) {
                    Duration currentCell = getDuration(employeeTable.getValueAt(row, col));
                    currentTotal = currentTotal.plus(currentCell);
                }
            }

            // Add this person's total hours to hashmap
            individualTotalWorked.put(employeeName, currentTotal);
        }

        calculateTips(individualTotalWorked);
    }

    /**
     * Populate the 'Tips share' column of each row with the amount that person is owed of the available tips.
     * @param individualTotalWorked The total duration worked for each employee.
     */
    private void calculateTips(Map<String, Duration> individualTotalWorked) {
        // Sum the time worked of all employees, then divide by the tips available to get a ratio
        Duration totalWorkedHours = Duration.ofHours(0);
        for (Map.Entry<String, Duration> employee : individualTotalWorked.entrySet()) {
            totalWorkedHours = totalWorkedHours.plus(employee.getValue());
        }
        float totalWorkedHoursFloat = (float) totalWorkedHours.toMinutes() / 60;
        float tipsRatio = tips / totalWorkedHoursFloat;

        // Update "Tips share" column for each employee with their hours multiplied by the ratio
        BigDecimal share;
        // Optionally recalculate total tip shares to ensure it matches tipsAvailable
        //BigDecimal recalculatedTips = BigDecimal.valueOf(0);
        // Using a LinkedHashMap, use mapIndex as the row as it will follow the map's insertion order.
        int mapIndex = 0;
        for (String key : individualTotalWorked.keySet()) {
            Duration d = individualTotalWorked.get(key);
            float time = (float) d.toMinutes() / 60;
            share = BigDecimal.valueOf(time * tipsRatio)
                    .round(new MathContext(4, RoundingMode.HALF_EVEN));

            //recalculatedTips = recalculatedTips.add(share);

            Object shareToShow = String.format("£%.2f", share);
            employeeTable.setValueAt(shareToShow, mapIndex, 13);

            mapIndex += 1;
        }

        //System.out.println(recalculatedTips);
    }

    /**
     * Create 10 test employees.
     * Names follow the pattern "Test [number]".
     * A random number of months are given a random time value.
     * Also automatically prompts tips input and month selection, then calculates tips.
     */
    private void seedTestData() {
        Object inputValue = JOptionPane.showInputDialog("Tips available:");
        if (inputValue == null) {
            // Alert if tips input cancelled
            JOptionPane.showMessageDialog(
                    null,
                    "Tips field was not filled. Cancelling test data creation.",
                    "Field error",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            tips = Float.valueOf(inputValue.toString());
            tipsLabel.setText(String.format("Tips available: %s", tips));

            for (int i = 0; i < 10; i++) {
                String testName = "Test " + (i+1);

                Random randNum = new Random();
                // Result should range between 1 and 12 (nextInt lower bound starts at 0 inclusive, upper bound is exclusive).
                int randomMonth = randNum.nextInt(12) + 1;
                // Following variable is redundant but being kept for clarity
                int numColumnsToChange = randomMonth;
                // Use a set to prevent duplicate months from appearing
                Set<Integer> affectedMonths = new HashSet<>();
                while (affectedMonths.size() < numColumnsToChange) {
                    affectedMonths.add(randNum.nextInt(12) + 1);
                }

                Object[] testRow = new Object[]{
                        testName, "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "0h 00m", "£0.00"
                };

                for (Integer monthIndex : affectedMonths) {
                    int randomHour = randNum.nextInt(11);
                    int randomMin1 = randNum.nextInt(6);
                    int randomMin2 = randNum.nextInt(10);
                    Object timeValue = String.format("%dh %d%dm", randomHour, randomMin1, randomMin2);

                    testRow[monthIndex] = timeValue;
                }
                employeeTableModel.addRow(testRow);
            }

            openMonthSelectDialog();
        }
    }

    /***
     * Add all recognised employees from an imported CSV file to the table.
     * @param listOfNewEmployees The beans list.
     */
    private void addCsvEmployeesToTable(List<CsvEmployee> listOfNewEmployees) {
        for (CsvEmployee csvEmp : listOfNewEmployees) {
            Object[] newRow = new Object[]{
                csvEmp.name,
                csvEmp.januaryDuration,
                csvEmp.februaryDuration,
                csvEmp.marchDuration,
                csvEmp.aprilDuration,
                csvEmp.mayDuration,
                csvEmp.juneDuration,
                csvEmp.julyDuration,
                csvEmp.augustDuration,
                csvEmp.septemberDuration,
                csvEmp.octoberDuration,
                csvEmp.novemberDuration,
                csvEmp.decemberDuration,
                csvEmp.tipShare
            };
            employeeTableModel.addRow(newRow);
        }
    }

    /***
     * Import a CSV file into the table.
     * @param replacesTable True if the import resets the table and sets it to the file contents. False if it adds onto the existing table.
     */
    private void importFile(boolean replacesTable) {
        JFileChooser chooser = new JFileChooser();

        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String fileName = file.getName();
            String fileType = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

            if (fileType.equals("csv")) {
                try {
                    List<CsvEmployee> beans = new CsvToBeanBuilder(new FileReader(fileName))
                            .withType(CsvEmployee.class).build().parse();
                    if (replacesTable) {
                        int rowCount = employeeTableModel.getRowCount();
                        for (int i = 0; i < rowCount; i++) {
                            employeeTableModel.removeRow(0);
                        }
                    }
                    addCsvEmployeesToTable(beans);

                } catch (Exception e) {
                    // File alert
                    JOptionPane.showMessageDialog(
                            null,
                            "No such file exists",
                            "File error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Exports the table as a CSV file.
     */
    private void exportFile() {
        JFileChooser chooser = new JFileChooser();
        int returnValue = chooser.showSaveDialog(frame);

        String fileName;
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getName();

            try (CSVWriter writer = new CSVWriter(new FileWriter(fileName))) {
                // As in makeTable()
                String[] headers = {"Name", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December", "Tip Share"};
                writer.writeNext(headers);

                String[] line = new String[14];
                for (int row = 0; row < employeeTableModel.getRowCount(); row++) {
                    for (int col = 0; col < employeeTableModel.getColumnCount(); col++) {
                        line[col] = String.valueOf(employeeTable.getValueAt(row, col));
                    }

                    writer.writeNext(line);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "No such file exists",
                        "File error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Adds a new component with grid bag constraints.
     * @param component The component to add
     * @param container What is being added to
     * @param layout A specified GridBagLayout
     * @param gbc Any pre-existing GridBagConstraints
     * @param gridX The grid column
     * @param gridY The grid row
     * @param gridWidth The number of columns the cell uses
     * @param gridHeight The number of rows the cell uses
     */
    private void addWithConstraints(Component component, Container container, GridBagLayout layout, GridBagConstraints gbc,
                                    int gridX, int gridY, int gridWidth, int gridHeight) {
        gbc.gridx = gridX;
        gbc.gridy = gridY;

        gbc.gridwidth = gridWidth;
        gbc.gridheight = gridHeight;

        layout.setConstraints(component, gbc);
        container.add(component);
    }

    /**
     * Adds a new component with grid bag constraints including insets for external padding.
     * @param component The component to add
     * @param container What is being added to
     * @param layout A specified GridBagLayout
     * @param gbc Any pre-existing GridBagConstraints
     * @param gridX The grid column
     * @param gridY The grid row
     * @param gridWidth The number of columns the cell uses
     * @param gridHeight The number of rows the cell uses
     * @param insets The external padding of the component
     */
    private void addWithConstraints(Component component, Container container, GridBagLayout layout, GridBagConstraints gbc,
                                    int gridX, int gridY, int gridWidth, int gridHeight, Insets insets) {
        gbc.gridx = gridX;
        gbc.gridy = gridY;

        gbc.gridwidth = gridWidth;
        gbc.gridheight = gridHeight;

        gbc.insets = insets;

        layout.setConstraints(component, gbc);
        container.add(component);
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

        // Add import buttons
        importReplacingButton = new JButton("Import CSV (replace table)");
        importReplacingButton.setActionCommand("IMPORT_FILE");
        importReplacingButton.addActionListener(this);

        importAddingButton = new JButton("Import CSV (add to existing)");
        importAddingButton.setActionCommand("IMPORT_FILE_ADD");
        importAddingButton.addActionListener(this);

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

        exportButton = new JButton("Export table");
        exportButton.setActionCommand("EXPORT_TABLE");
        exportButton.addActionListener(this);

        // test table content editing works correctly
        testButton = new JButton("Fill table with example data");
        testButton.setActionCommand("TEST_BTN");
        testButton.addActionListener(this);

        // Button to delete selected row from table
        JButton deleteRowButton = new JButton("Delete selected row");
        deleteRowButton.setActionCommand("DELETE_ROW");
        deleteRowButton.addActionListener(this);

        // Add components to appropriate panels
        GridBagLayout topLayout = new GridBagLayout();
        GridBagConstraints topGbc = new GridBagConstraints();
        JPanel topPane = new JPanel(topLayout);
        this.addWithConstraints(tipsSetButton, topPane, topLayout, topGbc, 0, 0, 1, 1, new Insets(2,0,0,0));
        this.addWithConstraints(tipsLabel, topPane, topLayout, topGbc, 1, 0, 2, 1, new Insets(2,5,0,0));
        this.addWithConstraints(importReplacingButton, topPane, topLayout, topGbc, 0, 1, 3, 1, new Insets(5,0,0,0));
        this.addWithConstraints(importAddingButton, topPane, topLayout, topGbc, 0, 2, 3, 1, new Insets(5,0,0,0));

        JPanel tablePane = new JPanel();
        this.addWithConstraints(scrollPane, tablePane, layout, gbc, 0, 0, 0, 0);

        JPanel bottomPane = new JPanel();
        this.addWithConstraints(addEmployeeButton, bottomPane, layout, gbc, 0, 0, 2, 1);
        this.addWithConstraints(calculateDividedTips, bottomPane, layout, gbc, 1, 0, 2, 1);
        this.addWithConstraints(exportButton, bottomPane, layout, gbc, 1, 1, 2, 1);
        this.addWithConstraints(testButton, bottomPane, layout, gbc, 2, 0, 2, 1);
        this.addWithConstraints(deleteRowButton, bottomPane, layout, gbc, 2, 1, 2, 1);

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
                Object inputValue = JOptionPane.showInputDialog("Do not include currency symbols! \n\nTips available:");
                if (inputValue != null && inputValue.toString().matches("\\d+.?(\\d{1,2})*")) {
                    tips = Float.valueOf(inputValue.toString());
                    tipsLabel.setText(String.format("Tips available: %s", tips));
                } else if (inputValue != null && !inputValue.toString().matches("\\d+.?(\\d{1,2})*")) {
                    // Alert user of syntax error
                    JOptionPane.showMessageDialog(
                            null,
                            "Tips was not filled out properly! \nUse standard currency format, but you don't need to include the currency type (such as £).",
                            "Syntax error",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    // Alert if tips input cancelled
                    JOptionPane.showMessageDialog(
                            null,
                            "Tips field was not filled.",
                            "Field error",
                            JOptionPane.ERROR_MESSAGE);
                }
                break;
            case "ADD_NEW_EMPLOYEE":
                newEmployee();
                break;
            case "CALCULATE_TIPS":
                openMonthSelectDialog();
                break;
            case "IMPORT_FILE":
                importFile(true);
                break;
            case "IMPORT_FILE_ADD":
                importFile(false);
                break;
            case "EXPORT_TABLE":
                exportFile();
                break;
            case "DELETE_ROW":
                int selectedRow = employeeTable.getSelectedRow();
                int result = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this row?");
                if (result == JOptionPane.OK_OPTION) {
                    employeeTableModel.removeRow(selectedRow);
                }
                break;
            case "TEST_BTN":
                seedTestData();
                break;
            default:
                System.out.println("Unknown command");
                break;
        }
    }
}
