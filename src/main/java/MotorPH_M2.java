import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.time.LocalTime;
import java.time.Duration;

public class MotorPH_M2 {

    static String[] ids = new String[50];
    static String[] names = new String[50];
    static double[] rates = new double[50];
    static int totalCount = 0;

    // File Names
    static String empFileName = "EmployeeDetails.csv";
    static String attFileName = "AttendanceRecord.csv";

    public static void main(String[] args) {
        loadEmployeeRecords();
        Scanner input = new Scanner(System.in);

        System.out.println("=== MotorPH Payroll System ===");
        System.out.print("Username: ");
        String user = input.nextLine();
        System.out.print("Password: ");
        String pass = input.nextLine();

        if (pass.equals("12345") && (user.equals("employee") || user.equals("payroll_staff"))) {
            System.out.println("\nLogin Successful. Welcome, " + user);
            
            System.out.print("\nEnter Employee ID: ");
            String searchID = input.nextLine();
            
            System.out.print("Enter Month Number (1-12): ");
            String searchMonth = input.nextLine();
            if (searchMonth.length() == 1) searchMonth = "0" + searchMonth;

            int index = -1;
            for (int i = 0; i < totalCount; i++) {
                if (ids[i].equals(searchID)) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                // Modified to ask for the cutoff type
                System.out.print("Enter Cutoff (1 for 1st Cutoff, 2 for 2nd Cutoff): ");
                int cutoff = input.nextInt();
                executePayrollCalculation(searchID, names[index], rates[index], searchMonth, cutoff);
            } else {
                System.out.println("Error: Employee record not found.");
            }
        } else {
            System.out.println("Access Denied: Invalid credentials.");
        }
    }

    public static String findFilePath(String fileName) {
        String[] possiblePaths = {
            fileName,
            "src/" + fileName,
            "src/main/java/" + fileName
        };

        for (String path : possiblePaths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        return null;
    }

    public static void loadEmployeeRecords() {
        String path = findFilePath(empFileName);
        
        if (path == null) {
            System.out.println("System Error: " + empFileName + " not found in project folders.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            reader.readLine(); 
            int i = 0;
            while ((line = reader.readLine()) != null && i < 50) {
                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                ids[i] = columns[0].trim();
                names[i] = columns[2].trim() + " " + columns[1].trim();
                rates[i] = Double.parseDouble(columns[18].trim()); 
                i++;
            }
            totalCount = i;
        } catch (Exception e) {
            System.out.println("Error reading employee data.");
        }
    }

    public static void executePayrollCalculation(String id, String name, double hourlyRate, String month, int cutoff) {
        String path = findFilePath(attFileName);
        double totalHours = 0;

        if (path == null) {
            System.out.println("System Error: " + attFileName + " not found.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); 
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                String logDate = cols[3].trim();
                
                if (cols[0].trim().equals(id) && logDate.startsWith(month + "/")) {
                    LocalTime timeIn = normalizeTime(cols[4].trim());
                    LocalTime timeOut = normalizeTime(cols[5].trim());
                    
                    long minutes = Duration.between(timeIn, timeOut).toMinutes();
                    double dailyHours = (minutes / 60.0) - 1.0; 
                    
                    if (dailyHours > 0) {
                        totalHours += dailyHours;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading attendance records.");
        }

        // Apply Logic: Deductions based on one month gross salary
        double monthlyGross = totalHours * hourlyRate;
        double sss = 0, philhealth = 0, pagibig = 0, tax = 0;

        // Apply deductions ONLY if it is the 2nd cutoff
        if (cutoff == 2) {
            sss = (monthlyGross > 24750) ? 1125 : monthlyGross * 0.045; 
            philhealth = monthlyGross * 0.025;
            pagibig = 100.00;
            
            // Tax calculation (Simplified placeholder for taxable income)
            double taxableIncome = monthlyGross - (sss + philhealth + pagibig);
            if (taxableIncome > 20833) {
                tax = (taxableIncome - 20833) * 0.20;
            }
        }

        // Net Salary calculation
        // For 1st cutoff, net is gross/2 (or however your specific payroll splits the gross)
        // For 2nd cutoff, it's the remaining balance minus all monthly deductions
        double currentPeriodGross = (cutoff == 1) ? (monthlyGross / 2) : (monthlyGross / 2);
        double netSalary = currentPeriodGross - sss - philhealth - pagibig - tax;

        System.out.println("\n------------------------------------");
        System.out.println("       PAYROLL SUMMARY REPORT       ");
        System.out.println("------------------------------------");
        System.out.println("Employee Name:   " + name.toUpperCase());
        System.out.println("Employee ID:     " + id);
        System.out.println("Hourly Rate:     PHP " + String.format("%.2f", hourlyRate));
        System.out.println("Total Hours:     " + String.format("%.2f", totalHours));
        System.out.println("Cutoff:          " + (cutoff == 1 ? "1st Cutoff" : "2nd Cutoff"));
        System.out.println("------------------------------------");
        System.out.println("MONTHLY GROSS:   PHP " + String.format("%.2f", monthlyGross));
        System.out.println("DEDUCTIONS (Applied on 2nd Cutoff):");
        System.out.println("  SSS:           PHP " + String.format("%.2f", sss));
        System.out.println("  PhilHealth:    PHP " + String.format("%.2f", philhealth));
        System.out.println("  Pag-IBIG:      PHP " + String.format("%.2f", pagibig));
        System.out.println("  Withholding Tax: PHP " + String.format("%.2f", tax));
        System.out.println("------------------------------------");
        System.out.println("NET SALARY:      PHP " + String.format("%.2f", netSalary));
        System.out.println("------------------------------------");
    }

    public static LocalTime normalizeTime(String timeStr) {
        if (timeStr.contains(":") && timeStr.indexOf(":") == 1) {
            timeStr = "0" + timeStr;
        }
        if (timeStr.length() > 5) {
            timeStr = timeStr.substring(0, 5);
        }
        return LocalTime.parse(timeStr);
    }
}