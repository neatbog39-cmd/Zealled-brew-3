/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pkgfinal.zealled.brew;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.util.Map;
import java.sql.*;
import javax.swing.Timer;
import java.sql.SQLException;
/**
 *
 * @author ASUS
 */
public class POS extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(POS.class.getName());
    
    // POS variables 
    private double subtotal = 0;
    private double taxRate = 0.12;
    private double total = 0;
    private String cashInput = "";
    private String receiptItems = "";
    private Map<String, Integer> itemQtyMap = new HashMap<>();
    private Map<String, Double> itemPriceMap = new HashMap<>();
    private Map<String, Integer> productIdMap = new HashMap<>();
    private Map<String, String[]> itemAddonsMap = new HashMap<>();
    private DefaultTableModel productModel;
    private DefaultTableModel addonsModel;
    private String selectedReceiptItem = "";
    private DefaultTableModel orderModel;
    /**
     * Creates new form POS
     */
    
        // ========================= CONSTRUCTOR & FIELDS =========================
    public POS() {
        initComponents();
        setupModels();
        setupAddonsTable();
        loadAddonsTable();     
        loadPOSTables();
        showWelcomeDisplay();
        setupEventListeners();
        loadAddonsToComboBox();
        setupCombos();
        setupOrderTable();
    }
    
    
    // ========================= TABLE & UI SETUP =========================
    // Products
    private void setupModels() {
    // MATCH Products form EXACTLY
    String[] columns = {"ID", "Name", "Category", "Size", "Selling Price", "Quantity"};
    productModel = new DefaultTableModel(columns, 0) { 
        public boolean isCellEditable(int row, int col) { return false; } 
    };
    jTableProducts.setModel(productModel);
    
    // Add formatting like Products form
    formatPOSTableDisplay();
}
    // Addons
    private void setupAddonsTable() {
    String[] columns = {"Addon Name", "Price"};  
    addonsModel = new DefaultTableModel(columns, 0) { 
        public boolean isCellEditable(int row, int col) { return false; } 
    };
    
    jTableAddons.setModel(addonsModel);
    
    
    jTableAddons.getColumnModel().getColumn(0).setPreferredWidth(180); 
    jTableAddons.getColumnModel().getColumn(1).setPreferredWidth(80);  
    
    
    javax.swing.table.DefaultTableCellRenderer rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
    rightRenderer.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jTableAddons.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
}
    // Color
    private void formatPOSTableDisplay() {
    java.text.NumberFormat currencyFormat = new java.text.DecimalFormat("₱#,##0.00");
    javax.swing.table.TableCellRenderer priceRenderer = (javax.swing.JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) -> {
        javax.swing.JLabel label = new javax.swing.JLabel();
        if (value != null) {
            try {
                double price = Double.parseDouble(value.toString()); // ✅ works for both Integer and Double
                label.setText(currencyFormat.format(price));
            } catch (Exception ex) {
                label.setText("₱0.00");
            }
        } else {
            label.setText("₱0.00");
        }
        label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        if (isSelected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            label.setBackground(table.getBackground());
            label.setForeground(table.getForeground());
        }
        label.setOpaque(true);
        return label;
    };
    jTableProducts.getColumnModel().getColumn(4).setCellRenderer(priceRenderer);

    // Quantity renderer — NO CHANGES NEEDED HERE
    javax.swing.table.TableCellRenderer quantityRenderer = (javax.swing.JTable table, Object value, 
        boolean isSelected, boolean hasFocus, int row, int column) -> {
        javax.swing.JLabel label = new javax.swing.JLabel();
        if (value != null && value instanceof Integer) {
            int qty = (Integer) value;
            label.setText(qty == 0 ? "Out of Stock" : String.valueOf(qty));
            if (qty == 0) label.setForeground(java.awt.Color.RED);
            else if (qty <= 5) label.setForeground(java.awt.Color.ORANGE);
        } else {
            label.setText(value != null ? value.toString() : "0");
        }
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        if (isSelected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            label.setBackground(table.getBackground());
            label.setForeground(table.getForeground());
        }
        label.setOpaque(true);
        return label;
    };
    jTableProducts.getColumnModel().getColumn(5).setCellRenderer(quantityRenderer);
}
    // Dine in / Take out
    private void setupCombos() {
        cmbOrderType.removeAllItems();
        cmbOrderType.addItem("Dine In");
        cmbOrderType.addItem("Take Out");
        cmbAddons.setSelectedIndex(0);
    }
    // Search / Stock Colors / POS Display
    private void setupEventListeners() {
        // Search
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchProducts(txtSearch.getText().trim());
            }
        });
        
        // Color coding
        jTableProducts.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    int stock = Integer.parseInt(table.getValueAt(row, 5).toString());
                    if (stock <= 0) c.setForeground(java.awt.Color.RED);
                    else if (stock <= 5) c.setForeground(java.awt.Color.ORANGE);
                    else c.setForeground(new java.awt.Color(0, 128, 0));
                } catch (Exception ignored) {}
                return c;
            }
        });
        
        txtDisplayPOS.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 16));
        txtDisplayPOS.setMargin(new java.awt.Insets(10, 20, 10, 10));
        txtDisplayPOS.setEditable(false);
        
        
    }
    
    
    // ========================= DATA LOADERS =========================
    
    public void loadPOSTables() {
    productModel.setRowCount(0);
    String sql = "SELECT p.ProductID, p.Name, c.category_name, p.Size, p.Price, p.Quantity " +
                "FROM products p LEFT JOIN category c ON p.Category = c.category_name ORDER BY p.Name";
    try (Connection con = ConnectorXampp.connect(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
        while (rs.next()) {
            productModel.addRow(new Object[]{
                rs.getInt("ProductID"), 
                rs.getString("Name"), 
                rs.getString("category_name"),
                rs.getString("Size"), 
                rs.getDouble("Price"),   
                rs.getInt("Quantity")    
            });
        }
        formatPOSTableDisplay();  
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
    }
}
    // Search
    private void searchProducts(String keyword) {
    productModel.setRowCount(0);
    if (keyword.isEmpty()) { 
        loadPOSTables(); 
        return; 
    }
    String sql = "SELECT p.ProductID, p.Name, c.category_name, p.Size, p.Price, p.Quantity " +
                "FROM products p LEFT JOIN category c ON p.Category = c.category_name " +
                "WHERE p.Quantity > 0 AND (p.Name LIKE ? OR c.category_name LIKE ?) ORDER BY p.Name";
    try (Connection con = ConnectorXampp.connect(); PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, "%" + keyword + "%"); 
        ps.setString(2, "%" + keyword + "%");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            productModel.addRow(new Object[]{
                rs.getInt("ProductID"), 
                rs.getString("Name"), 
                rs.getString("category_name"),
                rs.getString("Size"), 
                rs.getDouble("Price"),      
                rs.getInt("Quantity")    
            });
        }
        formatPOSTableDisplay(); 
    } catch (Exception e) {
        System.out.println("Search failed: " + e.getMessage());
    }
}
    
    private void loadAddonsToComboBox() {
        cmbAddons.removeAllItems();
        cmbAddons.addItem("None");
        String sql = "SELECT Name FROM addons ORDER BY Name";
        try (Connection con = ConnectorXampp.connect(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) cmbAddons.addItem(rs.getString("Name"));
        } catch (Exception e) {
            System.out.println("Addons load failed: " + e.getMessage());
        }
    }
    
    private void loadAddonsTable() {
    addonsModel.setRowCount(0);
    String sql = "SELECT Name, Price FROM addons ORDER BY Name";  
    try (Connection con = ConnectorXampp.connect(); 
         Statement st = con.createStatement(); 
         ResultSet rs = st.executeQuery(sql)) {
        
        while (rs.next()) {
            double price = rs.getDouble("Price");
            addonsModel.addRow(new Object[]{
                rs.getString("Name"),          
                String.format("₱%.2f", price)  
            });
        }
    } catch (Exception e) {
        System.out.println("Addons table load failed: " + e.getMessage());
    }
}
    
    // ========================= ADDON HELPERS =========================
    
    private double getAddonPrice(String addonName) { 
    if (addonName.equals("None")) return 0;
    
    String sql = "SELECT Price FROM addons WHERE Name = ?";  
    try (Connection con = ConnectorXampp.connect();
         PreparedStatement pst = con.prepareStatement(sql)) {
        pst.setString(1, addonName);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getDouble("Price");  
        }
    } catch (Exception e) {
        System.out.println("Addon price error: " + e.getMessage());
    }
    return 0;
}
    
    private String extractAddonName(String displayName) {
    if (displayName.contains("+")) {
        return displayName.split("\\+")[1].trim();
    }
    return "None";
}
    
    private int findProductIdByName(String displayName) {
    try (Connection con = ConnectorXampp.connect();
         PreparedStatement ps = con.prepareStatement(
             "SELECT ProductID FROM products WHERE Name LIKE ? LIMIT 1")) {
        
        ps.setString(1, "%" + displayName.split("\\$")[0] + "%"); 
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("ProductID");
        }
    } catch (Exception e) {
        System.out.println("Find ProductID error: " + e.getMessage());
    }
    return -1;
}

    private double getBasePrice(int productId) {  
    try (Connection con = ConnectorXampp.connect();
         PreparedStatement ps = con.prepareStatement("SELECT Price FROM products WHERE ProductID = ?")) {
        ps.setInt(1, productId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getDouble("Price");  
    } catch (Exception e) {
        System.out.println("Base price error: " + e.getMessage());
    }
    return 0;
}
    
   // ========================= RECEIPT & PAYMENT =========================
   
    // Receipt Display
    private void showWelcomeDisplay() {
    String cashierName = getCurrentCashierName();
    String orderDateTime = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a").format(new java.util.Date());
    
    txtDisplayPOS.setText(
        "☕ ZEALLED BREWS POS ☕\n" +
        "============================\n" +
        "Cashier: " + cashierName + "\n" +
        "Date: " + orderDateTime + "\n" +
        "Type: Dine In/Take Out\n\n" +
        "👆 CLICK PRODUCT\n" +
        "➕ SELECT ADDON (optional)\n" +
        "🔢 ENTER QUANTITY\n" +
        "💰 USE CALCULATOR\n" +
        "✅ PRESS PAY\n\n" +
        "----------------------------\nReady!"
    );
}
    // Receipt
    private void updateReceiptDisplay() {
    receiptItems = "";
    int totalItems = 0;
    subtotal = 0; // ✅ recalculate from table instead of maps

    for (int i = 0; i < orderModel.getRowCount(); i++) {
        String productName = orderModel.getValueAt(i, 0).toString();
        String addon = orderModel.getValueAt(i, 2).toString();
        int qty = (int) orderModel.getValueAt(i, 3);
        double unitPrice = ((Number) orderModel.getValueAt(i, 4)).doubleValue();
        double rowSubtotal = unitPrice * qty;

        subtotal += rowSubtotal;
        totalItems += qty;

        String displayLine = productName + (addon.equals("None") ? "" : " +" + addon);
        receiptItems += String.format("%-25s x%-2d ₱%.2f\n", displayLine, qty, rowSubtotal);
    }

    double vatable = subtotal / 1.12;
    double vat = vatable * 0.12;
    total = vatable + vat;

    String cashierName = getCurrentCashierName();
    String orderDateTime = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a").format(new java.util.Date());
    String orderType = cmbOrderType.getSelectedItem() != null ?
                       cmbOrderType.getSelectedItem().toString() : "Dine In";

    String tableNo = txtTableNumber.getText().trim().isEmpty() ? "—" : txtTableNumber.getText().trim();
    String customerName = txtCustomerName.getText().trim().isEmpty() ? "—" : txtCustomerName.getText().trim();

    double change = cashInput.isEmpty() ? 0 : Double.parseDouble(cashInput) - total;

    txtDisplayPOS.setText(
        "☕ ZEALLED BREWS ☕\n" +
        "============================\n" +
        "Cashier:  " + cashierName + "\n" +
        "Date:     " + orderDateTime + "\n" +
        "Type:     " + orderType + "\n" +
        "Table #:  " + tableNo + "\n" +
        "Customer: " + customerName + "\n\n" +
        receiptItems +
        "\n----------------------------\n" +
        "Total Items: " + totalItems + "\n" +
        "Vatable:    ₱" + String.format("%.2f", vatable) + "\n" +
        "VAT(12%):   ₱" + String.format("%.2f", vat) + "\n" +
        "TOTAL:      ₱" + String.format("%.2f", total) + "\n" +
        (cashInput.isEmpty() ? "" :
        "Cash:       ₱" + String.format("%.2f", Double.parseDouble(cashInput)) + "\n" +
        "Change:     ₱" + String.format("%.2f", change))
    );
    txtDisplayPOS.setCaretPosition(txtDisplayPOS.getDocument().getLength());
}
    // Cashier name from the Utilities
    private String getCurrentCashierName() {
    try (Connection con = ConnectorXampp.connect();
         PreparedStatement ps = con.prepareStatement(
             "SELECT full_name FROM users WHERE last_login = (SELECT MAX(last_login) FROM users WHERE role = 'Cashier') LIMIT 1")) {
        
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String fullName = rs.getString("full_name");
            return fullName != null ? fullName : "Cashier";
        }
    } catch (Exception e) {
        System.out.println("Cashier name lookup failed: " + e.getMessage());
    }
    return "Cashier";
}
   
    
    // Sum
    private void appendCash(String digit) {
        cashInput += digit;
        updateReceiptDisplay();
    }
    
    // Payment
    private void processPayment() {
    if (itemQtyMap.isEmpty()) {
        JOptionPane.showMessageDialog(this, "📦 Add items first!");
        return;
    }
    if (cashInput.isEmpty()) {
        JOptionPane.showMessageDialog(this, "💰 Enter cash amount!");
        return;
    }
    
    try {
        double cash = Double.parseDouble(cashInput);
        if (cash < total) {
            JOptionPane.showMessageDialog(this, String.format("💳 Need ₱%.2f more!", total - cash));
            return;
        }
        
        // 🔥 VAT BREAKDOWN IN CONFIRMATION
        double vatable = subtotal / 1.12;
        double vat = vatable * 0.12;
        
        if (JOptionPane.showConfirmDialog(this, 
            "Vatable:  ₱" + String.format("%.2f", vatable) + "\n" +
            "VAT(12%): ₱" + String.format("%.2f", vat) + "\n" +
            "TOTAL:    ₱" + String.format("%.2f", total) + "\n\n" +
            "Cash:     ₱" + String.format("%.2f", cash) + "\n" +
            "Change:   ₱" + String.format("%.2f", cash - total),
            "Confirm Payment", JOptionPane.YES_OPTION) == JOptionPane.YES_OPTION) {
            
            saveOrder(cash, cash - total);
            
            String finalReceipt = txtDisplayPOS.getText() + 
                "\n============================\n" +
                "✅ PAID! THANK YOU! ☕\n" +
                "🔔 Receipt printed\n" +
                "🔄 Resetting...";
            
            txtDisplayPOS.setText(finalReceipt);
            txtDisplayPOS.setCaretPosition(txtDisplayPOS.getDocument().getLength());
            
            new Timer(3000, e -> {
                resetPOS();
                ((Timer)e.getSource()).stop();
            }).start();
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "❌ Invalid cash amount!");
    }
}
    
    // ========================= ORDER SAVING (History/Transaction) =========================
    
    private void saveOrder(double cash, double change) {
    Connection con = null;
    try {
        con = ConnectorXampp.connect();
        con.setAutoCommit(false);
        
       
        int userId = getCurrentCashierId();
        
        
        PreparedStatement ps = con.prepareStatement(
        "INSERT INTO orders (`OrderDate`, `TotalAmount`, `OrderType`, `UserID`, `Cash`, `Change`, `TableNumber`, `CustomerName`) VALUES(NOW(), ?, ?, ?, ?, ?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS
        );
        ps.setDouble(1, total);
        ps.setString(2, cmbOrderType.getSelectedItem().toString());
        ps.setInt(3, userId);
        ps.setDouble(4, cash);
        ps.setDouble(5, change);
        ps.setString(6, txtTableNumber.getText().trim().isEmpty() ? "—" : txtTableNumber.getText().trim()); // ✅ NEW
        ps.setString(7, txtCustomerName.getText().trim().isEmpty() ? "—" : txtCustomerName.getText().trim()); // ✅ NEW
        ps.executeUpdate();
        
        ResultSet rs = ps.getGeneratedKeys();
        int orderId = rs.next() ? rs.getInt(1) : -1;
        
       
        for (String displayName : itemQtyMap.keySet()) {
            int qty = itemQtyMap.get(displayName);
            int productId = findProductIdForDisplayName(displayName);
            if (productId == -1) continue;
            
            // Deduct stock
            String stockSql = "UPDATE products SET Quantity = Quantity - ? WHERE ProductID = ?";
            try (PreparedStatement stockPs = con.prepareStatement(stockSql)) {
                stockPs.setInt(1, qty);
                stockPs.setInt(2, productId);
                stockPs.executeUpdate();
            }
            
            // Save order detail
            String[] allAddons = itemAddonsMap.get(displayName);
            String firstAddon = (allAddons != null && allAddons.length > 0) ? allAddons[0] : "None";
            double addonPrice = getAddonPrice(firstAddon);
            double basePrice = getBasePrice(productId);
            double unitPrice = itemPriceMap.get(displayName);
            
            String detailSql = "INSERT INTO order_details (OrderID, ProductID, AddonName, AddonPrice, BasePrice, Quantity, Subtotal) VALUES(?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement detailPs = con.prepareStatement(detailSql)) {
                detailPs.setInt(1, orderId);
                detailPs.setInt(2, productId);
                detailPs.setString(3, firstAddon);
                detailPs.setDouble(4, addonPrice);
                detailPs.setDouble(5, basePrice);
                detailPs.setInt(6, qty);
                detailPs.setDouble(7, unitPrice * qty);
                detailPs.executeUpdate();
            }
        }
        
        con.commit();
        System.out.println("✅ Order #" + orderId + " SAVED with Cashier ID: " + userId);
        Products.refreshAllStock();
        
    } catch (Exception e) {
        if (con != null) {
            try { con.rollback(); } catch (Exception rollbackEx) {}
        }
        JOptionPane.showMessageDialog(this, "❌ Error: " + e.getMessage());
        e.printStackTrace();
    } finally {
        if (con != null) {
            try { con.close(); } catch (Exception closeEx) {}
        }
    }
}


    private int getCurrentCashierId() {
    String sql = "SELECT id FROM users WHERE role = 'Cashier' AND last_login IS NOT NULL ORDER BY last_login DESC LIMIT 1";
    try (Connection con = ConnectorXampp.connect();
         Statement st = con.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
        if (rs.next()) return rs.getInt("id");
    } catch (Exception e) {
        System.out.println("Cashier ID lookup failed: " + e.getMessage());
    }
    return 1; 
}
    
    private int findProductIdForDisplayName(String displayName) {
   
    for (String key : productIdMap.keySet()) {
        if (displayName.contains(key.replace("ID_", ""))) {
            return productIdMap.get(key);
        }
    }
    
    
    try {
        String baseName = displayName.split("\\$")[0].trim(); 
        try (Connection con = ConnectorXampp.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT ProductID FROM products WHERE Name = ? LIMIT 1")) {
            ps.setString(1, baseName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("ProductID");
            }
        }
    } catch (Exception e) {
        System.out.println("ProductID lookup failed: " + e.getMessage());
    }
    
    return -1;
}
 
    private int getProductRow(int productId) {
    for (int i = 0; i < jTableProducts.getRowCount(); i++) {
        if ((Integer) jTableProducts.getValueAt(i, 0) == productId) {
            return i;
        }
    }
    return -1;
} 
    
    // ========================= RESET =========================
    private void resetPOS() {
    subtotal = total = 0;
    cashInput = receiptItems = "";
    itemQtyMap.clear();
    itemPriceMap.clear();
    productIdMap.clear();
    itemAddonsMap.clear();
    orderModel.setRowCount(0); // ✅ clear order table on reset

    txtSearch.setText("");
    cmbAddons.setSelectedIndex(0);

    txtDisplayPOS.setText("🔄 POS Reset Complete!\nReady for next order...");
    new Timer(1000, e -> {
        showWelcomeDisplay();
        loadPOSTables();
        ((Timer)e.getSource()).stop();
    }).start();
}
    
    private void setupOrderTable() {
    String[] columns = {"Product Name", "Size", "Addon", "Qty", "Unit Price", "Subtotal", "Table #", "Customer"};
    orderModel = new DefaultTableModel(columns, 0) {
        public boolean isCellEditable(int row, int col) { return false; }
    };
    jTableOrder.setModel(orderModel);

    java.text.NumberFormat currencyFormat = new java.text.DecimalFormat("₱#,##0.00");
    javax.swing.table.TableCellRenderer priceRenderer = (table, value, isSelected, hasFocus, row, column) -> {
        javax.swing.JLabel label = new javax.swing.JLabel();
        if (value != null) {
            try {
                double price = Double.parseDouble(value.toString());
                label.setText(currencyFormat.format(price));
            } catch (Exception ex) {
                label.setText("₱0.00");
            }
        } else {
            label.setText("₱0.00");
        }
        label.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        label.setOpaque(true);
        if (isSelected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            label.setBackground(table.getBackground());
            label.setForeground(table.getForeground());
        }
        return label;
    };
    jTableOrder.getColumnModel().getColumn(4).setCellRenderer(priceRenderer); // Unit Price
    jTableOrder.getColumnModel().getColumn(5).setCellRenderer(priceRenderer); // Subtotal

    // Column widths
    jTableOrder.getColumnModel().getColumn(0).setPreferredWidth(150); // Product Name
    jTableOrder.getColumnModel().getColumn(1).setPreferredWidth(60);  // Size
    jTableOrder.getColumnModel().getColumn(2).setPreferredWidth(100); // Addon
    jTableOrder.getColumnModel().getColumn(3).setPreferredWidth(40);  // Qty
    jTableOrder.getColumnModel().getColumn(4).setPreferredWidth(90);  // Unit Price
    jTableOrder.getColumnModel().getColumn(5).setPreferredWidth(90);  // Subtotal
    jTableOrder.getColumnModel().getColumn(6).setPreferredWidth(70);  // Table #
    jTableOrder.getColumnModel().getColumn(7).setPreferredWidth(100); // Customer
}
   
   
   
   
  
   
   
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane3 = new javax.swing.JScrollPane();
        jTableCart = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtDisplayPOS = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        btnPAY = new javax.swing.JButton();
        btnErase = new javax.swing.JButton();
        btn0 = new javax.swing.JButton();
        btn1 = new javax.swing.JButton();
        btn2 = new javax.swing.JButton();
        btn3 = new javax.swing.JButton();
        btn4 = new javax.swing.JButton();
        btn5 = new javax.swing.JButton();
        btn6 = new javax.swing.JButton();
        btn7 = new javax.swing.JButton();
        btn8 = new javax.swing.JButton();
        btn9 = new javax.swing.JButton();
        btnAC = new javax.swing.JButton();
        btnLogOut = new javax.swing.JButton();
        btnAddAddons = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableProducts = new javax.swing.JTable();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTableAddons = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        txtTableNumber = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        txtCustomerName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        cmbOrderType = new javax.swing.JComboBox<>();
        cmbAddons = new javax.swing.JComboBox<>();
        txtSearch = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        btnReports = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTableOrder = new javax.swing.JTable();
        btnEditQty = new javax.swing.JButton();
        btnChangeAddon = new javax.swing.JButton();
        btnCancelItem = new javax.swing.JButton();
        btnClearAll = new javax.swing.JButton();

        jTableCart.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Item No", "ProductID", "Name", "Category", "Size", "Selling Price", "addonName", "addonPrice", "Price", "Quantity", "Subtotal"
            }
        ));
        jScrollPane3.setViewportView(jTableCart);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txtDisplayPOS.setColumns(20);
        txtDisplayPOS.setRows(5);
        jScrollPane1.setViewportView(txtDisplayPOS);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(1050, 0, 410, 530));

        jPanel2.setForeground(new java.awt.Color(51, 51, 51));

        btnPAY.setBackground(new java.awt.Color(40, 40, 40));
        btnPAY.setForeground(new java.awt.Color(197, 160, 114));
        btnPAY.setText("Pay");
        btnPAY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPAYActionPerformed(evt);
            }
        });

        btnErase.setBackground(new java.awt.Color(40, 40, 40));
        btnErase.setForeground(new java.awt.Color(197, 160, 114));
        btnErase.setText("<--");
        btnErase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEraseActionPerformed(evt);
            }
        });

        btn0.setBackground(new java.awt.Color(40, 40, 40));
        btn0.setForeground(new java.awt.Color(197, 160, 114));
        btn0.setText("0");
        btn0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn0ActionPerformed(evt);
            }
        });

        btn1.setBackground(new java.awt.Color(40, 40, 40));
        btn1.setForeground(new java.awt.Color(197, 160, 114));
        btn1.setText("1");
        btn1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn1ActionPerformed(evt);
            }
        });

        btn2.setBackground(new java.awt.Color(40, 40, 40));
        btn2.setForeground(new java.awt.Color(197, 160, 114));
        btn2.setText("2");
        btn2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn2ActionPerformed(evt);
            }
        });

        btn3.setBackground(new java.awt.Color(40, 40, 40));
        btn3.setForeground(new java.awt.Color(197, 160, 114));
        btn3.setText("3");
        btn3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn3ActionPerformed(evt);
            }
        });

        btn4.setBackground(new java.awt.Color(40, 40, 40));
        btn4.setForeground(new java.awt.Color(197, 160, 114));
        btn4.setText("4");
        btn4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn4ActionPerformed(evt);
            }
        });

        btn5.setBackground(new java.awt.Color(40, 40, 40));
        btn5.setForeground(new java.awt.Color(197, 160, 114));
        btn5.setText("5");
        btn5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn5ActionPerformed(evt);
            }
        });

        btn6.setBackground(new java.awt.Color(40, 40, 40));
        btn6.setForeground(new java.awt.Color(197, 160, 114));
        btn6.setText("6");
        btn6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn6ActionPerformed(evt);
            }
        });

        btn7.setBackground(new java.awt.Color(40, 40, 40));
        btn7.setForeground(new java.awt.Color(197, 160, 114));
        btn7.setText("7");
        btn7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn7ActionPerformed(evt);
            }
        });

        btn8.setBackground(new java.awt.Color(40, 40, 40));
        btn8.setForeground(new java.awt.Color(197, 160, 114));
        btn8.setText("8");
        btn8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn8ActionPerformed(evt);
            }
        });

        btn9.setBackground(new java.awt.Color(40, 40, 40));
        btn9.setForeground(new java.awt.Color(197, 160, 114));
        btn9.setText("9");
        btn9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn9ActionPerformed(evt);
            }
        });

        btnAC.setBackground(new java.awt.Color(40, 40, 40));
        btnAC.setForeground(new java.awt.Color(197, 160, 114));
        btnAC.setText("AC");
        btnAC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnACActionPerformed(evt);
            }
        });

        btnLogOut.setBackground(new java.awt.Color(40, 40, 40));
        btnLogOut.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnLogOut.setForeground(new java.awt.Color(197, 160, 114));
        btnLogOut.setText("Log Out");
        btnLogOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogOutActionPerformed(evt);
            }
        });

        btnAddAddons.setBackground(new java.awt.Color(40, 40, 40));
        btnAddAddons.setForeground(new java.awt.Color(197, 160, 114));
        btnAddAddons.setText("Add");
        btnAddAddons.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddAddonsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(64, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(btnAddAddons, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnPAY, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(48, 48, 48))
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addComponent(btn1, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn2, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn3, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addComponent(btn4, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn5, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn6, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addComponent(btn7, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn8, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn9, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addComponent(btnAC, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn0, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btnErase, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addContainerGap()))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(btnLogOut, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(78, 78, 78))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(548, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn2, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn3, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn4, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn5, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn6, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn7, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn8, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn9, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAC, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn0, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnErase, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAddAddons, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnPAY, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnLogOut)
                .addGap(79, 79, 79))
        );

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(1050, 0, 410, 880));

        jTableProducts.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        jTableProducts.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ProductID", "Name", "Category", "Size", "Price", "Quantity"
            }
        ));
        jTableProducts.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTableProductsMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jTableProducts);

        jTableAddons.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Addon Name", "Price"
            }
        ));
        jScrollPane4.setViewportView(jTableAddons);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1028, Short.MAX_VALUE)
                    .addComponent(jScrollPane4))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 220, 1040, 310));

        jLabel1.setText("Table Number");

        jLabel2.setText("Customer name");

        cmbOrderType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Dine In", "Take Out" }));

        cmbAddons.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None", "Black Pearl", "Topping Boba", "Fruity Jelly", "Yakult", "Oreo", "Lychee" }));

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Times New Roman", 3, 18)); // NOI18N
        jLabel3.setText("Search:");

        btnReports.setBackground(new java.awt.Color(18, 20, 23));
        btnReports.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnReports.setForeground(new java.awt.Color(197, 160, 114));
        btnReports.setText("Reports");
        btnReports.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReportsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(txtTableNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCustomerName, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbOrderType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmbAddons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(133, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(btnReports)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(39, 39, 39))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap(35, Short.MAX_VALUE)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(27, 27, 27))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addComponent(btnReports)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtTableNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtCustomerName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cmbOrderType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(11, 11, 11))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addComponent(cmbAddons, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );

        jPanel1.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 550, 170));

        jTableOrder.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane5.setViewportView(jTableOrder);

        btnEditQty.setText("Edit Qty");
        btnEditQty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditQtyActionPerformed(evt);
            }
        });

        btnChangeAddon.setText("Change Addon");
        btnChangeAddon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChangeAddonActionPerformed(evt);
            }
        });

        btnCancelItem.setText("Cancel Item");
        btnCancelItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelItemActionPerformed(evt);
            }
        });

        btnClearAll.setText("Clear All");
        btnClearAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearAllActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 1028, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(btnEditQty)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnChangeAddon)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancelItem)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnClearAll)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEditQty)
                    .addComponent(btnChangeAddon)
                    .addComponent(btnCancelItem)
                    .addComponent(btnClearAll))
                .addGap(29, 29, 29)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(111, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 530, 1040, 350));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 885, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn1ActionPerformed
        // TODO add your handling code here:
        appendCash("1");
    }//GEN-LAST:event_btn1ActionPerformed

    private void btn2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn2ActionPerformed
        // TODO add your handling code here:
        appendCash("2");
    }//GEN-LAST:event_btn2ActionPerformed

    private void btn3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn3ActionPerformed
        // TODO add your handling code here:
        appendCash("3");
    }//GEN-LAST:event_btn3ActionPerformed

    private void btn4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn4ActionPerformed
        // TODO add your handling code here:
        appendCash("4");
    }//GEN-LAST:event_btn4ActionPerformed

    private void btn5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn5ActionPerformed
        // TODO add your handling code here:
        appendCash("5");
    }//GEN-LAST:event_btn5ActionPerformed

    private void btn6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn6ActionPerformed
        // TODO add your handling code here:
        appendCash("6");
    }//GEN-LAST:event_btn6ActionPerformed

    private void btn7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn7ActionPerformed
        // TODO add your handling code here:
        appendCash("7");
    }//GEN-LAST:event_btn7ActionPerformed

    private void btn8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn8ActionPerformed
        // TODO add your handling code here:
        appendCash("8");
    }//GEN-LAST:event_btn8ActionPerformed

    private void btn9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn9ActionPerformed
        // TODO add your handling code here:
        appendCash("9");
    }//GEN-LAST:event_btn9ActionPerformed

    private void btn0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn0ActionPerformed
        // TODO add your handling code here:
        appendCash("0");
    }//GEN-LAST:event_btn0ActionPerformed

    private void btnEraseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEraseActionPerformed
        // TODO add your handling code here:
           if (cashInput.length() > 0) cashInput = cashInput.substring(0, cashInput.length() - 1);
        updateReceiptDisplay();
    }//GEN-LAST:event_btnEraseActionPerformed

    private void btnACActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnACActionPerformed
        // TODO add your handling code here:
        resetPOS();
    }//GEN-LAST:event_btnACActionPerformed

    private void btnPAYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPAYActionPerformed
        // TODO add your handling code here:
         processPayment();
    }//GEN-LAST:event_btnPAYActionPerformed

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
        // TODO add your handling code here:
          searchProducts(txtSearch.getText().trim());
    }//GEN-LAST:event_txtSearchKeyReleased

    private void jTableProductsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTableProductsMouseClicked
        // TODO add your handling code here:
     int row = jTableProducts.getSelectedRow();
    if (row < 0) return;

    int id = (Integer) jTableProducts.getValueAt(row, 0);
    String name = jTableProducts.getValueAt(row, 1).toString();
    String size = jTableProducts.getValueAt(row, 3).toString();
    double price = ((Number) jTableProducts.getValueAt(row, 4)).doubleValue();
    int stock = (Integer) jTableProducts.getValueAt(row, 5);

    if (stock <= 0) {
        JOptionPane.showMessageDialog(this, name + " ❌ OUT OF STOCK");
        return;
    }

    String tableNo = txtTableNumber.getText().trim().isEmpty() ? "—" : txtTableNumber.getText().trim();
    String customerName = txtCustomerName.getText().trim().isEmpty() ? "—" : txtCustomerName.getText().trim();

    String addon = (String) cmbAddons.getSelectedItem();
    double addonPrice = getAddonPrice(addon);

    String qtyText = JOptionPane.showInputDialog(
        this,
        "🍵 " + name + "\n📏 Size: " + size +
        "\n💰 Price: ₱" + String.format("%.2f", price) +
        (addon.equals("None") ? "" : "\n➕ " + addon + ": ₱" + String.format("%.2f", addonPrice)) +
        "\n📦 Stock: " + stock +
        "\n🪑 Table #: " + tableNo +
        "\n👤 Customer: " + customerName +
        "\n\nEnter Quantity:"
    );

    if (qtyText == null || qtyText.trim().isEmpty()) return;

    try {
        int qty = Integer.parseInt(qtyText.trim());
        if (qty <= 0 || qty > stock) {
            JOptionPane.showMessageDialog(this, "❌ Quantity must be 1 to " + stock);
            return;
        }

        double unitPrice = price + addonPrice;
        double rowSubtotal = unitPrice * qty;

        // ✅ ALWAYS ADD NEW ROW — no duplicate merging
        orderModel.addRow(new Object[]{
            name + " (" + size + ")",
            size,
            addon,
            qty,
            unitPrice,
            rowSubtotal,
            tableNo,
            customerName
        });

        subtotal += rowSubtotal;

        // Use row index as unique key to avoid map conflicts
        String displayKey = name + "_" + size + "_" + addon + "_" + System.currentTimeMillis();

        itemQtyMap.put(displayKey, qty);
        itemPriceMap.put(displayKey, unitPrice);
        productIdMap.put(displayKey, id);
        if (!addon.equals("None")) {
            itemAddonsMap.put(displayKey, new String[]{addon});
        }

        updateReceiptDisplay();
        cmbAddons.setSelectedIndex(0);

    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "❌ Enter number only!");
    }
    }//GEN-LAST:event_jTableProductsMouseClicked

    private void btnLogOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogOutActionPerformed
        // TODO add your handling code here:
        Login h = new Login();
        h.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnLogOutActionPerformed

    private void btnAddAddonsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddAddonsActionPerformed
        // TODO add your handling code here:
      int row = jTableProducts.getSelectedRow();
    if (row < 0) {
        JOptionPane.showMessageDialog(this, "👆 Select a product first!");
        return;
    }
    
    int id = (Integer) jTableProducts.getValueAt(row, 0);
    String name = jTableProducts.getValueAt(row, 1).toString();
    String size = jTableProducts.getValueAt(row, 3).toString();
    String baseItemKey = "ID_" + id;
    
    String addon = (String) cmbAddons.getSelectedItem();
    if (addon.equals("None")) {
        JOptionPane.showMessageDialog(this, "➕ Select an addon first!");
        return;
    }
    
    // Find product already in cart
    String displayName = name + " (" + size + ")";
    String currentAddonsStr = "";
    
    // Check if product exists in cart
    for (String cartItem : itemQtyMap.keySet()) {
        if (cartItem.contains(baseItemKey.replace("ID_", ""))) {
            displayName = cartItem;
            String[] existingAddons = itemAddonsMap.get(cartItem);
            if (existingAddons != null) {
                currentAddonsStr = String.join(" + ", existingAddons);
            }
            break;
        }
    }
    

    String newAddonStr = currentAddonsStr.isEmpty() ? addon : currentAddonsStr + " + " + addon;
    String finalDisplayName = displayName + " +" + newAddonStr;
    
   
    double basePrice = getBasePrice(id);
    double totalAddonPrice = getAddonPrice(addon);  
    
  
    String[] existingAddons = itemAddonsMap.get(displayName);
    if (existingAddons != null) {
        for (String exAddon : existingAddons) {
            totalAddonPrice += getAddonPrice(exAddon);
        }
    }
    
    double newUnitPrice = basePrice + totalAddonPrice;
    
    int confirm = JOptionPane.showConfirmDialog(
        this, 
        "☕ " + displayName + "\n" +
        "➕ Current: " + currentAddonsStr + "\n" +
        "➕ Add: " + addon + "\n\n" +
        "💰 New Price: ₱" + String.format("%.2f", newUnitPrice) + "\n\n" +
        "Confirm?",
        "Add Addon", 
        JOptionPane.YES_NO_OPTION
    );
    
    if (confirm == JOptionPane.YES_OPTION) {
      
        String[] addonsArray;
        if (existingAddons != null) {
            addonsArray = new String[existingAddons.length + 1];
            System.arraycopy(existingAddons, 0, addonsArray, 0, existingAddons.length);
            addonsArray[existingAddons.length] = addon;
        } else {
            addonsArray = new String[]{addon};
        }
        
       
        if (itemQtyMap.containsKey(displayName)) {
            int qty = itemQtyMap.get(displayName);
            double oldPrice = itemPriceMap.get(displayName);
            
           
            subtotal += (newUnitPrice - oldPrice) * qty;
            
            itemQtyMap.remove(displayName);
            itemPriceMap.remove(displayName);
            itemAddonsMap.remove(displayName);
            
            itemQtyMap.put(finalDisplayName, qty);
            itemPriceMap.put(finalDisplayName, newUnitPrice);  
            itemAddonsMap.put(finalDisplayName, addonsArray);
        }
        
       
        if (productIdMap.containsKey(baseItemKey)) {
            productIdMap.put(finalDisplayName, productIdMap.get(baseItemKey));
        }
        
        updateReceiptDisplay();  
        cmbAddons.setSelectedIndex(0);
    }
    }//GEN-LAST:event_btnAddAddonsActionPerformed

    private void btnReportsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReportsActionPerformed
        // TODO add your handling code here:
        Reports a = new Reports();
       a.setVisible(true);
       this.dispose();
    }//GEN-LAST:event_btnReportsActionPerformed

    private void btnCancelItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelItemActionPerformed
        // TODO add your handling code here:
         int selectedRow = jTableOrder.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "⚠️ Select an item to cancel.");
        return;
    }

    String productName = orderModel.getValueAt(selectedRow, 0).toString();
    int qty = (int) orderModel.getValueAt(selectedRow, 3);
    double unitPrice = ((Number) orderModel.getValueAt(selectedRow, 4)).doubleValue();

    int confirm = JOptionPane.showConfirmDialog(this,
        "❌ Remove '" + productName + "' from order?",
        "Cancel Item", JOptionPane.YES_NO_OPTION);

    if (confirm == JOptionPane.YES_OPTION) {
        subtotal -= unitPrice * qty;
        if (subtotal < 0) subtotal = 0;

        orderModel.removeRow(selectedRow);
        updateReceiptDisplay();
    }
    }//GEN-LAST:event_btnCancelItemActionPerformed

    private void btnEditQtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditQtyActionPerformed
        // TODO add your handling code here:
        int selectedRow = jTableOrder.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "⚠️ Select an item to edit.");
        return;
    }

    String productName = orderModel.getValueAt(selectedRow, 0).toString();
    String addon = orderModel.getValueAt(selectedRow, 2).toString();
    int currentQty = (int) orderModel.getValueAt(selectedRow, 3);
    double unitPrice = ((Number) orderModel.getValueAt(selectedRow, 4)).doubleValue();

    String newQtyText = JOptionPane.showInputDialog(this,
        "✏️ Edit quantity for: " + productName +
        "\nAddon: " + addon +
        "\nCurrent Qty: " + currentQty +
        "\n\nNew Quantity:", currentQty);

    if (newQtyText == null || newQtyText.trim().isEmpty()) return;

    try {
        int newQty = Integer.parseInt(newQtyText.trim());
        if (newQty <= 0) {
            JOptionPane.showMessageDialog(this, "❌ Quantity must be at least 1.");
            return;
        }

        subtotal -= unitPrice * currentQty;
        subtotal += unitPrice * newQty;
        if (subtotal < 0) subtotal = 0;

        orderModel.setValueAt(newQty, selectedRow, 3);
        orderModel.setValueAt(unitPrice * newQty, selectedRow, 5);

        updateReceiptDisplay();

    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "❌ Enter a valid number!");
    }
    }//GEN-LAST:event_btnEditQtyActionPerformed

    private void btnChangeAddonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnChangeAddonActionPerformed
        // TODO add your handling code here:
       int selectedRow = jTableOrder.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "⚠️ Select an item to change addon.");
        return;
    }

    String productName = orderModel.getValueAt(selectedRow, 0).toString();
    String currentAddon = orderModel.getValueAt(selectedRow, 2).toString();
    int qty = (int) orderModel.getValueAt(selectedRow, 3);
    double currentUnitPrice = ((Number) orderModel.getValueAt(selectedRow, 4)).doubleValue();

    String[] addonOptions = new String[cmbAddons.getItemCount()];
    for (int i = 0; i < cmbAddons.getItemCount(); i++) {
        addonOptions[i] = cmbAddons.getItemAt(i);
    }

    String newAddon = (String) JOptionPane.showInputDialog(
        this,
        "🔄 Change addon for: " + productName +
        "\nCurrent Addon: " + currentAddon +
        "\n\nSelect new addon:",
        "Change Addon",
        JOptionPane.QUESTION_MESSAGE,
        null,
        addonOptions,
        currentAddon
    );

    if (newAddon == null || newAddon.equals(currentAddon)) return;

    double oldAddonPrice = getAddonPrice(currentAddon);
    double newAddonPrice = getAddonPrice(newAddon);
    double basePrice = currentUnitPrice - oldAddonPrice;
    double newUnitPrice = basePrice + newAddonPrice;
    double newSubtotal = newUnitPrice * qty;

    subtotal -= currentUnitPrice * qty;
    subtotal += newSubtotal;
    if (subtotal < 0) subtotal = 0;

    orderModel.setValueAt(newAddon, selectedRow, 2);
    orderModel.setValueAt(newUnitPrice, selectedRow, 4);
    orderModel.setValueAt(newSubtotal, selectedRow, 5);

    updateReceiptDisplay();
    JOptionPane.showMessageDialog(this, "✅ Addon changed to: " + newAddon);
    }//GEN-LAST:event_btnChangeAddonActionPerformed

    private void btnClearAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearAllActionPerformed
        // TODO add your handling code here:
        if (orderModel.getRowCount() == 0) {
        JOptionPane.showMessageDialog(this, "⚠️ No items in order.");
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(this,
        "🗑️ Clear ALL items from order?",
        "Clear Order", JOptionPane.YES_NO_OPTION);

    if (confirm == JOptionPane.YES_OPTION) {
        orderModel.setRowCount(0);
        subtotal = 0;
        cashInput = "";
        itemQtyMap.clear();
        itemPriceMap.clear();
        productIdMap.clear();
        itemAddonsMap.clear();
        updateReceiptDisplay();
    }
    }//GEN-LAST:event_btnClearAllActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(POS.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(POS.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(POS.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(POS.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new POS().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn0;
    private javax.swing.JButton btn1;
    private javax.swing.JButton btn2;
    private javax.swing.JButton btn3;
    private javax.swing.JButton btn4;
    private javax.swing.JButton btn5;
    private javax.swing.JButton btn6;
    private javax.swing.JButton btn7;
    private javax.swing.JButton btn8;
    private javax.swing.JButton btn9;
    private javax.swing.JButton btnAC;
    private javax.swing.JButton btnAddAddons;
    private javax.swing.JButton btnCancelItem;
    private javax.swing.JButton btnChangeAddon;
    private javax.swing.JButton btnClearAll;
    private javax.swing.JButton btnEditQty;
    private javax.swing.JButton btnErase;
    private javax.swing.JButton btnLogOut;
    private javax.swing.JButton btnPAY;
    private javax.swing.JButton btnReports;
    private javax.swing.JComboBox<String> cmbAddons;
    private javax.swing.JComboBox<String> cmbOrderType;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTable jTableAddons;
    private javax.swing.JTable jTableCart;
    private javax.swing.JTable jTableOrder;
    private javax.swing.JTable jTableProducts;
    private javax.swing.JTextField txtCustomerName;
    private javax.swing.JTextArea txtDisplayPOS;
    private javax.swing.JTextField txtSearch;
    private javax.swing.JTextField txtTableNumber;
    // End of variables declaration//GEN-END:variables
}
