import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class BusReservationSystem {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/bus_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== BUS RESERVATION SYSTEM ===");
            System.out.println("1. Display Bus & Booking Details");
            System.out.println("2. Book Seats");
            System.out.println("3. Cancel Booking");
            System.out.println("4. Add New Bus");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); 

            switch (choice) {
                case 1:
                    displayDetails();
                    break;
                case 2:
                    bookSeats(scanner);
                    break;
                case 3:
                    cancelBooking(scanner);
                    break;
                case 4:
                    addBus(scanner);
                    break;
                case 5:
                    System.out.println("Exiting system. Goodbye!");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver not found in classpath: " + e.getMessage());
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void displayDetails() {
        String fetchBusesSql = "SELECT * FROM buses";
        String fetchBookingsSql = "SELECT * FROM bookings";

        try (Connection conn = getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(fetchBusesSql);
             ResultSet rs1 = stmt1.executeQuery()) {

            System.out.println("\n--- AVAILABLE BUSES ---");
            System.out.printf("%-12s | %-25s | %-12s | %-12s | %-15s%n", 
                    "Bus Number", "Route", "Travel Date", "Total Seats", "Available Seats");
            System.out.println("----------------------------------------------------------------------------------");
            
            while (rs1.next()) {
                System.out.printf("%-12s | %-25s | %-12s | %-12d | %-15d%n",
                        rs1.getString("bus_number"),
                        rs1.getString("route"),
                        rs1.getDate("travel_date"),
                        rs1.getInt("total_seats"),
                        rs1.getInt("available_seats"));
            }

            try (PreparedStatement stmt2 = conn.prepareStatement(fetchBookingsSql);
                 ResultSet rs2 = stmt2.executeQuery()) {

                System.out.println("\n--- RECENT BOOKINGS ---");
                System.out.printf("%-12s | %-12s | %-20s | %-12s | %-20s%n", 
                        "Booking ID", "Bus Number", "Passenger Name", "Seats", "Booking Date");
                System.out.println("----------------------------------------------------------------------------------");

                boolean hasBookings = false;
                while (rs2.next()) {
                    hasBookings = true;
                    System.out.printf("%-12d | %-12s | %-20s | %-12d | %-20s%n",
                            rs2.getInt("booking_id"),
                            rs2.getString("bus_number"),
                            rs2.getString("passenger_name"),
                            rs2.getInt("seats_booked"),
                            rs2.getTimestamp("booking_date"));
                }
                if (!hasBookings) {
                    System.out.println("No active bookings found.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching details: " + e.getMessage());
        }
    }

    private static void bookSeats(Scanner scanner) {
        System.out.print("Enter Bus Number: ");
        String busNumber = scanner.nextLine().trim();
        System.out.print("Enter Passenger Name: ");
        String passengerName = scanner.nextLine().trim();
        System.out.print("Enter Number of Seats to Book: ");
        int seatsToBook = scanner.nextInt();
        scanner.nextLine();

        if (seatsToBook <= 0) {
            System.out.println("Invalid number of seats.");
            return;
        }

        String checkSeatsSql = "SELECT available_seats FROM buses WHERE bus_number = ? FOR UPDATE";
        String updateBusSql = "UPDATE buses SET available_seats = available_seats - ? WHERE bus_number = ?";
        String insertBookingSql = "INSERT INTO bookings (bus_number, passenger_name, seats_booked) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); 

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSeatsSql)) {
                checkStmt.setString(1, busNumber);
                ResultSet rs = checkStmt.executeQuery();

                if (!rs.next()) {
                    System.out.println("Error: Bus with number " + busNumber + " not found.");
                    conn.rollback();
                    return;
                }

                int availableSeats = rs.getInt("available_seats");
                if (availableSeats < seatsToBook) {
                    System.out.println("Booking failed: Only " + availableSeats + " seats available.");
                    conn.rollback();
                    return;
                }

                try (PreparedStatement updateStmt = conn.prepareStatement(updateBusSql)) {
                    updateStmt.setInt(1, seatsToBook);
                    updateStmt.setString(2, busNumber);
                    updateStmt.executeUpdate();
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(insertBookingSql)) {
                    insertStmt.setString(1, busNumber);
                    insertStmt.setString(2, passengerName);
                    insertStmt.setInt(3, seatsToBook);
                    insertStmt.executeUpdate();
                }

                conn.commit(); 
                System.out.println("Successfully booked " + seatsToBook + " seat(s) for " + passengerName + "!");

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Transaction failed and rolled back: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void cancelBooking(Scanner scanner) {
        System.out.print("Enter Booking ID to cancel: ");
        int bookingId = scanner.nextInt();
        scanner.nextLine();

        String selectBookingSql = "SELECT bus_number, seats_booked FROM bookings WHERE booking_id = ? FOR UPDATE";
        String deleteBookingSql = "DELETE FROM bookings WHERE booking_id = ?";
        String restoreSeatsSql = "UPDATE buses SET available_seats = available_seats + ? WHERE bus_number = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); 

            try (PreparedStatement selectStmt = conn.prepareStatement(selectBookingSql)) {
                selectStmt.setInt(1, bookingId);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next()) {
                    System.out.println("Error: Booking ID " + bookingId + " not found.");
                    conn.rollback();
                    return;
                }

                String busNumber = rs.getString("bus_number");
                int seatsToRestore = rs.getInt("seats_booked");

                try (PreparedStatement restoreStmt = conn.prepareStatement(restoreSeatsSql)) {
                    restoreStmt.setInt(1, seatsToRestore);
                    restoreStmt.setString(2, busNumber);
                    restoreStmt.executeUpdate();
                }

                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteBookingSql)) {
                    deleteStmt.setInt(1, bookingId);
                    deleteStmt.executeUpdate();
                }

                conn.commit(); 
                System.out.println("Booking ID " + bookingId + " cancelled successfully. " + seatsToRestore + " seat(s) restored.");

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Transaction failed and rolled back: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void addBus(Scanner scanner) {
        System.out.print("Enter Bus Number: ");
        String busNumber = scanner.nextLine().trim();
        System.out.print("Enter Route (e.g., CityA to CityB): ");
        String route = scanner.nextLine().trim();
        System.out.print("Enter Travel Date (YYYY-MM-DD): ");
        String travelDate = scanner.nextLine().trim();
        System.out.print("Enter Total Seats: ");
        int totalSeats = scanner.nextInt();
        scanner.nextLine();

        String sql = "INSERT INTO buses (bus_number, route, travel_date, total_seats, available_seats) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, busNumber);
            stmt.setString(2, route);
            stmt.setDate(3, java.sql.Date.valueOf(travelDate));
            stmt.setInt(4, totalSeats);
            stmt.setInt(5, totalSeats);

            stmt.executeUpdate();
            System.out.println("Bus " + busNumber + " added successfully!");
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid date format. Use YYYY-MM-DD.");
        } catch (SQLException e) {
            System.err.println("Error adding bus: " + e.getMessage());
        }
    }
}