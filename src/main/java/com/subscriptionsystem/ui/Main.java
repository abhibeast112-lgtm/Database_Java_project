package com.subscriptionsystem.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.subscriptionsystem.database.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class Main {
    private static final Color DARK_BG = Color.decode("#08090D");
    private static final Color DARK_CARD = Color.decode("#171A24");
    private static final Color COBALT = Color.decode("#2547C8");
    private static final Color LIGHT_BG = Color.decode("#F8F0E5");
    private static final Color LIGHT_CARD = Color.decode("#FFF9F3");
    private static final Color BURGUNDY = Color.decode("#6E2038");
    private static final Color PINK = Color.decode("#D8A0AE");

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
            UIManager.put("TextField.arc", 10);
            UIManager.put("Component.focusColor", accent());
            UIManager.put("Table.rowHeight", 36);
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

        for (Component component : sidebar.getComponents()) {
            if (component instanceof JLabel label) {
                label.setForeground(mutedColor());
            }
        }

        for (JButton button : navButtons.values()) {
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
        }

        for (CrudPanel panel : crudPanels) panel.applyTheme();

        showPage(pageTitle.getText().equals("Overview")
                ? "Overview" : pageTitle.getText());

        frame.getContentPane().setBackground(background());
        frame.repaint();
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
        final DefaultTableModel model = new DefaultTableModel();
        final Map<String, JTextField> inputs = new LinkedHashMap<>();
        final List<JButton> actionButtons = new ArrayList<>();
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
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.setFillsViewportHeight(true);

            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) fillFromSelection();
            });

            JPanel top = new JPanel(new BorderLayout(12, 12));
            JLabel section = new JLabel("All " + label.toLowerCase());
            section.setFont(new Font("SansSerif", Font.BOLD, 19));

            JButton refresh = new JButton("↻  Refresh");
            refresh.setFont(new Font("SansSerif", Font.BOLD, 13));
            refresh.addActionListener(e -> loadData());
            actionButtons.add(refresh);

            top.add(section, BorderLayout.WEST);
            top.add(refresh, BorderLayout.EAST);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createEmptyBorder());

            JPanel editor = new JPanel(new BorderLayout(0, 14));
            editor.setBorder(new EmptyBorder(18, 20, 18, 20));

            JLabel formTitle = new JLabel("Record details");
            formTitle.setFont(new Font("SansSerif", Font.BOLD, 22));

            JPanel fields = new JPanel(new GridLayout(0, 4, 12, 10));
            for (String field : spec.fields) {
                JLabel name = new JLabel(pretty(field));
                name.setFont(new Font("SansSerif", Font.PLAIN, 15));

                JTextField input = new JTextField();
                input.setFont(new Font("SansSerif", Font.PLAIN, 16));
                input.setPreferredSize(new Dimension(220, 44));
                input.putClientProperty("JTextField.placeholderText", pretty(field));
                inputs.put(field, input);

                fields.add(name);
                fields.add(input);
            }

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
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

            actions.add(insert);
            actions.add(update);
            actions.add(delete);
            actions.add(clear);

            actionButtons.add(insert);
            actionButtons.add(update);
            actionButtons.add(delete);
            actionButtons.add(clear);

            JPanel formContent = new JPanel(new BorderLayout(0, 14));
            formContent.add(formTitle, BorderLayout.NORTH);
            formContent.add(fields, BorderLayout.CENTER);
            editor.add(formContent, BorderLayout.CENTER);
            editor.add(actions, BorderLayout.SOUTH);

            add(top, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            add(editor, BorderLayout.SOUTH);
        }

        private void styleAction(JButton button, boolean primary) {
            button.putClientProperty("primaryAction", primary);
            button.setFont(new Font("SansSerif", Font.BOLD, 13));
            button.setPreferredSize(new Dimension(primary ? 160 : 150, 42));
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
            button.setBorderPainted(false);
            button.setBackground(primary ? accent() : surface());
            button.setForeground(primary ? Color.WHITE : textColor());
        }

        void applyTheme() {
            setBackground(background());
            table.setBackground(surface());
            table.setForeground(textColor());
            table.setSelectionBackground(accent());
            table.setSelectionForeground(Color.WHITE);
            table.setGridColor(dark
                    ? Color.decode("#303441") : Color.decode("#E5D8C9"));
            table.getTableHeader().setBackground(surface());
            table.getTableHeader().setForeground(textColor());

            for (Component component : getComponents()) {
                if (component instanceof JPanel panel) themePanel(panel);
            }

            for (JButton button : actionButtons) {
                if (button.getClientProperty("primaryAction") != null) {
                    styleActionColors(button,
                            Boolean.TRUE.equals(button.getClientProperty("primaryAction")));
                } else {
                    button.setBackground(surface());
                    button.setForeground(textColor());
                    button.setOpaque(true);
                }
            }
            repaint();
        }

        private void themePanel(Container container) {
            container.setBackground(background());
            for (Component c : container.getComponents()) {
                if (c instanceof JLabel label) label.setForeground(textColor());
                if (c instanceof JTextField field) {
                    field.setBackground(surface());
                    field.setForeground(textColor());
                    field.setCaretColor(textColor());
                }
                if (c instanceof JPanel child) themePanel(child);
            }
        }

        void loadData() {
            String sql = "SELECT * FROM " + spec.table
                    + " ORDER BY " + spec.key + " DESC";
            try (Connection c = DBConnection.getConnection();
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(sql)) {
                ResultSetMetaData md = rs.getMetaData();
                model.setRowCount(0);
                model.setColumnCount(0);

                for (int i = 1; i <= md.getColumnCount(); i++) {
                    model.addColumn(pretty(md.getColumnLabel(i)));
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
            for (int i = 0; i < model.getColumnCount(); i++) {
                if (model.getColumnName(i).equalsIgnoreCase(pretty(field))) return i;
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
                if (col >= 0) {
                    Object value = model.getValueAt(row, col);
                    inputs.get(field).setText(value == null ? "" : value.toString());
                }
            }
        }

        private List<String> values() {
            List<String> result = new ArrayList<>();
            for (String field : spec.fields) result.add(inputs.get(field).getText().trim());
            return result;
        }

        private boolean validate(List<String> values) {
            if (values.stream().anyMatch(String::isBlank)) {
                JOptionPane.showMessageDialog(frame,
                        "Please fill in all fields.", "Missing information",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }
            return true;
        }

        private void insertRecord() {
            List<String> values = values();
            if (!validate(values)) return;

            String sql = "INSERT INTO " + spec.table + " ("
                    + String.join(", ", spec.fields) + ") VALUES ("
                    + String.join(", ", Collections.nCopies(values.size(), "?")) + ")";

            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < values.size(); i++) ps.setString(i + 1, values.get(i));
                ps.executeUpdate();
                clearForm();
                loadData();
                refreshCounts();
                JOptionPane.showMessageDialog(frame, label + " inserted successfully.");
            } catch (SQLException ex) {
                showError("Insert failed", ex);
            }
        }

        private void updateRecord() {
            if (selectedId == null) {
                JOptionPane.showMessageDialog(frame, "Select a row to update.");
                return;
            }
            List<String> values = values();
            if (!validate(values)) return;

            List<String> sets = new ArrayList<>();
            for (String field : spec.fields) sets.add(field + " = ?");
            String sql = "UPDATE " + spec.table + " SET "
                    + String.join(", ", sets) + " WHERE " + spec.key + " = ?";

            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < values.size(); i++) ps.setString(i + 1, values.get(i));
                ps.setObject(values.size() + 1, selectedId);
                ps.executeUpdate();
                clearForm();
                loadData();
                refreshCounts();
                setStatus("Record updated successfully");
            } catch (SQLException ex) {
                showError("Update failed", ex);
            }
        }

        private void deleteRecord() {
            if (selectedId == null) {
                JOptionPane.showMessageDialog(frame, "Select a row to delete.");
                return;
            }
            int choice = JOptionPane.showConfirmDialog(frame,
                    "Delete this record? This cannot be undone.",
                    "Confirm deletion", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;

            String sql = "DELETE FROM " + spec.table + " WHERE " + spec.key + " = ?";
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setObject(1, selectedId);
                ps.executeUpdate();
                clearForm();
                loadData();
                refreshCounts();
                setStatus("Record deleted successfully");
            } catch (SQLException ex) {
                showError("Delete failed",
                        new SQLException(ex.getMessage()
                                + "\nThis record may be referenced by another table.", ex));
            }
        }

        private void clearForm() {
            selectedId = null;
            table.clearSelection();
            for (JTextField input : inputs.values()) input.setText("");
        }
    }
}
