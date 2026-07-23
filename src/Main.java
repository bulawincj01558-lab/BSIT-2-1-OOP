import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ArrayList<Student> students = new ArrayList<>();
        ArrayList<Course> courses = new ArrayList<>();
        HashMap<String, ArrayList<String>> enrollments = new HashMap<>();

        String[] validPrograms = {"BSIT", "BSCS"};

        int choice = -1;
        while (choice != 0) {
            printMenu();
            choice = Integer.parseInt(sc.nextLine());

            switch (choice) {
                case 1: {
                    System.out.println("--- REGISTER STUDENT ---");
                    System.out.print("Student ID:");
                    String id = sc.nextLine();

                    System.out.print("Full Name:");
                    String name = sc.nextLine();

                    String program;
                    while (true) {
                        System.out.print("Program:");
                        program = sc.nextLine();
                        if (isValidProgram(program, validPrograms)) {
                            break;
                        }
                        System.out.println("[ERROR] Invalid program. Allowed: "
                                + String.join(", ", validPrograms));
                    }

                    int yearLevel = -1;
                    while (true) {
                        System.out.print("Year Level:");
                        try {
                            yearLevel = Integer.parseInt(sc.nextLine());
                            if (yearLevel >= 1 && yearLevel <= 4) {
                                break;
                            }
                        } catch (NumberFormatException e) {
                        }
                        System.out.println("[ERROR] Year level must be between 1 and 4.");
                    }

                    students.add(new Student(id, name, program, yearLevel));
                    enrollments.put(id, new ArrayList<>());
                    System.out.println("[OK]Student registered successfully");
                    break;
                }

                case 2: {
                    System.out.println("--- ADD COURSE ---");
                    System.out.print("Course Code: ");
                    String code = sc.nextLine();

                    System.out.print("Title:");
                    String title = sc.nextLine();

                    int units = readInt(sc, "Units: ");
                    int capacity = readInt(sc, "Capacity: ");

                    courses.add(new Course(code, title, units, capacity));
                    System.out.println("[OK] Course added successfully!");
                    break;
                }

                case 3: {
                    System.out.println("===== ENROLL STUDENT =====");
                    System.out.print("Student ID:");
                    String id = sc.nextLine();
                    System.out.print("Course Code:");
                    String code = sc.nextLine();
                    Student s = findStudent(students, id);
                    Course c = findCourse(courses, code);

                    if (s == null) {
                        System.out.println("[ERROR]Student not found.");
                    } else if (c == null) {
                        System.out.println("[ERROR]Course not found.");
                    } else if (c.isFull()) {
                        System.out.println("[ERROR]Course " + code + " is already full.");
                    } else if (enrollments.get(id).contains(code)) {
                        System.out.println("[ERROR] Student is already enrolled in " + code + ".");
                    } else {
                        enrollments.get(id).add(code);
                        c.addOneEnrollee();
                        System.out.println("[OK] " + s.getFullName() + " enrolled in "
                                + code + " (" + c.getTitle() + ").");
                    }
                    break;
                }

                case 4: {
                    System.out.println("===== ALL STUDENTS =====");
                    if (students.isEmpty()) {
                        System.out.println("No students.");
                    } else {
                        for (Student s : students) {
                            System.out.println(s.describe());
                        }
                    }
                    break;
                }

                case 5: {
                    System.out.println("--- ALL COURSES ---");
                    if (courses.isEmpty()) {
                        System.out.println("No courses yet.");
                    } else {
                        for (Course c : courses) {
                            System.out.println(c.getCourseCode() + " | " + c.getTitle()
                                    + " | " + c.getUnits() + " units | "
                                    + c.getEnrolledCount() + "/" + c.getCapacity());
                        }
                    }
                    break;
                }

                case 6: {
                    System.out.print("Student ID: ");
                    String id = sc.nextLine();
                    Student s = findStudent(students, id);

                    if (s == null) {
                        System.out.println("[ERROR] Student not found.");
                        break;
                    }

                    System.out.println("===== STUDENT LOAD: " + s.getFullName() + " ====== ");
                    ArrayList<String> codes = enrollments.get(id);
                    int totalUnits = 0;

                    if (codes == null || codes.isEmpty()) {
                        System.out.println("No enrolled courses.");
                    } else {
                        for (String code : codes) {
                            Course c = findCourse(courses, code);
                            if (c != null) {
                                System.out.println(c.getCourseCode() + " " + c.getTitle()
                                        + " " + c.getUnits() + " units");
                                totalUnits += c.getUnits();
                            }
                        }
                        System.out.println("--------------------------------------");
                        System.out.println("Total Units: " + totalUnits);
                    }
                    break;
                }

                case 0:
                    System.out.println("Thank you for using the Liceo Enrollment System");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    static void printMenu() {
        System.out.println("=====================================");
        System.out.println("LICEO ENROLLMENT SYSTEM ");
        System.out.println("=====================================");
        System.out.println("[1] Register Student");
        System.out.println("[2] Add Course Offering");
        System.out.println("[3] Enroll Student to Course");
        System.out.println("[4] View All Students");
        System.out.println("[5] View All Courses");
        System.out.println("[6] View Student Load (Courses + Total Units)");
        System.out.println("[0] Exit");
        System.out.println("----------------------------------------");
        System.out.print("Enter choice: ");
    }

    static Student findStudent(ArrayList<Student> list, String id) {
        for (Student s : list) {
            if (s.getStudentId().equals(id)) {
                return s;
            }
        }
        return null;
    }

    static Course findCourse(ArrayList<Course> list, String code) {
        for (Course c : list) {
            if (c.getCourseCode().equals(code)) {
                return c;
            }
        }
        return null;
    }

    static boolean isValidProgram(String program, String[] validPrograms) {
        for (String p : validPrograms) {
            if (p.equalsIgnoreCase(program)) {
                return true;
            }
        }
        return false;
    }

    static int readInt(Scanner sc, String prompt) {
        int value = -1;
        while (true) {
            System.out.print(prompt);
            try {
                value = Integer.parseInt(sc.nextLine());
                break;
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Please enter a valid number.");
            }
        }
        return value;
    }
}