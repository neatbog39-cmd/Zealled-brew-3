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

        jPanel3 = new javax.swing.JPanel();
        btnLogOut = new javax.swing.JButton();
        lblWelcome = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        btnDashBoard = new javax.swing.JButton();
        btnProducts = new javax.swing.JButton();
        btnCategory = new javax.swing.JButton();
        btnAddons = new javax.swing.JButton();
        btnSize = new javax.swing.JButton();
        btnHistory = new javax.swing.JButton();
        btnUtilities = new javax.swing.JButton();
        btnInventory = new javax.swing.JButton();
        btnPOS = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        Refreshbtn = new javax.swing.JButton();
        btnSettings = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        lblCriticalLowCount = new javax.swing.JLabel();
        lblLowStockCount = new javax.swing.JLabel();
        lblTotalProducts = new javax.swing.JLabel();
        lblOutOfStockCount = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableInventory = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(40, 40, 40));
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel3.setBackground(new java.awt.Color(18, 20, 23));

        btnLogOut.setBackground(new java.awt.Color(18, 20, 23));
        btnLogOut.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnLogOut.setForeground(new java.awt.Color(197, 160, 114));
        btnLogOut.setText("Log Out");
        btnLogOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogOutActionPerformed(evt);
            }
        });

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/admin1.1.png"))); // NOI18N

        btnDashBoard.setBackground(new java.awt.Color(18, 20, 23));
        btnDashBoard.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnDashBoard.setForeground(new java.awt.Color(197, 160, 114));
        btnDashBoard.setText("Dashboard");
        btnDashBoard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDashBoardActionPerformed(evt);
            }
        });

        btnProducts.setBackground(new java.awt.Color(18, 20, 23));
        btnProducts.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnProducts.setForeground(new java.awt.Color(197, 160, 114));
        btnProducts.setText("Products");
        btnProducts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProductsActionPerformed(evt);
            }
        });

        btnCategory.setBackground(new java.awt.Color(18, 20, 23));
        btnCategory.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnCategory.setForeground(new java.awt.Color(197, 160, 114));
        btnCategory.setText("Category");
        btnCategory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCategoryActionPerformed(evt);
            }
        });

        btnAddons.setBackground(new java.awt.Color(18, 20, 23));
        btnAddons.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnAddons.setForeground(new java.awt.Color(197, 160, 114));
        btnAddons.setText("Addons");
        btnAddons.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddonsActionPerformed(evt);
            }
        });

        btnSize.setBackground(new java.awt.Color(18, 20, 23));
        btnSize.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnSize.setForeground(new java.awt.Color(197, 160, 114));
        btnSize.setText("Size");
        btnSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSizeActionPerformed(evt);
            }
        });

        btnHistory.setBackground(new java.awt.Color(18, 20, 23));
        btnHistory.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnHistory.setForeground(new java.awt.Color(197, 160, 114));
        btnHistory.setText("History");
        btnHistory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHistoryActionPerformed(evt);
            }
        });

        btnUtilities.setBackground(new java.awt.Color(18, 20, 23));
        btnUtilities.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnUtilities.setForeground(new java.awt.Color(197, 160, 114));
        btnUtilities.setText("Utilities");
        btnUtilities.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUtilitiesActionPerformed(evt);
            }
        });

        btnInventory.setBackground(new java.awt.Color(18, 20, 23));
        btnInventory.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnInventory.setForeground(new java.awt.Color(197, 160, 114));
        btnInventory.setText("Inventory");
        btnInventory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnInventoryActionPerformed(evt);
            }
        });

        btnPOS.setBackground(new java.awt.Color(18, 20, 23));
        btnPOS.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnPOS.setForeground(new java.awt.Color(197, 160, 114));
        btnPOS.setText("POS");
        btnPOS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPOSActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(lblWelcome, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btnLogOut, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnDashBoard, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
                                .addComponent(btnProducts, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnCategory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnAddons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnSize, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnHistory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnUtilities, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnInventory, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnPOS, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGap(17, 17, 17))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(lblWelcome, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                .addGap(704, 704, 704))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(58, 58, 58)
                .addComponent(btnDashBoard, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnProducts)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnCategory)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnAddons)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSize)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnHistory)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnUtilities)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnInventory)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnPOS)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnLogOut)
                .addGap(26, 26, 26))
        );

        getContentPane().add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        jPanel1.setBackground(new java.awt.Color(18, 20, 23));
        jPanel1.setForeground(new java.awt.Color(51, 51, 51));

        jPanel4.setBackground(new java.awt.Color(0, 0, 0));

        Refreshbtn.setBackground(new java.awt.Color(18, 20, 23));
        Refreshbtn.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        Refreshbtn.setForeground(new java.awt.Color(197, 160, 114));
        Refreshbtn.setText("Refresh");
        Refreshbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RefreshbtnActionPerformed(evt);
            }
        });

        btnSettings.setBackground(new java.awt.Color(18, 20, 23));
        btnSettings.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnSettings.setForeground(new java.awt.Color(197, 160, 114));
        btnSettings.setText("Settings");
        btnSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSettingsActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Serif", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(197, 160, 114));
        jLabel1.setText("INVENTORY");

        lblCriticalLowCount.setFont(new java.awt.Font("Serif", 1, 24)); // NOI18N
        lblCriticalLowCount.setForeground(new java.awt.Color(197, 160, 114));
        lblCriticalLowCount.setText("Critcal Low:");

        lblLowStockCount.setFont(new java.awt.Font("Serif", 1, 24)); // NOI18N
        lblLowStockCount.setForeground(new java.awt.Color(197, 160, 114));
        lblLowStockCount.setText("Low Stock:");

        lblTotalProducts.setFont(new java.awt.Font("Serif", 1, 24)); // NOI18N
        lblTotalProducts.setForeground(new java.awt.Color(197, 160, 114));
        lblTotalProducts.setText("Total Products:");

        lblOutOfStockCount.setFont(new java.awt.Font("Serif", 1, 24)); // NOI18N
        lblOutOfStockCount.setForeground(new java.awt.Color(197, 160, 114));
        lblOutOfStockCount.setText("Out of Stock:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblCriticalLowCount, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTotalProducts, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblOutOfStockCount, javax.swing.GroupLayout.DEFAULT_SIZE, 222, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(lblLowStockCount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTotalProducts)
                    .addComponent(lblLowStockCount))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblOutOfStockCount)
                    .addComponent(lblCriticalLowCount))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        jTableInventory.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
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

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(Refreshbtn, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnSettings, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1352, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(17, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(Refreshbtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnSettings)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 663, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(177, 177, 177))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(45, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(846, 846, 846))
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 0, 1420, 840));

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

    private void btnLogOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogOutActionPerformed
        // TODO add your handling code here:
        Login h = new Login();
        h.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnLogOutActionPerformed

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
    private javax.swing.JButton btnLogOut;
    private javax.swing.JButton btnPOS;
    private javax.swing.JButton btnProducts;
    private javax.swing.JButton btnSettings;
    private javax.swing.JButton btnSize;
    private javax.swing.JButton btnUtilities;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableInventory;
    private javax.swing.JLabel lblCriticalLowCount;
    private javax.swing.JLabel lblLowStockCount;
    private javax.swing.JLabel lblOutOfStockCount;
    private javax.swing.JLabel lblTotalProducts;
    private javax.swing.JLabel lblWelcome;
    // End of variables declaration//GEN-END:variables
}
