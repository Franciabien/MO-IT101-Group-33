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
            System.out.println("\nLogin Successful.");
            
            System.out.print("\nEnter Employee ID: ");
            String searchID = input.nextLine();
            
            System.out.print("Enter Month (1-12): ");
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
                // Simplified prompt
                System.out.print("Is this for 1st or 2nd cutoff? (Enter 1 or 2): ");
                int cutoffNum = input.nextInt();
                executePayrollCalculation(searchID, names[index], rates[index], searchMonth, cutoffNum);
            } else {
                System.out.println("Employee ID not found.");
            }
        } else {
            System.out.println("Invalid Login.");
        }
    }

    public static String findFilePath(String fileName) {
        String[] possiblePaths = {fileName, "src/" + fileName, "src/main/java/" + fileName};
        for (String path : possiblePaths) {
            if (new File(path).exists()) return path;
        }
        return null;
    }

    public static void loadEmployeeRecords() {
        String path = findFilePath(empFileName);
        if (path == null) return;

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
            System.out.println("Error loading files.");
        }
    }

    public static void executePayrollCalculation(String id, String name, double rate, String month, int period) {
        String path = findFilePath(attFileName);
        double totalHrs = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); 
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols[0].trim().equals(id) && cols[3].trim().startsWith(month + "/")) {
                    LocalTime in = normalizeTime(cols[4].trim());
                    LocalTime out = normalizeTime(cols[5].trim());
                    long mins = Duration.between(in, out).toMinutes();
                    double hours = (mins / 60.0) - 1.0; 
                    if (hours > 0) totalHrs += hours;
                }
            }
        } catch (Exception e) {
            System.out.println("Attendance Error.");
        }

        // Logic based on teacher's rule
        double fullMonthGross = totalHrs * rate;
        double sss = 0, phil = 0, pagibig = 0, tax = 0;

        // Only calculate deductions if it is the 2nd period
        if (period == 2) {
            // Simplified SSS logic
            if (fullMonthGross > 24750) sss = 1125;
            else sss = fullMonthGross * 0.045;

            phil = fullMonthGross * 0.025;
            pagibig = 100.0;

            double taxable = fullMonthGross - (sss + phil + pagibig);
            if (taxable > 20833) tax = (taxable - 20833) * 0.20;
        }

        // Divide gross by 2 to get current cutoff pay
        double currentGross = fullMonthGross / 2;
        double netPay = currentGross - (sss + phil + pagibig + tax);

        System.out.println("\n--- Payroll Result ---");
        System.out.println("Name: " + name);
        System.out.println("ID: " + id);
        System.out.println("Total Monthly Hours: " + totalHrs);
        System.out.println("Full Monthly Gross: " + fullMonthGross);
        System.out.println("Current Cutoff Gross: " + currentGross);
        System.out.println("Deductions (SSS/Phil/Pag/Tax): " + (sss + phil + pagibig + tax));
        System.out.println("NET PAY: " + netPay);
    }

    public static LocalTime normalizeTime(String t) {
        if (t.contains(":") && t.indexOf(":") == 1) t = "0" + t;
        if (t.length() > 5) t = t.substring(0, 5);
        return LocalTime.parse(t);
    }
}