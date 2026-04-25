/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pkgfinal.zealled.brew;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane; 
import java.sql.SQLException;  

/**
 *
 * @author ASUS
 */
public class Inventory extends javax.swing.JFrame {

    /**
     * Creates new form Inventory
     *
     */
    public Inventory() {
        initComponents();
        setupInventoryTable();
        loadInventoryData();
        loadProductStats();
        loadInventoryData();
    }
    public void refreshInventoryData() {
    loadProductStats();
    loadInventoryData();
}
     // ========================= TABLE SETUP =========================
    private void setupInventoryTable() {
    String[] columns = {"Product Name", "Category", "Size", "Quantity", "Status"};  // Added Status
    DefaultTableModel model = new DefaultTableModel(columns, 0);
    jTableInventory.setModel(model);
    jTableInventory.setDefaultEditor(Object.class, null);
    jTableInventory.getTableHeader().setReorderingAllowed(false);
}

    // ========================= PRODUCT STATS =========================
    private void loadProductStats() {
    StockThresholds thresholds = getStockThresholds();
    
    try (Connection con = ConnectorXampp.connect()) {
        // TOTAL PRODUCTS
        String sqlTotal = "SELECT COUNT(*) as total FROM products";
        try (PreparedStatement pst = con.prepareStatement(sqlTotal);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                lblTotalProducts.setText("Total Products: " + rs.getInt("total"));
            }
        } catch (Exception e) {
            System.err.println("Error loading total products: " + e.getMessage());
        }

        // CRITICAL LOW
        String sqlCritical = String.format("""
            SELECT COUNT(*) as critical_count 
            FROM products 
            WHERE Quantity > 0 AND Quantity >= %d AND Quantity <= %d
        """, thresholds.criticalLowMin, thresholds.criticalLowMax);
        
        try (PreparedStatement pst = con.prepareStatement(sqlCritical);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                lblCriticalLowCount.setText("Critical Low: " + rs.getInt("critical_count"));
            }
        } catch (Exception e) {
            System.err.println("Error loading critical low: " + e.getMessage());
        }

        // LOW STOCK
        String sqlLow = String.format("""
            SELECT COUNT(*) as low_count 
            FROM products 
            WHERE Quantity >= %d AND Quantity <= %d
        """, thresholds.lowStockMin, thresholds.lowStockMax);
        
