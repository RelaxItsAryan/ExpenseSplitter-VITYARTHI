import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;

/**
 * A plain-Java, console-based group expense splitter.
 * Save as ExpenseSplitter.java, then run:
 * javac ExpenseSplitter.java
 * java ExpenseSplitter
 */
public class ExpenseSplitter {
    private static final Scanner INPUT = new Scanner(System.in);
    private static final List<String> members = new ArrayList<>();
    private static final List<Expense> expenses = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("====================================");
        System.out.println("       GROUP EXPENSE SPLITTER");
        System.out.println("====================================");

        boolean running = true;
        while (running) {
            showMenu();
            int choice = readInt("Choose an option: ");

            switch (choice) {
                case 1 -> addMember();
                case 2 -> addExpense();
                case 3 -> showExpenses();
                case 4 -> showBalances();
                case 5 -> showSettlements();
                case 6 -> running = false;
                default -> System.out.println("Please choose a number from 1 to 6.");
            }
        }
        System.out.println("Goodbye! Your expenses were not saved after closing the program.");
    }

    private static void showMenu() {
        System.out.println("\n1. Add member");
        System.out.println("2. Add expense");
        System.out.println("3. View expenses");
        System.out.println("4. View balances");
        System.out.println("5. View settlement plan");
        System.out.println("6. Exit");
    }

    private static void addMember() {
        String name = readNonEmpty("Member name: ");
        if (findMember(name) != null) {
            System.out.println("That member is already in the group.");
            return;
        }
        members.add(name);
        System.out.println(name + " was added.");
    }

    private static void addExpense() {
        if (members.size() < 2) {
            System.out.println("Add at least two members before adding an expense.");
            return;
        }

        String description = readNonEmpty("What was the expense for? ");
        long total = readMoney("Total amount: ");
        printMembers();
        String paidBy = chooseMember("Who paid? ");
        int splitType = readInt("Split equally (1) or enter custom amounts (2)? ");

        Map<String, Long> shares = splitType == 2
                ? makeCustomShares(total)
                : makeEqualShares(total);

        if (shares == null) {
            return;
        }
        expenses.add(new Expense(description, total, paidBy, shares));
        System.out.println("Expense added successfully.");
    }

    private static Map<String, Long> makeEqualShares(long total) {
        Map<String, Long> shares = new LinkedHashMap<>();
        long baseShare = total / members.size();
        long remainingCents = total % members.size();

        for (int i = 0; i < members.size(); i++) {
            // The first few people receive one extra cent when division is uneven.
            shares.put(members.get(i), baseShare + (i < remainingCents ? 1 : 0));
        }
        return shares;
    }

    private static Map<String, Long> makeCustomShares(long total) {
        System.out.println("Enter each member's share. The amounts must sum to the total.");
        Map<String, Long> shares = new LinkedHashMap<>();
        long sum = 0L;
        for (String m : members) {
            long amt = readMoney("Amount for " + m + ": ");
            shares.put(m, amt);
            sum += amt;
        }
        if (sum != total) {
            System.out.println("The shares do not add up to the total (" + formatMoney(sum) + " vs " + formatMoney(total) + "). Expense not added.");
            return null;
        }
        return shares;
    }

    private static void showExpenses() {
        if (expenses.isEmpty()) {
            System.out.println("No expenses recorded.");
            return;
        }
        System.out.println("\nExpenses:");
        for (Expense e : expenses) {
            System.out.println("- " + e.description + ": " + formatMoney(e.total) + " paid by " + e.paidBy);
            for (Map.Entry<String, Long> entry : e.shares.entrySet()) {
                System.out.println("    " + entry.getKey() + ": " + formatMoney(entry.getValue()));
            }
        }
    }

    private static void showBalances() {
        Map<String, Long> balances = computeBalances();
        System.out.println("\nBalances (positive = should receive, negative = owes):");
        for (String m : members) {
            System.out.println(m + ": " + formatMoney(balances.getOrDefault(m, 0L)));
        }
    }

    private static void showSettlements() {
        Map<String, Long> balances = computeBalances();
        List<Participant> creditors = new ArrayList<>();
        List<Participant> debtors = new ArrayList<>();
        for (Map.Entry<String, Long> e : balances.entrySet()) {
            long v = e.getValue();
            if (v > 0) creditors.add(new Participant(e.getKey(), v));
            else if (v < 0) debtors.add(new Participant(e.getKey(), -v)); // store positive owed amount for debtors
        }

        creditors.sort(Comparator.comparingLong(p -> -p.amount));
        debtors.sort(Comparator.comparingLong(p -> -p.amount));

        System.out.println("\nSettlement plan:");
        int i = 0, j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            Participant d = debtors.get(i);
            Participant c = creditors.get(j);
            long pay = Math.min(d.amount, c.amount);
            System.out.println(d.name + " pays " + c.name + " " + formatMoney(pay));
            d.amount -= pay;
            c.amount -= pay;
            if (d.amount == 0) i++;
            if (c.amount == 0) j++;
        }
        if (i == 0 && j == 0) {
            System.out.println("Nothing to settle. Everyone is even.");
        }
    }

    private static Map<String, Long> computeBalances() {
        Map<String, Long> balances = new LinkedHashMap<>();
        for (String m : members) balances.put(m, 0L);
        for (Expense e : expenses) {
            // payer paid the total
            balances.put(e.paidBy, balances.getOrDefault(e.paidBy, 0L) + e.total);
            // each person's share is deducted from their balance
            for (Map.Entry<String, Long> s : e.shares.entrySet()) {
                balances.put(s.getKey(), balances.getOrDefault(s.getKey(), 0L) - s.getValue());
            }
        }
        return balances;
    }

    private static String formatMoney(long cents) {
        BigDecimal bd = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return bd.toPlainString();
    }

    private static String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = INPUT.nextLine().trim();
            if (!line.isEmpty()) return line;
            System.out.println("Value cannot be empty.");
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = INPUT.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static long readMoney(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = INPUT.nextLine().trim();
            try {
                BigDecimal bd = new BigDecimal(line).setScale(2, RoundingMode.HALF_UP);
                bd = bd.multiply(BigDecimal.valueOf(100));
                return bd.longValueExact();
            } catch (Exception ex) {
                System.out.println("Enter a valid monetary amount like 12.34");
            }
        }
    }

    private static void printMembers() {
        System.out.println("Members:");
        for (int i = 0; i < members.size(); i++) {
            System.out.println((i + 1) + ". " + members.get(i));
        }
    }

    private static String chooseMember(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = INPUT.nextLine().trim();
            // try numeric selection
            try {
                int idx = Integer.parseInt(line);
                if (idx >= 1 && idx <= members.size()) return members.get(idx - 1);
            } catch (NumberFormatException ignored) {}
            String found = findMember(line);
            if (found != null) return found;
            System.out.println("Unknown member. Enter the member's number or exact name.");
        }
    }

    private static String findMember(String name) {
        for (String m : members) {
            if (m.equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    private static class Expense {
        final String description;
        final long total; // in cents
        final String paidBy;
        final Map<String, Long> shares; // per-member cents

        Expense(String description, long total, String paidBy, Map<String, Long> shares) {
            this.description = description;
            this.total = total;
            this.paidBy = paidBy;
            this.shares = new LinkedHashMap<>(shares);
        }
    }

    private static class Participant {
        String name;
        long amount; // positive amount: creditor receives amount, debtor.amount holds positive owed amount

        Participant(String name, long amount) {
            this.name = name;
            this.amount = amount;
        }
    }
}
