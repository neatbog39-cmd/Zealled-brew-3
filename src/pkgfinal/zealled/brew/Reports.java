/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pkgfinal.zealled.brew;
import javax.swing.JOptionPane;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.toedter.calendar.JDateChooser;
/**
 *
 * @author ASUS
 */
public class Reports extends javax.swing.JFrame {
    private javax.swing.table.DefaultTableModel historyModel;
    /**
     * Creates new form DashBoard
     */
    
    // ========================= CONSTRUCTOR & FIELDS =========================
    public Reports() {
        initComponents();
        setupHistoryTable();
        loadOrders();
    }
    
    // ========================= TABLE SETUP =========================
    private void setupHistoryTable() {
    String[] columns = {
        "OrderID", "Date", "Type", "Table #", "Customer", "Products", 
        "Vatable", "VAT(12%)", "Total", "Cash", "Change", "Cashier"
    };

    historyModel = new javax.swing.table.DefaultTableModel(columns, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    jTableHistory.setModel(historyModel);

    javax.swing.table.DefaultTableCellRenderer rightRenderer =
        new javax.swing.table.DefaultTableCellRenderer();
    rightRenderer.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);

    for (int i = 6; i <= 10; i++) { // ✅ shifted by 2 for new columns
        jTableHistory.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
    }

    jTableHistory.getColumnModel().getColumn(0).setPreferredWidth(60);   // OrderID
    jTableHistory.getColumnModel().getColumn(1).setPreferredWidth(120);  // Date
    jTableHistory.getColumnModel().getColumn(2).setPreferredWidth(80);   // Type
    jTableHistory.getColumnModel().getColumn(3).setPreferredWidth(80);   // Table #
    jTableHistory.getColumnModel().getColumn(4).setPreferredWidth(120);  // Customer
    jTableHistory.getColumnModel().getColumn(5).setPreferredWidth(200);  // Products
    jTableHistory.getColumnModel().getColumn(9).setPreferredWidth(90);   // Cash
    jTableHistory.getColumnModel().getColumn(10).setPreferredWidth(90);  // Change
    jTableHistory.getColumnModel().getColumn(11).setPreferredWidth(120); // Cashier
}
    
    // Add this method after setupHistoryTable()
    private void addDailyTotalsRow(Date selectedDate) {
    if (historyModel.getRowCount() == 0) return;

    double totalVatable = 0, totalVAT = 0, totalAmount = 0, totalCash = 0, totalChange = 0;

    for (int i = 0; i < historyModel.getRowCount(); i++) {
        totalVatable += parseAmount(historyModel.getValueAt(i, 6).toString());  // ✅ shifted
        totalVAT     += parseAmount(historyModel.getValueAt(i, 7).toString());
        totalAmount  += parseAmount(historyModel.getValueAt(i, 8).toString());
        totalCash    += parseAmount(historyModel.getValueAt(i, 9).toString());
        totalChange  += parseAmount(historyModel.getValueAt(i, 10).toString());
    }

    Object[] totalRow = {
        "🔢TOTAL", "", "", "", "", "",
        String.format("₱%.2f", totalVatable),
        String.format("₱%.2f", totalVAT),
        String.format("₱%.2f", totalAmount),
        String.format("₱%.2f", totalCash),
        String.format("₱%.2f", totalChange),
        String.format("** %d Orders **", historyModel.getRowCount())
    };

    historyModel.addRow(totalRow);
}

    // Helper to parse amount from formatted string
    private double parseAmount(String amountStr) {
    try {
        return Double.parseDouble(amountStr.replaceAll("[₱,]", ""));
    } catch (Exception e) {
        return 0;
    }
}
    
    

    // ========================= LOAD ORDERS =========================
    private void loadOrders() {
    String searchTerm = txtSearch.getText().trim().toLowerCase();
    Date selectedDate = dateChooser.getDate();
    
    historyModel.setRowCount(0); // Clear table first
    
    if (!searchTerm.isEmpty() || selectedDate != null) {
        filteredSearch(searchTerm, selectedDate);
    } else {
        loadAllOrders();
    }
}

