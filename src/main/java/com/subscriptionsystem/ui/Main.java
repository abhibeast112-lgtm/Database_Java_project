package com.subscriptionsystem.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.subscriptionsystem.database.DBConnection;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.List;

public class Main {
    private static final Color DARK_BG = Color.decode("#08090D");
    private static final Color DARK_CARD = Color.decode("#171A24");
    private static final Color DARK_INPUT = Color.decode("#202535");
    private static final Color DARK_BORDER = Color.decode("#343B52");
    private static final Color COBALT = Color.decode("#2547C8");
    private static final Color LIGHT_BG = Color.decode("#F8F0E5");
    private static final Color LIGHT_CARD = Color.decode("#FFF9F3");
    private static final Color LIGHT_INPUT = Color.decode("#FFFFFF");
    private static final Color LIGHT_BORDER = Color.decode("#DFC8CE");
    private static final Color BURGUNDY = Color.decode("#6E2038");
    private static final Color PINK = Color.decode("#D8A0AE");

    private static final DateTimeFormatter ISO_DATE_FMT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd")
                    .withResolverStyle(ResolverStyle.STRICT);

    private static boolean dark = true;
    private static JFrame frame;
    private static JPanel root, sidebar, content, header;
    private static JLabel pageTitle, pageSubtitle, statusLabel;
    private static JButton themeButton;
    private static final CardLayout pageLayout = new CardLayout();
    private static final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private static final Map<String, JLabel> counts = new LinkedHashMap<>();
    private static final List<CrudPanel> crudPanels = new ArrayList<>();

    private static final LinkedHashMap<String, TableSpec> TABLES =
            new LinkedHashMap<>();

    static {
        TABLES.put("Customers", new TableSpec(
                "customers", "customer_id", "name", "email", "phone"));
        TABLES.put("Plans", new TableSpec(
                "plans", "plan_id", "plan_name", "price", "duration_days"));
        TABLES.put("Subscriptions", new TableSpec(
                "subscriptions", "subscription_id",
                "customer_id", "plan_id", "start_date", "end_date", "status"));
        TABLES.put("Payments", new TableSpec(
                "payments", "payment_id",
                "subscription_id", "amount", "payment_date", "status"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applyLookAndFeel();

            frame = new JFrame("Subscript | Subscription Studio");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(1000, 700));
            frame.setSize(1440, 900);
            frame.setLocationRelativeTo(null);

            root = new JPanel(new BorderLayout());
            sidebar = new JPanel();
            sidebar.setPreferredSize(new Dimension(350, 0));
            sidebar.setBorder(new EmptyBorder(28, 18, 22, 18));
            sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

            content = new JPanel(pageLayout);
            content.setBorder(new EmptyBorder(24, 28, 12, 28));

            buildSidebar();
            buildPages();

            JPanel right = new JPanel(new BorderLayout());
            right.add(buildHeader(), BorderLayout.NORTH);
            right.add(content, BorderLayout.CENTER);

            statusLabel = new JLabel("●  Ready");
            statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            statusLabel.setBorder(new EmptyBorder(10, 4, 4, 4));
            right.add(statusLabel, BorderLayout.SOUTH);

            root.add(sidebar, BorderLayout.WEST);
            root.add(right, BorderLayout.CENTER);
            frame.setContentPane(root);

            showPage("Overview");
            applyTheme();
            frame.setVisible(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

            refreshAll();
        });
    }

