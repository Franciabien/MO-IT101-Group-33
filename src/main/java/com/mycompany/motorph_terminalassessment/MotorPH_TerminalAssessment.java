package com.mycompany.motorph_terminalassessment;

import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.time.LocalTime;
import java.time.Duration;
import java.util.ArrayList;

public class MotorPH_TerminalAssessment {

    private static ArrayList<Employee> employeeList = new ArrayList<>();
    private static final String EMP_FILE = "EmployeeDetails.csv";
    private static final String ATT_FILE = "AttendanceRecord.csv";

    public static void main(String[] args) {
        loadEmployeeRecords();
        
        Scanner input = new Scanner(System.in);

        System.out.println("=== MotorPH Payroll System ===");
        System.out.print("Username: ");
        String user = input.nextLine().trim().toLowerCase(); 
        System.out.print("Password: ");
        String pass = input.nextLine().trim();

        if (pass.equals("12345") && (user.contains("employee") || user.contains("payroll_staff"))) {
            System.out.println("\nLogin Successful. Total Records Loaded: " + employeeList.size());
            
            System.out.print("\nEnter Employee ID: ");
            String searchID = input.nextLine().trim();
            
            Employee foundEmp = findEmployeeById(searchID);

            if (foundEmp != null) {
                System.out.print("Enter Month (1-12): ");
                String month = input.nextLine().trim();
                if (month.length() == 1) month = "0" + month;

                System.out.print("Select Cutoff (1 = 1st Cutoff, 2 = 2nd Cutoff): ");
                int period = input.nextInt();
                
                calculateAndDisplayPayroll(foundEmp, month, period);
            } else {
                System.out.println("Error: Employee ID [" + searchID + "] not found.");
            }
        } else {
            System.out.println("Invalid Credentials.");
        }
        input.close();
    }

    private static Employee findEmployeeById(String id) {
        for (Employee e : employeeList) {
            if (e.getId().equals(id)) return e;
        }
        return null;
    }

    public static void loadEmployeeRecords() {
        File file = new File(EMP_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); 
            String line;
            while ((line = reader.readLine()) != null) {
                // Regex split helps handle commas that are inside names in the CSV
                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (columns.length > 18) {
                    employeeList.add(new Employee(
                        columns[0].trim(), 
                        columns[2].trim() + " " + columns[1].trim(), 
                        Double.parseDouble(columns[18].trim())
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading Employee CSV.");
        }
    }

    public static void calculateAndDisplayPayroll(Employee emp, String month, int period) {
        double totalMonthlyHours = 0;
        
        try (BufferedReader br = new BufferedReader(new FileReader(ATT_FILE))) {
            br.readLine(); 
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols[0].trim().equals(emp.getId()) && cols[3].trim().startsWith(month + "/")) {
                    LocalTime timeIn = parseTime(cols[4].trim());
                    LocalTime timeOut = parseTime(cols[5].trim());
                    
                    double duration = (Duration.between(timeIn, timeOut).toMinutes() / 60.0) - 1.0;
                    if (duration > 0) totalMonthlyHours += duration;
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading Attendance CSV.");
        }

        double monthlyGross = totalMonthlyHours * emp.getRate();
        double sss = 0, philhealth = 0, pagibig = 0, tax = 0;

        // Apply government deductions only for the 2nd cutoff period
        if (period == 2 && monthlyGross > 0) {
            sss = (monthlyGross > 24750) ? 1125 : monthlyGross * 0.045;
            philhealth = monthlyGross * 0.025;
            pagibig = 100.0;
            
            double taxableIncome = monthlyGross - (sss + philhealth + pagibig);
            if (taxableIncome > 20833) {
                tax = (taxableIncome - 20833) * 0.20;
            }
        }

        double currentGross = monthlyGross / 2;
        double totalDeductions = sss + philhealth + pagibig + tax;
        double netPay = Math.max(0, currentGross - totalDeductions);

        System.out.println("\n========================================");
        System.out.println("          PAYROLL SUMMARY               ");
        System.out.println("========================================");
        System.out.println("Employee Name  : " + emp.getName());
        System.out.println("Employee ID    : " + emp.getId());
        System.out.println("Total Hours    : " + String.format("%,.2f", totalMonthlyHours));
        System.out.println("Gross Income   : PHP " + String.format("%,.2f", currentGross));
        System.out.println("Total Deduct   : PHP " + String.format("%,.2f", totalDeductions));
        System.out.println("----------------------------------------");
        System.out.println("NET PAY        : PHP " + String.format("%,.2f", netPay));
        System.out.println("========================================");
    }

    // Helper to fix time format if the CSV uses 1-digit hours like 8:00
    private static LocalTime parseTime(String timeStr) {
        if (timeStr.contains(":") && timeStr.indexOf(":") == 1) {
            timeStr = "0" + timeStr;
        }
        return LocalTime.parse(timeStr.substring(0, 5));
    }
}

class Employee {
    private final String id;
    private final String name;
    private final double rate;

    public Employee(String id, String name, double rate) {
        this.id = id;
        this.name = name;
        this.rate = rate;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getRate() { return rate; }
}