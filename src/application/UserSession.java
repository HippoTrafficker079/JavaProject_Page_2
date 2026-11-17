package application;

public class UserSession {
    public static String lecturerName = "Dr. Hla Myint";
    public static String departmentCode = "CEIT";
    public static String departmentFullName = "Computer Engineering and Information Technology";

    public static void setDepartmentByCode(String code) {
        departmentCode = code;
        departmentFullName = switch (code.toUpperCase()) {
            case "CEIT" -> "Computer Engineering and Information Technology";
            case "CE"   -> "Civil Engineering";
            case "ME"   -> "Mechanical Engineering";
            case "EP"   -> "Electrical Power Engineering";
            case "EC"   -> "Electronic Engineering";
            case "MC"   -> "Mechatronic Engineering";
            default     -> code;
        };
    }

    public static String getFormattedDepartment() {
        return departmentCode + " (" + departmentFullName + ")";
    }
}