    // ========================= FILTERED SEARCH =========================
    private void filteredSearch(String searchTerm, Date selectedDate) {
    historyModel.setRowCount(0);

    StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT o.OrderID, o.OrderDate, o.OrderType,
               COALESCE(o.TableNumber, '—') AS TableNumber,
               COALESCE(o.CustomerName, '—') AS CustomerName,
               GROUP_CONCAT(p.Name SEPARATOR ', ') AS Products,
               SUM(od.Subtotal) AS OrderSubtotal,
               o.TotalAmount, o.`Cash`, o.`Change`
        FROM orders o 
        JOIN order_details od ON o.OrderID = od.OrderID 
        JOIN products p ON od.ProductID = p.ProductID 
        LEFT JOIN users u ON o.UserID = u.id
        WHERE 1=1
        """);

    try (Connection con = ConnectorXampp.connect()) {

        if (selectedDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            sql.append(" AND DATE(o.OrderDate) = ?");
        }

        if (!searchTerm.isEmpty()) {
            sql.append(" AND (o.OrderID LIKE ? ");
            sql.append("OR o.OrderDate LIKE ? ");
            sql.append("OR o.OrderType LIKE ? ");
            sql.append("OR o.TableNumber LIKE ? ");    // ✅ NEW
            sql.append("OR o.CustomerName LIKE ? ");   // ✅ NEW
            sql.append("OR p.Name LIKE ? ");
            sql.append("OR u.full_name LIKE ?)");
        }

        sql.append(" GROUP BY o.OrderID ORDER BY o.OrderID DESC LIMIT 100");

        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int paramIndex = 1;

            if (selectedDate != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                ps.setString(paramIndex++, dateFormat.format(selectedDate));
            }

            if (!searchTerm.isEmpty()) {
                String searchPattern = "%" + searchTerm + "%";
                for (int i = 0; i < 7; i++) { // ✅ 7 params now instead of 5
                    ps.setString(paramIndex++, searchPattern);
                }
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                double subtotal = rs.getDouble("OrderSubtotal");
                double vatable = subtotal / 1.12;
                double vat = vatable * 0.12;
                double total = rs.getDouble("TotalAmount");
                double cash = rs.getDouble("Cash");
                double change = rs.getDouble("Change");

                String cashierName = getCashierNameForOrder(rs.getInt("OrderID"));

                historyModel.addRow(new Object[]{
                    rs.getInt("OrderID"),
                    rs.getString("OrderDate").substring(0, 16),
                    rs.getString("OrderType"),
                    rs.getString("TableNumber"),   // ✅ NEW
                    rs.getString("CustomerName"),  // ✅ NEW
                    rs.getString("Products"),
                    String.format("₱%.2f", vatable),
                    String.format("₱%.2f", vat),
                    String.format("₱%.2f", total),
                    String.format("₱%.2f", cash),
                    String.format("₱%.2f", change),
                    cashierName
                });
            }
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "❌ Filter Error: " + e.getMessage());
        e.printStackTrace();
    }

    addDailyTotalsRow(selectedDate);
}
    
    // ========================= LOAD ALL ORDERS =========================
    private void loadAllOrders() {
    historyModel.setRowCount(0);

    String sql = """
        SELECT o.OrderID, o.OrderDate, o.OrderType, 
               COALESCE(o.TableNumber, '—') AS TableNumber,
               COALESCE(o.CustomerName, '—') AS CustomerName,
               COALESCE(GROUP_CONCAT(COALESCE(p.Name, 'DELETED') SEPARATOR ', '), 'EMPTY') AS Products,
               COALESCE(SUM(od.Subtotal), 0) AS OrderSubtotal,
               o.TotalAmount, o.`Cash`, o.`Change`
        FROM orders o 
        LEFT JOIN order_details od ON o.OrderID = od.OrderID 
        LEFT JOIN products p ON od.ProductID = p.ProductID 
        GROUP BY o.OrderID 
        ORDER BY o.OrderID DESC LIMIT 100
        """;

    try (Connection con = ConnectorXampp.connect();
         Statement st = con.createStatement();
         ResultSet rs = st.executeQuery(sql)) {

        while (rs.next()) {
            double subtotal = rs.getDouble("OrderSubtotal");
            double vatable = subtotal / 1.12;
            double vat = vatable * 0.12;
            double total = rs.getDouble("TotalAmount");
            double cash = rs.getDouble("Cash");
            double change = rs.getDouble("Change");

            String cashierName = getCashierNameForOrder(rs.getInt("OrderID"));

            historyModel.addRow(new Object[]{
                rs.getInt("OrderID"),
                rs.getString("OrderDate").substring(0, 16),
                rs.getString("OrderType"),
                rs.getString("TableNumber"),   // ✅ NEW
                rs.getString("CustomerName"),  // ✅ NEW
                rs.getString("Products"),
                String.format("₱%.2f", vatable),
                String.format("₱%.2f", vat),
                String.format("₱%.2f", total),
                String.format("₱%.2f", cash),
                String.format("₱%.2f", change),
                cashierName
            });
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "❌ Database Error: " + e.getMessage());
        e.printStackTrace();
    }
    addDailyTotalsRow(null);
}

    // ========================= RECEIPT DETAILS =========================
    private void showOrderDetails(int orderId){
    StringBuilder receipt = new StringBuilder();

    try(Connection con = ConnectorXampp.connect()){
        
        // HEADER
        String headerSql = """
            SELECT o.OrderDate, o.OrderType, o.TotalAmount, o.Cash, o.`Change`,
           o.TableNumber, o.CustomerName,
           u.full_name AS CashierName 
            FROM orders o 
            LEFT JOIN users u ON o.UserID = u.id 
            WHERE o.OrderID = ?
            """;
        
        try(PreparedStatement headerPs = con.prepareStatement(headerSql)){
            headerPs.setInt(1, orderId);
            ResultSet headerRs = headerPs.executeQuery();
            
            if (headerRs.next()) {
                String orderDateTime = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a")
                    .format(headerRs.getTimestamp("OrderDate"));
                String orderType = headerRs.getString("OrderType");
                String cashierName = headerRs.getString("CashierName");
                
                receipt.append("☕ ZEALLED BREWS ☕\n")
                        .append("============================\n")
                        .append("Cashier:  ").append(cashierName != null ? cashierName : "Cashier").append("\n")
                        .append("Date:     ").append(orderDateTime).append("\n")
                        .append("Type:     ").append(orderType).append("\n")
                        .append("Table #:  ").append(headerRs.getString("TableNumber") != null ? headerRs.getString("TableNumber") : "—").append("\n")  // ✅ NEW
                        .append("Customer: ").append(headerRs.getString("CustomerName") != null ? headerRs.getString("CustomerName") : "—").append("\n\n"); // ✅ NEW
            }
        }

        // ITEMS
        String itemsSql = """
            SELECT p.Name, p.Size, od.AddonName, od.Quantity, od.Subtotal, 
                   od.BasePrice, od.AddonPrice 
            FROM order_details od 
            JOIN products p ON od.ProductID = p.ProductID 
            WHERE od.OrderID = ? ORDER BY od.Subtotal DESC
            """;
        
        double subtotal = 0;
        int totalItems = 0;
        
        try(PreparedStatement itemsPs = con.prepareStatement(itemsSql)){
            itemsPs.setInt(1, orderId);
            ResultSet itemsRs = itemsPs.executeQuery();
            
            while(itemsRs.next()){
                String name = itemsRs.getString("Name");
                String size = itemsRs.getString("Size");
                String addon = itemsRs.getString("AddonName");
                int qty = itemsRs.getInt("Quantity");
                double itemSubtotal = itemsRs.getDouble("Subtotal");
                
                subtotal += itemSubtotal;
                totalItems += qty;
                
                String displayName = name + " (" + size + ")" + 
                    (addon.equals("None") ? "" : " +" + addon);
                String itemLine = String.format("%-25s x%-2d ₱%.2f", displayName, qty, itemSubtotal);
                receipt.append(itemLine).append("\n");
            }
        }

        // VAT
        double vatable = subtotal / 1.12;
        double vat = vatable * 0.12;
        double total = vatable + vat;
        
        
        String paymentSql = "SELECT `Cash`, `Change` FROM orders WHERE OrderID = ?";
        double cashAmount = 0, changeAmount = 0;
        
        try(PreparedStatement paymentPs = con.prepareStatement(paymentSql)){
            paymentPs.setInt(1, orderId);
            ResultSet paymentRs = paymentPs.executeQuery();
            if(paymentRs.next()){
                cashAmount = paymentRs.getDouble("Cash");
                changeAmount = paymentRs.getDouble("Change");
            }
        }


        receipt.append("\n----------------------------\n")
               .append("Total Items: ").append(totalItems).append("\n")
               .append("Vatable:    ₱").append(String.format("%.2f", vatable)).append("\n")
               .append("VAT(12%):   ₱").append(String.format("%.2f", vat)).append("\n")
               .append("TOTAL:      ₱").append(String.format("%.2f", total)).append("\n")
               .append("💵 Cash:     ₱").append(String.format("%.2f", cashAmount)).append("\n")
               .append("🔄 Change:   ₱").append(String.format("%.2f", changeAmount)).append("\n")
               .append("============================\n")
               .append("✅ PAID! THANK YOU! ☕");

   
        javax.swing.JTextArea textArea = new javax.swing.JTextArea(receipt.toString());
        textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 14));
        textArea.setEditable(false);
        textArea.setMargin(new java.awt.Insets(10, 10, 10, 10));
        
        JOptionPane.showMessageDialog(
            this, 
            new javax.swing.JScrollPane(textArea),
            "📄 Receipt #"+orderId, 
            JOptionPane.INFORMATION_MESSAGE
        );

    }catch(Exception e){
        JOptionPane.showMessageDialog(this, "❌ Error loading receipt: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    // ========================= HELPER METHODS =========================
    private String getCashierNameForOrder(int orderId) {
    String sql = """
        SELECT u.full_name 
        FROM orders o 
        JOIN users u ON o.UserID = u.id 
        WHERE o.OrderID = ? AND u.role = 'Cashier'
        """;
    
    try (Connection con = ConnectorXampp.connect();
         PreparedStatement ps = con.prepareStatement(sql)) {
        
        ps.setInt(1, orderId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String fullName = rs.getString("full_name");
            return fullName != null ? fullName : "Cashier";
        }
    } catch (Exception e) {
        System.out.println("Cashier lookup failed for Order #" + orderId + ": " + e.getMessage());
    }
    return "Cashier";
}
    
   
  

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        btnLoad = new javax.swing.JButton();
        btnViewDetails = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        dateChooser = new com.toedter.calendar.JDateChooser();
        btnClearFilters = new javax.swing.JButton();
        btnclose = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableHistory = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));

        jPanel2.setBackground(new java.awt.Color(40, 40, 40));
        jPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel1.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(197, 160, 114));
        jLabel1.setText("Search:");

        txtSearch.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        btnLoad.setBackground(new java.awt.Color(40, 40, 40));
        btnLoad.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnLoad.setForeground(new java.awt.Color(197, 160, 114));
        btnLoad.setText("Load");
        btnLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoadActionPerformed(evt);
            }
        });

        btnViewDetails.setBackground(new java.awt.Color(40, 40, 40));
        btnViewDetails.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnViewDetails.setForeground(new java.awt.Color(197, 160, 114));
        btnViewDetails.setText("View Details");
        btnViewDetails.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewDetailsActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(197, 160, 114));
        jLabel2.setText("HISTORY");

        dateChooser.setBackground(new java.awt.Color(40, 40, 40));
        dateChooser.setForeground(new java.awt.Color(197, 160, 114));
        dateChooser.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                dateChooserPropertyChange(evt);
            }
        });

        btnClearFilters.setBackground(new java.awt.Color(40, 40, 40));
        btnClearFilters.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnClearFilters.setForeground(new java.awt.Color(197, 160, 114));
        btnClearFilters.setText("Clear");
        btnClearFilters.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearFiltersActionPerformed(evt);
            }
        });

        btnclose.setBackground(new java.awt.Color(40, 40, 40));
        btnclose.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnclose.setForeground(new java.awt.Color(197, 160, 114));
        btnclose.setText("Close");
        btnclose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btncloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 334, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnLoad))
                    .addComponent(btnClearFilters, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(btnViewDetails)
                        .addGap(18, 18, 18)
                        .addComponent(btnclose)))
                .addContainerGap(817, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnLoad)
                    .addComponent(btnViewDetails)
                    .addComponent(btnclose))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dateChooser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnClearFilters))
                .addContainerGap(10, Short.MAX_VALUE))
        );

        jTableHistory.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        jTableHistory.setForeground(new java.awt.Color(0, 0, 0));
        jTableHistory.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "OrderID", "Date", "OrderDate", "OrderType", "Products", "TotalAmount"
            }
        ));
        jScrollPane1.setViewportView(jTableHistory);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 616, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(36, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1530, 820));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btncloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btncloseActionPerformed
        // TODO add your handling code here:
        POS a = new POS();
       a.setVisible(true);
       this.dispose();
    }//GEN-LAST:event_btncloseActionPerformed

    private void btnViewDetailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewDetailsActionPerformed
        // TODO add your handling code here:
        int row = jTableHistory.getSelectedRow();
    
    if(row == -1){
        JOptionPane.showMessageDialog(this, "📋 Select an order first!");
        return;
    }
    
    try {
        int orderId = (Integer) historyModel.getValueAt(row, 0);
        showOrderDetails(orderId);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error viewing details: " + e.getMessage());
        e.printStackTrace();
    }
    }//GEN-LAST:event_btnViewDetailsActionPerformed

    private void btnLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadActionPerformed
        // TODO add your handling code here:
         loadOrders();
    }//GEN-LAST:event_btnLoadActionPerformed

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
        // TODO add your handling code here:
    loadOrders();
    }//GEN-LAST:event_txtSearchKeyReleased

    private void dateChooserPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_dateChooserPropertyChange
        // TODO add your handling code here:
         if ("date".equals(evt.getPropertyName())) {
            loadOrders();  // Filter when date changes
        }
    }//GEN-LAST:event_dateChooserPropertyChange

    private void btnClearFiltersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearFiltersActionPerformed
        // TODO add your handling code here:
        txtSearch.setText("");
        dateChooser.setDate(null);
        loadOrders();  // Show all orders
    
    }//GEN-LAST:event_btnClearFiltersActionPerformed

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
            java.util.logging.Logger.getLogger(Reports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Reports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Reports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Reports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Reports().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClearFilters;
    private javax.swing.JButton btnLoad;
    private javax.swing.JButton btnViewDetails;
    private javax.swing.JButton btnclose;
    private com.toedter.calendar.JDateChooser dateChooser;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableHistory;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