    private static void buildSidebar() {
        JLabel brand = new JLabel("SUBSCRIPT");
        brand.setFont(new Font("SansSerif", Font.BOLD, 25));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandSub = new JLabel("SUBSCRIPTION STUDIO");
        brandSub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        brandSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(brandSub);
        sidebar.add(Box.createVerticalStrut(42));

        JLabel menu = new JLabel("WORKSPACE");
        menu.setFont(new Font("SansSerif", Font.BOLD, 11));
        menu.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(menu);
        sidebar.add(Box.createVerticalStrut(12));

        addNav("Overview", "◈");
        addNav("Customers", "◎");
        addNav("Plans", "◇");
        addNav("Subscriptions", "▤");
        addNav("Payments", "◉");

        sidebar.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("<html><b>SUBSCRIPT</b><br>Database Management</html>");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 12));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(footer);
    }

    private static void addNav(String name, String icon) {
        JButton button = new JButton(icon + "     " + name);
        button.setFont(new Font("SansSerif", Font.PLAIN, 18));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        button.setPreferredSize(new Dimension(250, 54));
        button.setBorder(new EmptyBorder(0, 14, 0, 10));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> showPage(name));
        navButtons.put(name, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(7));
    }

    private static JPanel buildHeader() {
        header = new JPanel(new BorderLayout(16, 8));
        header.setBorder(new EmptyBorder(25, 30, 20, 30));

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        pageTitle = new JLabel("Overview");
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 36));

        pageSubtitle = new JLabel("Your subscription business at a glance.");
        pageSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 17));

        titles.add(pageTitle);
        titles.add(Box.createVerticalStrut(6));
        titles.add(pageSubtitle);

        themeButton = new JButton("☀  Light mode");
        themeButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        themeButton.setPreferredSize(new Dimension(155, 44));
        themeButton.addActionListener(e -> {
            dark = !dark;
            applyLookAndFeel();
            SwingUtilities.updateComponentTreeUI(frame);
            applyTheme();
        });

        header.add(titles, BorderLayout.WEST);
        header.add(themeButton, BorderLayout.EAST);
        return header;
    }

    private static void buildPages() {
        content.add(buildDashboard(), "Overview");

        for (Map.Entry<String, TableSpec> entry : TABLES.entrySet()) {
            CrudPanel panel = new CrudPanel(entry.getKey(), entry.getValue());
            crudPanels.add(panel);
            content.add(panel, entry.getKey());
        }
    }

    private static JPanel buildDashboard() {
        JPanel page = new JPanel(new BorderLayout(0, 28));
        page.setBorder(new EmptyBorder(4, 0, 20, 0));

        JPanel intro = new JPanel();
        intro.setLayout(new BoxLayout(intro, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("Welcome back.");
        heading.setFont(new Font("SansSerif", Font.BOLD, 27));

        JLabel description = new JLabel(
                "Everything you need to manage your subscriptions, in one place.");
        description.setFont(new Font("SansSerif", Font.PLAIN, 16));

        intro.add(heading);
        intro.add(Box.createVerticalStrut(8));
        intro.add(description);

        JPanel cards = new JPanel(new GridLayout(1, 4, 18, 0));
        cards.setPreferredSize(new Dimension(800, 155));
        addCountCard(cards, "CUSTOMERS", "customers");
        addCountCard(cards, "PLANS", "plans");
        addCountCard(cards, "SUBSCRIPTIONS", "subscriptions");
        addCountCard(cards, "PAYMENTS", "payments");

        JPanel lower = new JPanel(new BorderLayout(0, 12));
        JLabel lowerTitle = new JLabel("Getting started");
        lowerTitle.setFont(new Font("SansSerif", Font.BOLD, 20));

        JLabel help = new JLabel(
                "<html><div style='line-height:1.6'>"
                + "Use the sidebar to manage your records.<br>"
                + "Select a row to edit or delete it, or fill in the form to insert a new record.<br>"
                + "Every successful change is saved to your MySQL database."
                + "</div></html>");
        help.setFont(new Font("SansSerif", Font.PLAIN, 15));
        help.setBorder(new EmptyBorder(18, 20, 18, 20));

        lower.add(lowerTitle, BorderLayout.NORTH);
        lower.add(help, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 28));
        center.add(intro, BorderLayout.NORTH);
        center.add(cards, BorderLayout.CENTER);

        page.add(center, BorderLayout.NORTH);
        page.add(lower, BorderLayout.CENTER);
        return page;
    }

    private static void addCountCard(JPanel parent, String title, String key) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));

        JLabel number = new JLabel("—");
        number.setFont(new Font("SansSerif", Font.BOLD, 36));

        JLabel caption = new JLabel("Total records");
        caption.setFont(new Font("SansSerif", Font.PLAIN, 13));

        counts.put(key, number);
        card.add(label);
        card.add(Box.createVerticalStrut(16));
        card.add(number);
        card.add(Box.createVerticalStrut(5));
        card.add(caption);
        parent.add(card);
    }

    private static void showPage(String name) {
        pageLayout.show(content, name);
        pageTitle.setText(name.equals("Overview") ? "Overview" : name);
        pageSubtitle.setText(switch (name) {
            case "Overview" -> "Your subscription business at a glance.";
            case "Customers" -> "Manage your customer records.";
            case "Plans" -> "Create and maintain subscription plans.";
            case "Subscriptions" -> "Manage customer subscriptions.";
            case "Payments" -> "Review and manage payment records.";
            default -> "";
        });

        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean selected = entry.getKey().equals(name);
            entry.getValue().setBackground(selected ? accent() : surface());
            entry.getValue().setForeground(selected
                    ? Color.WHITE : textColor());
        }

        if (!name.equals("Overview")) {
            for (CrudPanel panel : crudPanels) {
                if (panel.label.equals(name)) {
                    panel.loadData();
                    break;
                }
            }
        }
    }

    private static void applyLookAndFeel() {
        try {
            if (dark) FlatDarkLaf.setup();
            else FlatLightLaf.setup();

            UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 15));
            UIManager.put("Component.arc", 12);
            UIManager.put("Button.arc", 12);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("Component.focusColor", accent());
            UIManager.put("Table.rowHeight", 46);
            UIManager.put("ComboBox.background", inputBg());
            UIManager.put("ComboBox.foreground", textColor());
            UIManager.put("ComboBox.buttonBackground", inputBg());
            UIManager.put("ComboBox.selectionBackground", accent());
            UIManager.put("ComboBox.selectionForeground", Color.WHITE);
            UIManager.put("ComboBox.popupBackground", inputBg());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Color background() {
        return dark ? DARK_BG : LIGHT_BG;
    }

    private static Color surface() {
        return dark ? DARK_CARD : LIGHT_CARD;
    }

    private static Color inputBg() {
        return dark ? DARK_INPUT : LIGHT_INPUT;
    }

    private static Color borderColor() {
        return dark ? DARK_BORDER : LIGHT_BORDER;
    }

    private static Color accent() {
        return dark ? COBALT : BURGUNDY;
    }

    private static Color textColor() {
        return dark ? Color.WHITE : Color.decode("#351A25");
    }

    private static Color mutedColor() {
        return dark ? Color.decode("#A9AFBF") : Color.decode("#765D63");
    }

    private static void applyTheme() {
        if (frame == null) return;

        themeButton.setText(dark ? "☀  Light mode" : "☾  Dark mode");
        themeButton.setBackground(surface());
        themeButton.setForeground(textColor());
        themeButton.setBorderPainted(false);
        themeButton.setFocusPainted(false);

        root.setBackground(background());
        sidebar.setBackground(surface());
        content.setBackground(background());
        header.setBackground(background());
        if (statusLabel != null) {
            statusLabel.setForeground(mutedColor());
            if (statusLabel.getParent() != null) {
                statusLabel.getParent().setBackground(background());
            }
        }
        if (pageTitle != null) {
            pageTitle.setForeground(textColor());
            if (pageTitle.getParent() != null) {
                pageTitle.getParent().setBackground(background());
            }
        }
        if (pageSubtitle != null) pageSubtitle.setForeground(mutedColor());

        for (Component component : sidebar.getComponents()) {
            if (component instanceof JLabel label) {
                label.setForeground(label.getFont().getSize() >= 20 ? textColor() : mutedColor());
            }
        }

        for (JButton button : navButtons.values()) {
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
        }

        for (Component comp : content.getComponents()) {
            if (comp instanceof CrudPanel panel) {
                panel.applyTheme();
            } else if (comp instanceof JPanel dash) {
                themeDashboard(dash);
            }
        }

        showPage(pageTitle.getText().equals("Overview")
                ? "Overview" : pageTitle.getText());

        frame.getContentPane().setBackground(background());
        frame.repaint();
    }

    private static void themeDashboard(Container container) {
        boolean isCard = container.getLayout() instanceof BoxLayout
                && ((JComponent) container).getBorder() instanceof EmptyBorder eb
                && eb.getBorderInsets(container).top == 20;
        container.setBackground(isCard ? surface() : background());
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel lbl) {
                boolean isMuted = lbl.getFont().getSize() <= 16 && !lbl.getFont().isBold();
                lbl.setForeground(isMuted ? mutedColor() : textColor());
                if (lbl.getBorder() instanceof EmptyBorder) {
                    lbl.setOpaque(true);
                    lbl.setBackground(surface());
                }
            } else if (c instanceof Container child) {
                themeDashboard(child);
            }
        }
    }

    private static void refreshAll() {
        for (CrudPanel panel : crudPanels) panel.loadData();
        refreshCounts();
    }

    private static void refreshCounts() {
        try (Connection connection = DBConnection.getConnection()) {
            for (Map.Entry<String, JLabel> entry : counts.entrySet()) {
                try (Statement statement = connection.createStatement();
                     ResultSet rs = statement.executeQuery(
                             "SELECT COUNT(*) FROM " + entry.getKey())) {
                    if (rs.next()) entry.getValue().setText(rs.getString(1));
                }
            }
            setStatus("Connected to MySQL  ·  All changes are saved automatically");
        } catch (SQLException ex) {
            setStatus("Database unavailable  ·  Check DBConnection.java");
        }
    }

    private static void setStatus(String message) {
        if (statusLabel != null) statusLabel.setText("●  " + message);
    }

    private static void showError(String title, Exception ex) {
        JOptionPane.showMessageDialog(frame, ex.getMessage(),
                title, JOptionPane.ERROR_MESSAGE);
        setStatus("Operation failed");
    }

    private static String pretty(String value) {
        return switch (value) {
            case "customer_id" -> "Customer";
            case "plan_id" -> "Plan";
            case "subscription_id" -> "Subscription";
            case "duration_days" -> "Duration (Days)";
            case "start_date" -> "Start Date (YYYY-MM-DD)";
            case "end_date" -> "End Date (YYYY-MM-DD)";
            case "payment_date" -> "Payment Date (YYYY-MM-DD)";
            default -> {
                StringBuilder out = new StringBuilder();
                for (String word : value.replace('_', ' ').split(" ")) {
                    if (!word.isEmpty()) {
                        if (!out.isEmpty()) out.append(' ');
                        out.append(Character.toUpperCase(word.charAt(0)))
                           .append(word.substring(1));
                    }
                }
                yield out.toString();
            }
        };
    }

    private static String columnTitle(String value) {
        StringBuilder out = new StringBuilder();
        for (String word : value.replace('_', ' ').split(" ")) {
            if (!word.isEmpty()) {
                if (!out.isEmpty()) out.append(' ');
                out.append(Character.toUpperCase(word.charAt(0)))
                   .append(word.substring(1));
            }
        }
        return out.toString();
    }

    private static class ComboItem {
        final Integer id;
        final String display;

        ComboItem(Integer id, String display) {
            this.id = id;
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    private static class DateInputPanel extends JPanel {
        final JTextField textField = new JTextField();
        final JButton pickButton = new JButton("📅");

        DateInputPanel(String placeholder) {
            super(new BorderLayout(8, 0));
            setOpaque(false);
            textField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            textField.setPreferredSize(new Dimension(200, 44));
            textField.putClientProperty("JTextField.placeholderText", placeholder);

            pickButton.setFont(new Font("SansSerif", Font.PLAIN, 16));
            pickButton.setPreferredSize(new Dimension(48, 44));
            pickButton.setFocusPainted(false);
            pickButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pickButton.setToolTipText("Open calendar date picker");
            pickButton.addActionListener(e -> openCalendarDialog());

            add(textField, BorderLayout.CENTER);
            add(pickButton, BorderLayout.EAST);
        }

        private void openCalendarDialog() {
            LocalDate initial = LocalDate.now();
            String current = textField.getText().trim();
            if (!current.isEmpty()) {
                try {
                    initial = LocalDate.parse(current, ISO_DATE_FMT);
                } catch (DateTimeParseException ignored) {}
            }

            JDialog dialog = new JDialog(frame, "Select Date", true);
            dialog.setSize(360, 350);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(8, 8));

            JPanel container = new JPanel(new BorderLayout(8, 10));
            container.setBorder(new EmptyBorder(14, 14, 14, 14));
            container.setBackground(surface());

            YearMonth[] currentYm = {YearMonth.from(initial)};
            LocalDate selectedDate = initial;

            JPanel nav = new JPanel(new BorderLayout(6, 0));
            nav.setOpaque(false);
            JButton prevMonth = new JButton("◀");
            JButton nextMonth = new JButton("▶");
            JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
            monthLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            monthLabel.setForeground(textColor());

            for (JButton b : List.of(prevMonth, nextMonth)) {
                b.setFont(new Font("SansSerif", Font.BOLD, 13));
                b.setBackground(inputBg());
                b.setForeground(textColor());
                b.setFocusPainted(false);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            nav.add(prevMonth, BorderLayout.WEST);
            nav.add(monthLabel, BorderLayout.CENTER);
            nav.add(nextMonth, BorderLayout.EAST);

            JPanel grid = new JPanel(new GridLayout(0, 7, 4, 4));
            grid.setOpaque(false);

            Runnable renderCalendar = () -> {
                grid.removeAll();
                YearMonth ym = currentYm[0];
                monthLabel.setText(ym.getMonth().name().charAt(0)
                        + ym.getMonth().name().substring(1).toLowerCase() + " " + ym.getYear());
                String[] days = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
                for (String d : days) {
                    JLabel dl = new JLabel(d, SwingConstants.CENTER);
                    dl.setFont(new Font("SansSerif", Font.BOLD, 12));
                    dl.setForeground(mutedColor());
                    grid.add(dl);
                }
                int firstDow = ym.atDay(1).getDayOfWeek().getValue(); // 1=Mon .. 7=Sun
                for (int i = 1; i < firstDow; i++) {
                    grid.add(new JLabel(""));
                }
                for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                    LocalDate date = ym.atDay(day);
                    JButton dayBtn = new JButton(String.valueOf(day));
                    dayBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
                    dayBtn.setMargin(new Insets(2, 2, 2, 2));
                    dayBtn.setFocusPainted(false);
                    dayBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    boolean isSel = date.equals(selectedDate);
                    dayBtn.setBackground(isSel ? accent() : inputBg());
                    dayBtn.setForeground(isSel ? Color.WHITE : textColor());
                    dayBtn.addActionListener(ev -> {
                        textField.setText(date.format(ISO_DATE_FMT));
                        dialog.dispose();
                    });
                    grid.add(dayBtn);
                }
                grid.revalidate();
                grid.repaint();
            };

            prevMonth.addActionListener(ev -> {
                currentYm[0] = currentYm[0].minusMonths(1);
                renderCalendar.run();
            });
            nextMonth.addActionListener(ev -> {
                currentYm[0] = currentYm[0].plusMonths(1);
                renderCalendar.run();
            });

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            bottom.setOpaque(false);
            JButton todayBtn = new JButton("Today");
            todayBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
            todayBtn.setBackground(accent());
            todayBtn.setForeground(Color.WHITE);
            todayBtn.setFocusPainted(false);
            todayBtn.addActionListener(ev -> {
                textField.setText(LocalDate.now().format(ISO_DATE_FMT));
                dialog.dispose();
            });
            bottom.add(todayBtn);

            renderCalendar.run();
            container.add(nav, BorderLayout.NORTH);
            container.add(grid, BorderLayout.CENTER);
            container.add(bottom, BorderLayout.SOUTH);
            dialog.setContentPane(container);
            dialog.setVisible(true);
        }

        String getText() {
            return textField.getText().trim();
        }

        void setText(String text) {
            textField.setText(text);
        }

        void applyTheme() {
            textField.setBackground(inputBg());
            textField.setForeground(textColor());
            textField.setCaretColor(textColor());
            textField.setBorder(new CompoundBorder(
                    new LineBorder(borderColor(), 1, true),
                    new EmptyBorder(6, 12, 6, 12)));
            pickButton.setBackground(inputBg());
            pickButton.setForeground(textColor());
            pickButton.setBorder(new LineBorder(borderColor(), 1, true));
        }
    }

    private static class TableSpec {
        final String table, key;
        final List<String> fields;

        TableSpec(String table, String key, String... fields) {
            this.table = table;
            this.key = key;
            this.fields = List.of(fields);
        }
    }

    private static class CrudPanel extends JPanel {
        final String label;
        final TableSpec spec;
        final JTable table = new JTable();
        final DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        final Map<String, JComponent> inputs = new LinkedHashMap<>();
        final List<JButton> actionButtons = new ArrayList<>();
        final JScrollPane scroll;
        final JPanel editor;
        final JPanel formContent;
        final JPanel fieldsPanel;
        final JPanel actionsPanel;
        final JPanel topPanel;
        Object selectedId;

        CrudPanel(String label, TableSpec spec) {
            this.label = label;
            this.spec = spec;
            setLayout(new BorderLayout(0, 18));
            setBorder(new EmptyBorder(4, 0, 4, 0));

            table.setModel(model);
            table.setRowHeight(46);
            table.setFont(new Font("SansSerif", Font.PLAIN, 16));
            table.getTableHeader().setFont(
                    new Font("SansSerif", Font.BOLD, 15));
            table.getTableHeader().setPreferredSize(new Dimension(0, 44));
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.setFillsViewportHeight(true);
            table.setShowHorizontalLines(true);
            table.setShowVerticalLines(false);
            table.setIntercellSpacing(new Dimension(0, 1));

            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) fillFromSelection();
            });

            topPanel = new JPanel(new BorderLayout(12, 12));
            JLabel section = new JLabel("All " + label.toLowerCase());
            section.setFont(new Font("SansSerif", Font.BOLD, 20));

            JButton refresh = new JButton("↻  Refresh");
            refresh.setFont(new Font("SansSerif", Font.BOLD, 14));
            refresh.setPreferredSize(new Dimension(130, 40));
            refresh.addActionListener(e -> loadData());
            actionButtons.add(refresh);

            topPanel.add(section, BorderLayout.WEST);
            topPanel.add(refresh, BorderLayout.EAST);

            scroll = new JScrollPane(table);
            scroll.setBorder(new LineBorder(borderColor(), 1, true));

            editor = new JPanel(new BorderLayout(0, 18));
            editor.setBorder(new CompoundBorder(
                    new LineBorder(borderColor(), 1, true),
                    new EmptyBorder(22, 24, 22, 24)));

            JLabel formTitle = new JLabel("Record details");
            formTitle.setFont(new Font("SansSerif", Font.BOLD, 22));

            fieldsPanel = new JPanel(new GridLayout(0, 2, 24, 16));
            for (String field : spec.fields) {
                JPanel cell = new JPanel(new BorderLayout(0, 7));
                cell.setOpaque(false);

                JLabel name = new JLabel(pretty(field));
                name.setFont(new Font("SansSerif", Font.BOLD, 14));

                JComponent control = createControl(field);
                inputs.put(field, control);

                cell.add(name, BorderLayout.NORTH);
                cell.add(control, BorderLayout.CENTER);
                fieldsPanel.add(cell);
            }
            if (spec.fields.size() % 2 != 0) {
                JPanel filler = new JPanel();
                filler.setOpaque(false);
                fieldsPanel.add(filler);
            }

            actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            actionsPanel.setOpaque(false);
            JButton insert = new JButton("＋  Insert record");
            JButton update = new JButton("Update selected");
            JButton delete = new JButton("Delete selected");
            JButton clear = new JButton("Clear");

            insert.addActionListener(e -> insertRecord());
            update.addActionListener(e -> updateRecord());
            delete.addActionListener(e -> deleteRecord());
            clear.addActionListener(e -> clearForm());

            styleAction(insert, true);
            styleAction(update, false);
            styleAction(delete, false);
            styleAction(clear, false);

            actionsPanel.add(insert);
            actionsPanel.add(update);
            actionsPanel.add(delete);
            actionsPanel.add(clear);

            actionButtons.add(insert);
            actionButtons.add(update);
            actionButtons.add(delete);
            actionButtons.add(clear);

            formContent = new JPanel(new BorderLayout(0, 16));
            formContent.setOpaque(false);
            formContent.add(formTitle, BorderLayout.NORTH);
            formContent.add(fieldsPanel, BorderLayout.CENTER);
            editor.add(formContent, BorderLayout.CENTER);
            editor.add(actionsPanel, BorderLayout.SOUTH);

            add(topPanel, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            add(editor, BorderLayout.SOUTH);
        }

        private JComponent createControl(String field) {
            return switch (field) {
                case "customer_id", "plan_id", "subscription_id" -> {
                    JComboBox<ComboItem> combo = new JComboBox<>();
                    styleComboBox(combo);
                    yield combo;
                }
                case "status" -> {
                    String[] opts = spec.table.equals("subscriptions")
                            ? new String[]{"-- Select Status --", "ACTIVE", "CANCELLED", "EXPIRED"}
                            : new String[]{"-- Select Status --", "PENDING", "COMPLETED", "FAILED"};
                    JComboBox<String> combo = new JComboBox<>(opts);
                    styleComboBox(combo);
                    yield combo;
                }
                case "start_date", "end_date", "payment_date" ->
                        new DateInputPanel("YYYY-MM-DD (e.g. " + LocalDate.now() + ")");
                default -> {
                    JTextField input = new JTextField();
                    input.setFont(new Font("SansSerif", Font.PLAIN, 16));
                    input.setPreferredSize(new Dimension(240, 44));
                    input.putClientProperty("JTextField.placeholderText", switch (field) {
                        case "name" -> "Enter customer full name";
                        case "email" -> "Enter email address (e.g. user@example.com)";
                        case "phone" -> "Enter phone number";
                        case "plan_name" -> "Enter plan name (e.g. Pro Monthly)";
                        case "price" -> "Enter price (e.g. 29.99)";
                        case "duration_days" -> "Enter duration in days (e.g. 30)";
                        case "amount" -> "Enter payment amount (e.g. 29.99)";
                        default -> pretty(field);
                    });
                    yield input;
                }
            };
        }

        private <T> void styleComboBox(JComboBox<T> combo) {
            combo.setFont(new Font("SansSerif", Font.PLAIN, 16));
            combo.setPreferredSize(new Dimension(240, 44));
            combo.setRenderer(new BasicComboBoxRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setFont(new Font("SansSerif", Font.PLAIN, 15));
                    setBorder(new EmptyBorder(8, 12, 8, 12));
                    if (isSelected) {
                        setBackground(accent());
                        setForeground(Color.WHITE);
                    } else {
                        setBackground(inputBg());
                        boolean isPlaceholder = (value instanceof ComboItem ci && ci.id == null)
                                || (value instanceof String s && s.startsWith("--"));
                        setForeground(isPlaceholder ? mutedColor() : textColor());
                    }
                    return this;
                }
            });
        }

        private void refreshForeignKeyDropdowns() {
            if (!spec.table.equals("subscriptions") && !spec.table.equals("payments")) {
                return;
            }
            try (Connection c = DBConnection.getConnection()) {
                if (spec.table.equals("subscriptions")) {
                    @SuppressWarnings("unchecked")
                    JComboBox<ComboItem> custCombo = (JComboBox<ComboItem>) inputs.get("customer_id");
                    Integer prevCust = getSelectedComboId(custCombo);
                    custCombo.removeAllItems();
                    custCombo.addItem(new ComboItem(null, "-- Select Customer --"));
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT customer_id, name, email FROM customers ORDER BY name ASC");
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("customer_id");
                            String name = rs.getString("name");
                            String email = rs.getString("email");
                            custCombo.addItem(new ComboItem(id, name + " (" + email + ") [ID: " + id + "]"));
                        }
                    }
                    selectComboById(custCombo, prevCust);

                    @SuppressWarnings("unchecked")
                    JComboBox<ComboItem> planCombo = (JComboBox<ComboItem>) inputs.get("plan_id");
                    Integer prevPlan = getSelectedComboId(planCombo);
                    planCombo.removeAllItems();
                    planCombo.addItem(new ComboItem(null, "-- Select Plan --"));
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT plan_id, plan_name, price, duration_days FROM plans ORDER BY plan_name ASC");
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("plan_id");
                            String pName = rs.getString("plan_name");
                            BigDecimal price = rs.getBigDecimal("price");
                            int days = rs.getInt("duration_days");
                            planCombo.addItem(new ComboItem(id,
                                    pName + " - $" + price + " / " + days + "d [ID: " + id + "]"));
                        }
                    }
                    selectComboById(planCombo, prevPlan);
                } else if (spec.table.equals("payments")) {
                    @SuppressWarnings("unchecked")
                    JComboBox<ComboItem> subCombo = (JComboBox<ComboItem>) inputs.get("subscription_id");
                    Integer prevSub = getSelectedComboId(subCombo);
                    subCombo.removeAllItems();
                    subCombo.addItem(new ComboItem(null, "-- Select Subscription --"));
                    String sql = "SELECT s.subscription_id, c.name AS customer_name, p.plan_name, s.status "
                            + "FROM subscriptions s "
                            + "JOIN customers c ON s.customer_id = c.customer_id "
                            + "JOIN plans p ON s.plan_id = p.plan_id "
                            + "ORDER BY s.subscription_id DESC";
                    try (PreparedStatement ps = c.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("subscription_id");
                            String cName = rs.getString("customer_name");
                            String pName = rs.getString("plan_name");
                            String st = rs.getString("status");
                            subCombo.addItem(new ComboItem(id,
                                    "#" + id + " · " + cName + " — " + pName + " (" + st + ")"));
                        }
                    }
                    selectComboById(subCombo, prevSub);
                }
            } catch (SQLException ignored) {
                // Handled during main loadData query
            }
        }

        private Integer getSelectedComboId(JComboBox<ComboItem> combo) {
            if (combo == null) return null;
            Object item = combo.getSelectedItem();
            return (item instanceof ComboItem ci) ? ci.id : null;
        }

        private void selectComboById(JComboBox<ComboItem> combo, Integer targetId) {
            if (combo == null) return;
            if (targetId == null) {
                if (combo.getItemCount() > 0) combo.setSelectedIndex(0);
                return;
            }
            for (int i = 0; i < combo.getItemCount(); i++) {
                ComboItem item = combo.getItemAt(i);
                if (item != null && targetId.equals(item.id)) {
                    combo.setSelectedIndex(i);
                    return;
                }
            }
            if (combo.getItemCount() > 0) combo.setSelectedIndex(0);
        }

        private void styleAction(JButton button, boolean primary) {
            button.putClientProperty("primaryAction", primary);
            button.setFont(new Font("SansSerif", Font.BOLD, 14));
            button.setPreferredSize(new Dimension(primary ? 175 : 160, 44));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setFocusPainted(false);
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    button.setBackground(dark ? Color.decode("#4169E1") : PINK);
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    styleActionColors(button, primary);
                }
            });
            styleActionColors(button, primary);
        }

        private void styleActionColors(JButton button, boolean primary) {
            button.setOpaque(true);
            button.setBorderPainted(!primary);
            if (!primary) {
                button.setBorder(new LineBorder(borderColor(), 1, true));
            }
            button.setBackground(primary ? accent() : inputBg());
            button.setForeground(primary ? Color.WHITE : textColor());
        }

        void applyTheme() {
            setBackground(background());
            topPanel.setBackground(background());
            for (Component c : topPanel.getComponents()) {
                if (c instanceof JLabel lbl) lbl.setForeground(textColor());
            }

            table.setBackground(surface());
            table.setForeground(textColor());
            table.setSelectionBackground(accent());
            table.setSelectionForeground(Color.WHITE);
            table.setGridColor(dark
                    ? Color.decode("#303441") : Color.decode("#E5D8C9"));
            table.getTableHeader().setBackground(surface());
            table.getTableHeader().setForeground(textColor());
            scroll.setBackground(surface());
            scroll.getViewport().setBackground(surface());
            scroll.setBorder(new LineBorder(borderColor(), 1, true));

            editor.setBackground(surface());
            editor.setBorder(new CompoundBorder(
                    new LineBorder(borderColor(), 1, true),
                    new EmptyBorder(22, 24, 22, 24)));

            themeFormComponents(formContent);

            for (JButton button : actionButtons) {
                if (button.getClientProperty("primaryAction") != null) {
                    styleActionColors(button,
                            Boolean.TRUE.equals(button.getClientProperty("primaryAction")));
                } else {
                    button.setBackground(surface());
                    button.setForeground(textColor());
                    button.setBorder(new LineBorder(borderColor(), 1, true));
                    button.setOpaque(true);
                }
            }
            repaint();
        }

        private void themeFormComponents(Container container) {
            for (Component c : container.getComponents()) {
                if (c instanceof JLabel lbl) {
                    lbl.setForeground(textColor());
                } else if (c instanceof JTextField field) {
                    field.setBackground(inputBg());
                    field.setForeground(textColor());
                    field.setCaretColor(textColor());
                    field.setBorder(new CompoundBorder(
                            new LineBorder(borderColor(), 1, true),
                            new EmptyBorder(6, 12, 6, 12)));
                } else if (c instanceof JComboBox<?> combo) {
                    combo.setBackground(inputBg());
                    combo.setForeground(textColor());
                    combo.setBorder(new LineBorder(borderColor(), 1, true));
                } else if (c instanceof DateInputPanel datePanel) {
                    datePanel.applyTheme();
                } else if (c instanceof JPanel child) {
                    themeFormComponents(child);
                }
            }
        }

        void loadData() {
            refreshForeignKeyDropdowns();
            String sql = "SELECT * FROM " + spec.table
                    + " ORDER BY " + spec.key + " DESC";
            try (Connection c = DBConnection.getConnection();
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(sql)) {
                ResultSetMetaData md = rs.getMetaData();
                model.setRowCount(0);
                model.setColumnCount(0);

                for (int i = 1; i <= md.getColumnCount(); i++) {
                    model.addColumn(columnTitle(md.getColumnLabel(i)));
                }

                while (rs.next()) {
                    Object[] row = new Object[md.getColumnCount()];
                    for (int i = 1; i <= row.length; i++) row[i - 1] = rs.getObject(i);
                    model.addRow(row);
                }
                applyTheme();
                setStatus(label + " loaded  ·  " + model.getRowCount() + " records");
            } catch (SQLException ex) {
                showError("Could not load " + label, ex);
            }
        }

        private int column(String field) {
            String target = columnTitle(field);
            for (int i = 0; i < model.getColumnCount(); i++) {
                if (model.getColumnName(i).equalsIgnoreCase(target)) return i;
            }
            return -1;
        }

        private void fillFromSelection() {
            int selected = table.getSelectedRow();
            if (selected < 0) {
                selectedId = null;
                return;
            }
            int row = table.convertRowIndexToModel(selected);
            int idColumn = column(spec.key);
            if (idColumn >= 0) selectedId = model.getValueAt(row, idColumn);

            for (String field : spec.fields) {
                int col = column(field);
                if (col < 0) continue;
                Object value = model.getValueAt(row, col);
                JComponent control = inputs.get(field);

                if (control instanceof JTextField tf) {
                    tf.setText(value == null ? "" : value.toString());
                } else if (control instanceof DateInputPanel dp) {
                    dp.setText(value == null ? "" : value.toString());
                } else if (control instanceof JComboBox<?> combo) {
                    if (field.equals("status")) {
                        combo.setSelectedItem(value == null ? combo.getItemAt(0) : value.toString());
                    } else if (value != null) {
                        try {
                            int fkId = Integer.parseInt(value.toString());
                            @SuppressWarnings("unchecked")
                            JComboBox<ComboItem> fkCombo = (JComboBox<ComboItem>) combo;
                            selectComboById(fkCombo, fkId);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        private Map<String, String> collectRawValues() {
            Map<String, String> result = new LinkedHashMap<>();
            for (String field : spec.fields) {
                JComponent control = inputs.get(field);
                String val = "";
                if (control instanceof JTextField tf) {
                    val = tf.getText().trim();
                } else if (control instanceof DateInputPanel dp) {
                    val = dp.getText();
                } else if (control instanceof JComboBox<?> combo) {
                    Object sel = combo.getSelectedItem();
                    if (sel instanceof ComboItem ci) {
                        val = ci.id == null ? "" : String.valueOf(ci.id);
                    } else if (sel instanceof String s) {
                        val = s.startsWith("--") ? "" : s.trim();
                    }
                }
                result.put(field, val);
            }
            return result;
        }

        private boolean validateAndBind(Map<String, String> rawValues, List<Object> sqlParams) {
            for (String field : spec.fields) {
                String val = rawValues.getOrDefault(field, "");
                if (val.isBlank()) {
                    JOptionPane.showMessageDialog(frame,
                            "Please provide a valid value for \"" + pretty(field) + "\".",
                            "Missing Information",
                            JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            }

            LocalDate startDateVal = null;
            for (String field : spec.fields) {
                String val = rawValues.get(field);
                switch (field) {
                    case "customer_id", "plan_id", "subscription_id" -> {
                        try {
                            sqlParams.add(Integer.parseInt(val));
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(frame,
                                    "Please select a valid " + pretty(field) + " from the dropdown.",
                                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                            return false;
                        }
                    }
                    case "duration_days" -> {
                        try {
                            int days = Integer.parseInt(val);
                            if (days <= 0) {
                                JOptionPane.showMessageDialog(frame,
                                        "Duration (Days) must be a positive integer greater than 0.",
                                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                                return false;
                            }
                            sqlParams.add(days);
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(frame,
                                    "Duration (Days) must be a valid positive whole number (e.g. 30).",
                                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                            return false;
                        }
                    }
                    case "price", "amount" -> {
                        try {
                            BigDecimal dec = new BigDecimal(val);
                            if (dec.compareTo(BigDecimal.ZERO) < 0) {
                                JOptionPane.showMessageDialog(frame,
                                        pretty(field) + " cannot be negative. Please enter 0 or a positive decimal amount.",
                                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                                return false;
                            }
                            sqlParams.add(dec);
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(frame,
                                    pretty(field) + " must be a valid non-negative decimal number (e.g. 19.99).",
                                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                            return false;
                        }
                    }
                    case "start_date", "end_date", "payment_date" -> {
                        try {
                            LocalDate parsed = LocalDate.parse(val, ISO_DATE_FMT);
                            if (field.equals("start_date")) {
                                startDateVal = parsed;
                            } else if (field.equals("end_date") && startDateVal != null
                                    && parsed.isBefore(startDateVal)) {
                                JOptionPane.showMessageDialog(frame,
                                        "End Date (" + parsed + ") cannot be earlier than Start Date (" + startDateVal + ").",
                                        "Invalid Date Range", JOptionPane.WARNING_MESSAGE);
                                return false;
                            }
                            sqlParams.add(java.sql.Date.valueOf(parsed));
                        } catch (DateTimeParseException ex) {
                            JOptionPane.showMessageDialog(frame,
                                    "Invalid " + columnTitle(field) + ": \"" + val + "\".\n"
                                    + "Please enter a valid calendar date in ISO format YYYY-MM-DD (e.g. 2026-10-21).",
                                    "Invalid Date Format", JOptionPane.WARNING_MESSAGE);
                            return false;
                        }
                    }
                    default -> sqlParams.add(val);
                }
            }
            return true;
        }

        private void insertRecord() {
            Map<String, String> raw = collectRawValues();
            List<Object> params = new ArrayList<>();
            if (!validateAndBind(raw, params)) return;

            String sql = "INSERT INTO " + spec.table + " ("
                    + String.join(", ", spec.fields) + ") VALUES ("
                    + String.join(", ", Collections.nCopies(params.size(), "?")) + ")";

            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
                ps.executeUpdate();
                clearForm();
                refreshAll();
                setStatus(label + " record inserted successfully");
                JOptionPane.showMessageDialog(frame,
                        label + " record inserted successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                showError("Insert failed", ex);
            }
        }

        private void updateRecord() {
            if (selectedId == null) {
                JOptionPane.showMessageDialog(frame,
                        "Please select a row in the table to update.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Map<String, String> raw = collectRawValues();
            List<Object> params = new ArrayList<>();
            if (!validateAndBind(raw, params)) return;

            List<String> sets = new ArrayList<>();
            for (String field : spec.fields) sets.add(field + " = ?");
            String sql = "UPDATE " + spec.table + " SET "
                    + String.join(", ", sets) + " WHERE " + spec.key + " = ?";

            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
                ps.setObject(params.size() + 1, selectedId);
                ps.executeUpdate();
                clearForm();
                refreshAll();
                setStatus(label + " record updated successfully");
                JOptionPane.showMessageDialog(frame,
                        label + " record updated successfully.",
                        "Updated", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                showError("Update failed", ex);
            }
        }

        private void deleteRecord() {
            if (selectedId == null) {
                JOptionPane.showMessageDialog(frame,
                        "Please select a row in the table to delete.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int choice = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to delete this " + label.toLowerCase()
                    + " record (ID: " + selectedId + ")?\nThis action cannot be undone.",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;

            String sql = "DELETE FROM " + spec.table + " WHERE " + spec.key + " = ?";
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setObject(1, selectedId);
                ps.executeUpdate();
                clearForm();
                refreshAll();
                setStatus(label + " record deleted successfully");
                JOptionPane.showMessageDialog(frame,
                        label + " record deleted successfully.",
                        "Deleted", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                boolean fkViolation = ex.getErrorCode() == 1451
                        || (ex.getSQLState() != null && ex.getSQLState().startsWith("23"))
                        || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("foreign key"));
                if (fkViolation) {
                    String dependencyHint = switch (spec.table) {
                        case "customers" ->
                                "Cannot delete this Customer because they have one or more active or past Subscriptions.\n"
                                + "Please delete or reassign their Subscriptions first before deleting the customer.";
                        case "plans" ->
                                "Cannot delete this Plan because it is currently referenced by one or more Subscriptions.\n"
                                + "Please delete or update the associated Subscriptions first.";
                        case "subscriptions" ->
                                "Cannot delete this Subscription because it has associated Payment records.\n"
                                + "Please delete the related Payments first before deleting this subscription.";
                        default ->
                                "This record cannot be deleted because it is referenced by records in another table.";
                    };
                    showError("Cannot Delete — Foreign Key Constraint",
                            new SQLException(dependencyHint + "\n\nDatabase details: " + ex.getMessage(), ex));
                } else {
                    showError("Delete failed", ex);
                }
            }
        }

        private void clearForm() {
            selectedId = null;
            table.clearSelection();
            for (JComponent control : inputs.values()) {
                if (control instanceof JTextField tf) {
                    tf.setText("");
                } else if (control instanceof DateInputPanel dp) {
                    dp.setText("");
                } else if (control instanceof JComboBox<?> combo) {
                    if (combo.getItemCount() > 0) combo.setSelectedIndex(0);
                }
            }
            setStatus("Form cleared");
        }
    }
}