        try (PreparedStatement pst = con.prepareStatement(sqlLow);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                lblLowStockCount.setText("Low Stock: " + rs.getInt("low_count"));
            }
        } catch (Exception e) {
            System.err.println("Error loading low stock: " + e.getMessage());
        }

        // OUT OF STOCK
        String sqlOut = "SELECT COUNT(*) as out_count FROM products WHERE Quantity = 0";
        try (PreparedStatement pst = con.prepareStatement(sqlOut);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                lblOutOfStockCount.setText("Out of Stock: " + rs.getInt("out_count"));
            }
        } catch (Exception e) {
            System.err.println("Error loading out of stock: " + e.getMessage());
        }
        
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error loading product stats: " + e.getMessage());
    }
}

    // ========================= INVENTORY TABLE =========================
    private void loadInventoryData() {
    try (Connection con = ConnectorXampp.connect()) {
        StockThresholds thresholds = getStockThresholds();
        
        String sql = String.format("""
            SELECT Name, Category, Size, Quantity
            FROM products
            ORDER BY 
                CASE 
                    WHEN Quantity = 0 THEN 1
                    WHEN Quantity >= %d AND Quantity <= %d THEN 2
                    WHEN Quantity >= %d AND Quantity <= %d THEN 3
                    ELSE 4
                END, Quantity ASC
        """, thresholds.criticalLowMin, thresholds.criticalLowMax, 
           thresholds.lowStockMin, thresholds.lowStockMax);

        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            DefaultTableModel model = (DefaultTableModel) jTableInventory.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                int quantity = rs.getInt("Quantity");
                String status = getStatus(quantity);
                model.addRow(new Object[]{
                    rs.getString("Name"),
                    rs.getString("Category"),
                    rs.getString("Size"),
                    quantity,
                    status
                });
            }
        }
    } catch (Exception e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Error loading inventory: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    private String getStatus(int quantity) {
    StockThresholds thresholds = getStockThresholds();
    
    if (quantity == 0) return "OUT OF STOCK";
    else if (quantity >= thresholds.criticalLowMin && quantity <= thresholds.criticalLowMax) 
        return "CRITICAL LOW";
    else if (quantity >= thresholds.lowStockMin && quantity <= thresholds.lowStockMax) 
        return "LOW STOCK";
    else return "IN STOCK";
}
    
    private StockThresholds getStockThresholds() {
    StockThresholds thresholds = new StockThresholds();
    try (Connection con = ConnectorXampp.connect()) {
        String sql = "SELECT critical_low_min, critical_low_max, low_stock_min, low_stock_max FROM stock_settings LIMIT 1";
        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                thresholds.criticalLowMin = rs.getInt("critical_low_min");
                thresholds.criticalLowMax = rs.getInt("critical_low_max");
                thresholds.lowStockMin = rs.getInt("low_stock_min");
                thresholds.lowStockMax = rs.getInt("low_stock_max");
            } else {
                // Defaults
                thresholds.criticalLowMin = 1;
                thresholds.criticalLowMax = 5;
                thresholds.lowStockMin = 6;
                thresholds.lowStockMax = 10;
            }
        }
    } catch (SQLException e) {
        // Use defaults on error
        thresholds.criticalLowMin = 1;
        thresholds.criticalLowMax = 5;
        thresholds.lowStockMin = 6;
        thresholds.lowStockMax = 10;
    }
    return thresholds;
}

    // Helper class for thresholds
    private static class StockThresholds {
    int criticalLowMin = 1;
    int criticalLowMax = 5;
    int lowStockMin = 6;
    int lowStockMax = 10;
}
    


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTableInventory = new javax.swing.JTable();
        Refreshbtn = new javax.swing.JButton();
        lblTotalProducts = new javax.swing.JLabel();
        lblCriticalLowCount = new javax.swing.JLabel();
        lblLowStockCount = new javax.swing.JLabel();
        lblOutOfStockCount = new javax.swing.JLabel();
        btnDashBoard = new javax.swing.JButton();
        btnProducts = new javax.swing.JButton();
        btnCategory = new javax.swing.JButton();
        btnAddons = new javax.swing.JButton();
        btnSize = new javax.swing.JButton();
        btnHistory = new javax.swing.JButton();
        btnUtilities = new javax.swing.JButton();
        btnSettings = new javax.swing.JButton();
        btnInventory = new javax.swing.JButton();
        btnPOS = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(40, 40, 40));
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jTableInventory.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Product Name", "Category", "Size", "Quantity"
            }
        ));
        jScrollPane1.setViewportView(jTableInventory);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(375, 226, 710, 250));

        Refreshbtn.setText("Refresh");
        Refreshbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RefreshbtnActionPerformed(evt);
            }
        });
        getContentPane().add(Refreshbtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(931, 183, -1, -1));

        lblTotalProducts.setText("Total Products:");
        getContentPane().add(lblTotalProducts, new org.netbeans.lib.awtextra.AbsoluteConstraints(386, 161, 106, -1));

        lblCriticalLowCount.setText("Critcal Low:");
        getContentPane().add(lblCriticalLowCount, new org.netbeans.lib.awtextra.AbsoluteConstraints(498, 161, 109, -1));

        lblLowStockCount.setText("Low Stock:");
        getContentPane().add(lblLowStockCount, new org.netbeans.lib.awtextra.AbsoluteConstraints(386, 186, 106, -1));

        lblOutOfStockCount.setText("Out of Stock:");
        getContentPane().add(lblOutOfStockCount, new org.netbeans.lib.awtextra.AbsoluteConstraints(498, 186, 175, -1));

        btnDashBoard.setBackground(new java.awt.Color(18, 20, 23));
        btnDashBoard.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnDashBoard.setForeground(new java.awt.Color(197, 160, 114));
        btnDashBoard.setText("Dashboard");
        btnDashBoard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDashBoardActionPerformed(evt);
            }
        });
        getContentPane().add(btnDashBoard, new org.netbeans.lib.awtextra.AbsoluteConstraints(16, 29, 280, -1));

        btnProducts.setBackground(new java.awt.Color(18, 20, 23));
        btnProducts.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnProducts.setForeground(new java.awt.Color(197, 160, 114));
        btnProducts.setText("Products");
        btnProducts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProductsActionPerformed(evt);
            }
        });
        getContentPane().add(btnProducts, new org.netbeans.lib.awtextra.AbsoluteConstraints(63, 112, 230, -1));

        btnCategory.setBackground(new java.awt.Color(18, 20, 23));
        btnCategory.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnCategory.setForeground(new java.awt.Color(197, 160, 114));
        btnCategory.setText("Category");
        btnCategory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCategoryActionPerformed(evt);
            }
        });
        getContentPane().add(btnCategory, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 190, 260, -1));

        btnAddons.setBackground(new java.awt.Color(18, 20, 23));
        btnAddons.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnAddons.setForeground(new java.awt.Color(197, 160, 114));
        btnAddons.setText("Addons");
        btnAddons.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddonsActionPerformed(evt);
            }
        });
        getContentPane().add(btnAddons, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 240, 123, -1));

        btnSize.setBackground(new java.awt.Color(18, 20, 23));
        btnSize.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnSize.setForeground(new java.awt.Color(197, 160, 114));
        btnSize.setText("Size");
        btnSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSizeActionPerformed(evt);
            }
        });
        getContentPane().add(btnSize, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 300, 119, -1));

        btnHistory.setBackground(new java.awt.Color(18, 20, 23));
        btnHistory.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnHistory.setForeground(new java.awt.Color(197, 160, 114));
        btnHistory.setText("History");
        btnHistory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHistoryActionPerformed(evt);
            }
        });
        getContentPane().add(btnHistory, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 380, -1, -1));

        btnUtilities.setBackground(new java.awt.Color(18, 20, 23));
        btnUtilities.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnUtilities.setForeground(new java.awt.Color(197, 160, 114));
        btnUtilities.setText("Utilities");
        btnUtilities.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUtilitiesActionPerformed(evt);
            }
        });
        getContentPane().add(btnUtilities, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 430, -1, -1));

        btnSettings.setText("Settings");
        btnSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSettingsActionPerformed(evt);
            }
        });
        getContentPane().add(btnSettings, new org.netbeans.lib.awtextra.AbsoluteConstraints(950, 110, -1, -1));

        btnInventory.setText("Inventory");
        btnInventory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnInventoryActionPerformed(evt);
            }
        });
        getContentPane().add(btnInventory, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 540, -1, -1));

        btnPOS.setText("POS");
        btnPOS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPOSActionPerformed(evt);
            }
        });
        getContentPane().add(btnPOS, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 490, -1, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void RefreshbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RefreshbtnActionPerformed
        // TODO add your handling code here:
        loadInventoryData();
        loadProductStats();
    }//GEN-LAST:event_RefreshbtnActionPerformed

    private void btnDashBoardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDashBoardActionPerformed
        // TODO add your handling code here:
        DashBoard a = new DashBoard();
        a.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnDashBoardActionPerformed

    private void btnProductsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnProductsActionPerformed
        // TODO add your handling code here:
        Products c = new Products();
        c.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnProductsActionPerformed

    private void btnCategoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCategoryActionPerformed
        // TODO add your handling code here:
        Category d = new Category();
        d.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnCategoryActionPerformed

    private void btnAddonsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddonsActionPerformed
        // TODO add your handling code here:
        Addons e = new Addons();
        e.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnAddonsActionPerformed

    private void btnSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSizeActionPerformed
        // TODO add your handling code here:
        Size b = new Size();
        b.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnSizeActionPerformed

    private void btnHistoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHistoryActionPerformed
        // TODO add your handling code here:
        History f = new History();
        f.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnHistoryActionPerformed

    private void btnUtilitiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUtilitiesActionPerformed
        // TODO add your handling code here:
        Utilities g = new Utilities();
        g.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnUtilitiesActionPerformed

    private void btnSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSettingsActionPerformed
        // TODO add your handling code here:
        Settings i = new Settings();
        i.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnSettingsActionPerformed

    private void btnInventoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInventoryActionPerformed
        // TODO add your handling code here:
        Inventory z = new Inventory();
        z.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnInventoryActionPerformed

    private void btnPOSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPOSActionPerformed
        // TODO add your handling code here:
        AdminPOS q = new AdminPOS();
        q.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnPOSActionPerformed

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
            java.util.logging.Logger.getLogger(Inventory.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Inventory.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Inventory.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Inventory.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Inventory().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Refreshbtn;
    private javax.swing.JButton btnAddons;
    private javax.swing.JButton btnCategory;
    private javax.swing.JButton btnDashBoard;
    private javax.swing.JButton btnHistory;
    private javax.swing.JButton btnInventory;
    private javax.swing.JButton btnPOS;
    private javax.swing.JButton btnProducts;
    private javax.swing.JButton btnSettings;
    private javax.swing.JButton btnSize;
    private javax.swing.JButton btnUtilities;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableInventory;
    private javax.swing.JLabel lblCriticalLowCount;
    private javax.swing.JLabel lblLowStockCount;
    private javax.swing.JLabel lblOutOfStockCount;
    private javax.swing.JLabel lblTotalProducts;
    // End of variables declaration//GEN-END:variables
}
