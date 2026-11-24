package application;

public class UserSession {
    public static int userId = 1;
    public static String lecturerName = "Dr. Hla Myint";
    public static String departmentCode = "CEIT";
    public static String departmentFullName = "Computer Engineering and Information Technology";

    public static void setDepartmentByCode(String code) {
        if (code == null || code.trim().isEmpty())
            return;
        String clean = code.trim();
        departmentCode = switch (clean.toUpperCase()) {
            case "C", "CE" -> "C";
            case "MECH", "ME" -> "Mech";
            case "EC" -> "EC";
            case "CEIT" -> "CEIT";
            case "EP" -> "EP";
            case "MCE", "MC" -> "McE";
            case "CHE" -> "ChE";
            case "TEX" -> "Tex";
            case "MN" -> "Mn";
            case "PE" -> "PE";
            case "ARCH" -> "Arch";
            case "TELE" -> "Tele";
            case "MET" -> "Met";
            case "FE" -> "FE";
            default -> clean;
        };

        departmentFullName = switch (departmentCode.toUpperCase()) {
            case "C" -> "Civil Engineering";
            case "MECH" -> "Mechanical Engineering";
            case "EC" -> "Electronic Engineering";
            case "CEIT" -> "Computer Engineering and Information Technology";
            case "EP" -> "Electrical Power Engineering";
            case "MCE" -> "Mechatronic Engineering";
            case "CHE" -> "Chemical Engineering";
            case "TEX" -> "Textile Engineering";
            case "MN" -> "Mining Engineering";
            case "PE" -> "Petroleum Engineering";
            case "ARCH" -> "Architecture";
            case "TELE" -> "Telecommunication Engineering";
            case "MET" -> "Metallurgical Engineering and Materials Science";
            case "FE" -> "Food Engineering";
            default -> departmentCode;
        };
    }

    public static String getFormattedDepartment() {
        return departmentCode + " (" + departmentFullName + ")";
    }
}