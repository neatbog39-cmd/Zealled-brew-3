/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pkgfinal.zealled.brew;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author ASUS
 */
public class Products extends javax.swing.JFrame {
    

    /**
     * Creates new form DashBoard
     */
    
    // ========================= CONSTRUCTOR & FIELDS =========================
    public Products() {
      initComponents();
    loadCategoryComboBoxes();
    
    loadSizeComboBoxes();
    
   
    setupTableModel();
    loadStockData();
    formatTableDisplay();
    
    
    setFieldsEnabled(false);
    BTNAdd.setEnabled(true);
    BtnEdit.setEnabled(false);
    BtnUpdate.setEnabled(false);
    BtnDelete.setEnabled(false);
    BtnReset.setEnabled(false);

    
    jTable1.getSelectionModel().addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting() && jTable1.getSelectedRow() != -1) {
            BtnEdit.setEnabled(true);
            BtnDelete.setEnabled(true);
            BTNAdd.setEnabled(false);
        } else {
            resetButtons();
        }
    });
    }
 
         
    private int editingProductId = -1;
 
    public static java.util.ArrayList<String> productsComboBoxes = new java.util.ArrayList<>();
    
    
    // ========================= TABLE & COMBOBOX SETUP =========================
    private void setupTableModel() {
    String[] columns = {"ID", "Name", "Category", "Size", "Selling Price", "Quantity"};
    DefaultTableModel model = new DefaultTableModel(columns, 0) {
        public boolean isCellEditable(int row, int col) { return false; }
    };
    jTable1.setModel(model);
}
    
    private void loadCategoryComboBoxes() {
    try {
        String sql = "SELECT category_name FROM category";
        try (java.sql.Connection con = ConnectorXampp.connect();
             java.sql.Statement st = con.createStatement();
             java.sql.ResultSet rs = st.executeQuery(sql)) {
            
            productsComboBoxes.clear();
            productsComboBoxes.add("All"); 
            
            while (rs.next()) {
                productsComboBoxes.add(rs.getString("category_name"));
            }
            
            
            cmbCategory.setModel(new javax.swing.DefaultComboBoxModel<>(
                productsComboBoxes.toArray(new String[0])));
            cbcat.setModel(new javax.swing.DefaultComboBoxModel<>(
                productsComboBoxes.toArray(new String[0])));
            
            sortComboBoxAlphabetically(cmbCategory);
            sortComboBoxAlphabetically(cbcat);
        }
    } catch (Exception e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Error loading categories: " + e.getMessage());
    }
}  
    
     private void loadSizeComboBoxes() {
    try {
        String sql = "SELECT size_name FROM size ORDER BY size_name";
        try (java.sql.Connection con = ConnectorXampp.connect();
             java.sql.Statement st = con.createStatement();
             java.sql.ResultSet rs = st.executeQuery(sql)) {
            
            java.util.ArrayList<String> sizes = new java.util.ArrayList<>();
            sizes.add("All"); 
            
            while (rs.next()) {
                sizes.add(rs.getString("size_name"));
            }
            
            
            cmbSize.setModel(new javax.swing.DefaultComboBoxModel<>(
                sizes.toArray(new String[0])));
            
            sortComboBoxAlphabetically(cmbSize);
        }
    } catch (Exception e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Error loading sizes: " + e.getMessage());
    }
}

    private void sortComboBoxAlphabetically(javax.swing.JComboBox<String> comboBox) {
    java.util.List<String> items = new java.util.ArrayList<>();
    for (int i = 0; i < comboBox.getItemCount(); i++) {
        items.add(comboBox.getItemAt(i));
    }
    
    
    String allOption = items.remove(0); 
    items.sort(String.CASE_INSENSITIVE_ORDER); 
    items.add(0, allOption); 
    
    comboBox.setModel(new javax.swing.DefaultComboBoxModel<>(items.toArray(new String[0])));
}

   
    
    

    
    // ========================= STOCK MANAGEMENT(POS) =========================
    // DEDUCT STOCK 
    public static boolean deductStock(int productId, int quantitySold) {
    String sql = "UPDATE products SET Quantity = Quantity - ? WHERE ProductID = ?";
    try (java.sql.Connection con = ConnectorXampp.connect();
         java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
        pst.setInt(1, quantitySold);
        pst.setInt(2, productId);
        int rows = pst.executeUpdate();
        System.out.println("✅ STOCK DEDUCTED: ProductID=" + productId + ", Qty=" + quantitySold + ", Rows Affected=" + rows);
        return rows > 0;
    } catch (Exception e) {
        System.out.println("❌ STOCK ERROR: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}

    // ADD STOCK 
    public static boolean addStock(int productId, int quantity) {
    String sql = "UPDATE products SET Quantity = Quantity + ? WHERE ProductID = ?";
    try (java.sql.Connection con = ConnectorXampp.connect();
         java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
        pst.setInt(1, quantity);
        pst.setInt(2, productId);
        return pst.executeUpdate() > 0;
    } catch (Exception e) {
        System.out.println("Stock add error: " + e.getMessage());
        return false;
    }
}

    // REFRESH ALL STOCK 
    public static void refreshAllStock() {
    System.out.println("📦 All stock refreshed!");
}
    
    
    
    // =========================  TABLE DATA LOADERS =========================
    private void loadStockData() {
    String selectedCategory = cbcat.getSelectedItem().toString();

    try (java.sql.Connection con = ConnectorXampp.connect()) {
        String sql;
        if (selectedCategory.equals("All")) {
            sql = "SELECT * FROM products ORDER BY Name ASC, Size ASC";
        } else {
            sql = "SELECT * FROM products WHERE Category = ? ORDER BY Name ASC, Size ASC";
        }

        java.sql.PreparedStatement pst = con.prepareStatement(sql);
        if (!selectedCategory.equals("All")) {
            pst.setString(1, selectedCategory);
        }

        java.sql.ResultSet rs = pst.executeQuery();
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);

        while (rs.next()) {
            double rawPrice = rs.getDouble("Price");
            int rawQuantity = rs.getInt("Quantity");

            model.addRow(new Object[]{
                rs.getInt("ProductID"),
                rs.getString("Name"),
                rs.getString("Category"),
                rs.getString("Size"),
                rawPrice,
                rawQuantity
            });
        }
        formatTableDisplay(); // ✅ MOVED OUTSIDE the while loop

    } catch (Exception e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Error loading stock data: " + e.getMessage());
    }
}

    private void loadTableData() {
    DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
    model.setRowCount(0);

    String sql = "SELECT * FROM products ORDER BY Name ASC, Size ASC";

    try (java.sql.Connection con = ConnectorXampp.connect();
         java.sql.Statement st = con.createStatement();
         java.sql.ResultSet rs = st.executeQuery(sql)) {
       
        while (rs.next()) {
            double rawPrice = rs.getDouble("Price");
            int rawQuantity = rs.getInt("Quantity");
            
            model.addRow(new Object[]{
                rs.getInt("ProductID"),
                rs.getString("Name"),
                rs.getString("Category"),
                rs.getString("Size"),
                rawPrice,        
                rawQuantity     
            });
        }
        formatTableDisplay(); 
    } catch (Exception e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
    }
}

    private void loadTableData(String search) {
    DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
    model.setRowCount(0);

    String sql = "SELECT * FROM products WHERE Name LIKE ? OR Category LIKE ? ORDER BY Name ASC, Size ASC";

    try (java.sql.Connection con = ConnectorXampp.connect();
         java.sql.PreparedStatement pst = con.prepareStatement(sql)) {

        pst.setString(1, "%" + search + "%");
        pst.setString(2, "%" + search + "%");
        java.sql.ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            double rawPrice = rs.getDouble("Price"); // ✅ FIXED: was getInt
            int rawQuantity = rs.getInt("Quantity");

            model.addRow(new Object[]{
                rs.getInt("ProductID"),
                rs.getString("Name"),
                rs.getString("Category"),
                rs.getString("Size"),
                rawPrice,
                rawQuantity
            });
        }
        formatTableDisplay();
    } catch (Exception e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
    }
}
    
    
    // =========================  TABLE FORMATTING =========================
    private void formatTableDisplay() {
    java.text.NumberFormat currencyFormat = new java.text.DecimalFormat("₱#,##0.00");

    // Price renderer
    javax.swing.table.TableCellRenderer priceRenderer = (javax.swing.JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) -> {
        javax.swing.JLabel label = new javax.swing.JLabel();
        if (value != null) {
            try {
                double price = Double.parseDouble(value.toString()); // ✅ handles any number type
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
    jTable1.getColumnModel().getColumn(4).setCellRenderer(priceRenderer);

    // Quantity renderer
    javax.swing.table.TableCellRenderer quantityRenderer = (javax.swing.JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) -> {
        javax.swing.JLabel label = new javax.swing.JLabel();
        if (value != null) {
            try {
                int qty = Integer.parseInt(value.toString()); // ✅ handles any number type
                label.setText(qty == 0 ? "Out of Stock" : String.valueOf(qty));
                if (qty == 0) {
                    label.setForeground(java.awt.Color.RED);
                } else if (qty < 10) {
                    label.setForeground(java.awt.Color.ORANGE);
                }
            } catch (Exception ex) {
                label.setText("0");
            }
        } else {
            label.setText("0");
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
    jTable1.getColumnModel().getColumn(5).setCellRenderer(quantityRenderer);
}
    
    
    // =========================  UI CONTROL METHODS =========================
    private void setFieldsEnabled(boolean enabled) {
    txtItemName.setEnabled(enabled);
    txtQuantity.setEnabled(enabled);
    txtPrice.setEnabled(enabled);
    cmbCategory.setEnabled(enabled);
    cmbSize.setEnabled(enabled);
}

    private void clearFields() {
    txtItemName.setText("");
    txtPrice.setText("");
    txtQuantity.setText("");
    cmbCategory.setSelectedIndex(0);
    cmbSize.setSelectedIndex(0);
    txtSearch.setText("");
}

    private void resetButtons() {
    BTNAdd.setEnabled(true);
    BtnEdit.setEnabled(false);
    BtnUpdate.setEnabled(false);
    BtnDelete.setEnabled(false);
    BtnReset.setEnabled(false);
}
    
   
    // =========================  VALIDATION =========================
    private boolean isProductSizeExists(String productName, String size) {
    String sql = "SELECT COUNT(*) FROM products WHERE LOWER(Name) = LOWER(?) AND LOWER(Size) = LOWER(?)";
    
    if (editingProductId != -1) {
        sql += " AND ProductID != ?";
    }
    
    try (java.sql.Connection con = ConnectorXampp.connect();
         java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
        
        pst.setString(1, productName);
        pst.setString(2, size);
        
        
        if (editingProductId != -1) {
            pst.setInt(3, editingProductId);
        }
        
        java.sql.ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            int count = rs.getInt(1);
            System.out.println("🔍 Case-insensitive check: '" + productName + "' (" + size + ") = " + count + " others");
            return count > 0; 
        }
    } catch (Exception e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Error checking duplicate: " + e.getMessage());
    }
    return false;
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
        jPanel3 = new javax.swing.JPanel();
        btnDashBoard = new javax.swing.JButton();
        btnProducts = new javax.swing.JButton();
        btnCategory = new javax.swing.JButton();
        btnAddons = new javax.swing.JButton();
        btnHistory = new javax.swing.JButton();
        btnLogOut = new javax.swing.JButton();
        btnUtilities = new javax.swing.JButton();
        btnSize = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        btnInventory = new javax.swing.JButton();
        btnPOS = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        txtItemName = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        cmbCategory = new javax.swing.JComboBox<>();
        cmbSize = new javax.swing.JComboBox<>();
        txtPrice = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txtQuantity = new javax.swing.JTextField();
        BtnEdit = new javax.swing.JButton();
        BTNAdd = new javax.swing.JButton();
        BtnDelete = new javax.swing.JButton();
        BtnUpdate = new javax.swing.JButton();
        BtnReset = new javax.swing.JButton();
        cbcat = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));

        jPanel3.setBackground(new java.awt.Color(18, 20, 23));

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

        btnHistory.setBackground(new java.awt.Color(18, 20, 23));
        btnHistory.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnHistory.setForeground(new java.awt.Color(197, 160, 114));
        btnHistory.setText("Reports");
        btnHistory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHistoryActionPerformed(evt);
            }
        });

        btnLogOut.setBackground(new java.awt.Color(18, 20, 23));
        btnLogOut.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnLogOut.setForeground(new java.awt.Color(197, 160, 114));
        btnLogOut.setText("Log Out");
        btnLogOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogOutActionPerformed(evt);
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

        btnSize.setBackground(new java.awt.Color(18, 20, 23));
        btnSize.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnSize.setForeground(new java.awt.Color(197, 160, 114));
        btnSize.setText("Size");
        btnSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSizeActionPerformed(evt);
            }
        });

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/admin1.1.png"))); // NOI18N

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
                        .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnSize, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnLogOut, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnHistory, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnAddons, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnCategory, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnProducts, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnDashBoard, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnUtilities, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnInventory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnPOS, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(17, 17, 17))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 55, Short.MAX_VALUE)
                .addComponent(btnDashBoard)
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
                .addGap(12, 12, 12)
                .addComponent(btnInventory)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnPOS)
                .addGap(158, 158, 158)
                .addComponent(btnLogOut)
                .addGap(57, 57, 57))
        );

        jTable1.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        jTable1.setForeground(new java.awt.Color(197, 160, 114));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Name", "Category", "Size", "Selling Price", "Quantity"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jPanel2.setBackground(new java.awt.Color(18, 20, 23));

        jLabel1.setFont(new java.awt.Font("Serif", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(197, 160, 114));
        jLabel1.setText("Search Item:");

        jLabel7.setFont(new java.awt.Font("Serif", 1, 24)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(197, 160, 114));
        jLabel7.setText("Products");

        txtSearch.setFont(new java.awt.Font("Serif", 1, 14)); // NOI18N
        txtSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });

        jPanel4.setBackground(new java.awt.Color(18, 20, 23));

        jLabel2.setFont(new java.awt.Font("Serif", 1, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(197, 160, 114));
        jLabel2.setText("Item Name:");

        txtItemName.setFont(new java.awt.Font("Serif", 1, 14)); // NOI18N

        jLabel3.setFont(new java.awt.Font("Serif", 1, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(197, 160, 114));
        jLabel3.setText("Category:");

        jLabel4.setFont(new java.awt.Font("Serif", 1, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(197, 160, 114));
        jLabel4.setText("Size:");

        cmbCategory.setFont(new java.awt.Font("Serif", 1, 14)); // NOI18N

        cmbSize.setFont(new java.awt.Font("Serif", 1, 14)); // NOI18N
        cmbSize.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Small", "Medium", "Large" }));

        txtPrice.setFont(new java.awt.Font("Serif", 1, 14)); // NOI18N

        jLabel5.setFont(new java.awt.Font("Serif", 1, 14)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(197, 160, 114));
        jLabel5.setText("Price:");

        jLabel6.setFont(new java.awt.Font("Serif", 1, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(197, 160, 114));
        jLabel6.setText("Quantity:");

        txtQuantity.setFont(new java.awt.Font("Serif", 1, 14)); // NOI18N

        BtnEdit.setBackground(new java.awt.Color(18, 20, 23));
        BtnEdit.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BtnEdit.setForeground(new java.awt.Color(197, 160, 114));
        BtnEdit.setText("EDIT");
        BtnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnEditActionPerformed(evt);
            }
        });

        BTNAdd.setBackground(new java.awt.Color(18, 20, 23));
        BTNAdd.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BTNAdd.setForeground(new java.awt.Color(197, 160, 114));
        BTNAdd.setText("ADD");
        BTNAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BTNAddActionPerformed(evt);
            }
        });

        BtnDelete.setBackground(new java.awt.Color(18, 20, 23));
        BtnDelete.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BtnDelete.setForeground(new java.awt.Color(197, 160, 114));
        BtnDelete.setText("DELETE");
        BtnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnDeleteActionPerformed(evt);
            }
        });

        BtnUpdate.setBackground(new java.awt.Color(18, 20, 23));
        BtnUpdate.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BtnUpdate.setForeground(new java.awt.Color(197, 160, 114));
        BtnUpdate.setText("UPDATE");
        BtnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnUpdateActionPerformed(evt);
            }
        });

        BtnReset.setBackground(new java.awt.Color(18, 20, 23));
        BtnReset.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BtnReset.setForeground(new java.awt.Color(197, 160, 114));
        BtnReset.setText("RESET");
        BtnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnResetActionPerformed(evt);
            }
        });

        cbcat.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        cbcat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbcatActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(txtItemName, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(cmbCategory, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(txtPrice))
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(txtQuantity))
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(cmbSize, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(42, 42, 42)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(BtnDelete, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(BTNAdd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(BtnEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(BtnUpdate)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addComponent(BtnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(cbcat, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 208, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtItemName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cmbCategory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(BTNAdd)
                            .addComponent(BtnEdit))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(BtnDelete)
                            .addComponent(BtnUpdate))))
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cmbSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtPrice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtQuantity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(BtnReset)
                        .addGap(28, 28, 28)
                        .addComponent(cbcat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(19, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 494, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(661, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1365, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(306, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 31, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 491, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(51, 51, 51))
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1920, 870));

        pack();
    }// </editor-fold>//GEN-END:initComponents

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

    private void btnLogOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogOutActionPerformed
        // TODO add your handling code here:
        Login h = new Login();
       h.setVisible(true);
       this.dispose();
    }//GEN-LAST:event_btnLogOutActionPerformed

    private void BTNAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTNAddActionPerformed
        // TODO add your handling code here:
        if (!txtItemName.isEnabled()) {
       
        setFieldsEnabled(true);
        BtnReset.setEnabled(true);
        BTNAdd.setEnabled(true);
        BtnEdit.setEnabled(false);
        BtnUpdate.setEnabled(false);
        BtnDelete.setEnabled(false);
        clearFields();
        return;
    }

    
    String name = txtItemName.getText().trim();
    String category = cmbCategory.getSelectedItem().toString();
    String size = cmbSize.getSelectedItem().toString();
    String priceStr = txtPrice.getText().trim();
    String quantityStr = txtQuantity.getText().trim();

    if (name.isEmpty() || priceStr.isEmpty() || quantityStr.isEmpty()) {
        javax.swing.JOptionPane.showMessageDialog(this, "Please fill all fields.");
        return;
    }

    try {
        double price = Double.parseDouble(priceStr);
        
        int quantity = Integer.parseInt(quantityStr);
        
        
       if (isProductSizeExists(name, size)) {
    javax.swing.JOptionPane.showMessageDialog(this, 
        editingProductId == -1 ? 
            "❌ Product '" + name + "' (" + size + ") already exists!" :
            "❌ Another product '" + name + "' (" + size + ") already exists!\n" +
            "💡 Choose different name or size.", 
        "Duplicate Product", javax.swing.JOptionPane.WARNING_MESSAGE);
    return;
}
        

        String sql = "INSERT INTO products (Name, Category, Size, Price, Quantity) VALUES (?, ?, ?, ?, ?)";

        try (java.sql.Connection con = ConnectorXampp.connect();
            java.sql.PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, name);
            pst.setString(2, category);
            pst.setString(3, size);
            pst.setDouble(4, price);
            pst.setInt(5, quantity);
            pst.executeUpdate();

            javax.swing.JOptionPane.showMessageDialog(this, "Product added successfully.");
            loadTableData();
            clearFields();
            setFieldsEnabled(false);
            resetButtons();

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error adding product: " + e.getMessage());
        }

    } catch (NumberFormatException e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Price and Quantity must be numbers.");
    }
    }//GEN-LAST:event_BTNAddActionPerformed

    private void BtnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnEditActionPerformed
        // TODO add your handling code here:
     int selectedRow = jTable1.getSelectedRow();
    if (selectedRow == -1) {
        javax.swing.JOptionPane.showMessageDialog(this, "Please select a product to edit.");
        return;
    }

    editingProductId = (int) jTable1.getValueAt(selectedRow, 0);
    
    
    String itemName = (String) jTable1.getValueAt(selectedRow, 1);
    String category = (String) jTable1.getValueAt(selectedRow, 2);
    String size = (String) jTable1.getValueAt(selectedRow, 3);
    double price = ((Number) jTable1.getValueAt(selectedRow, 4)).doubleValue();
    
    int quantity = (int) jTable1.getValueAt(selectedRow, 5); 

    
    txtItemName.setText(itemName);
    cmbCategory.setSelectedItem(category);
    cmbSize.setSelectedItem(size);
    txtPrice.setText(String.valueOf(price));
    txtQuantity.setText(String.valueOf(quantity));

    setFieldsEnabled(true);
    BtnUpdate.setEnabled(true);
    BtnReset.setEnabled(true);
    BtnEdit.setEnabled(false);
    BtnDelete.setEnabled(false);
    BTNAdd.setEnabled(false);
    }//GEN-LAST:event_BtnEditActionPerformed

    private void BtnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnDeleteActionPerformed
        // TODO add your handling code here:
       int selectedRow = jTable1.getSelectedRow();
    if (selectedRow == -1) {
        javax.swing.JOptionPane.showMessageDialog(this, "Please select a product to delete.");
        return;
    }

    int id = (int) jTable1.getValueAt(selectedRow, 0);
    String productName = (String) jTable1.getValueAt(selectedRow, 1);
    
    int confirm = javax.swing.JOptionPane.showConfirmDialog(this, 
        "Delete '" + productName + "'?\n" +
        "This will PERMANENTLY delete all related orders too!", 
        "⚠️ PERMANENT DELETE", 
        javax.swing.JOptionPane.YES_NO_OPTION, 
        javax.swing.JOptionPane.WARNING_MESSAGE);
    
    if (confirm == javax.swing.JOptionPane.YES_OPTION) {
        java.sql.Connection con = null;
        try {
            con = ConnectorXampp.connect();
            con.setAutoCommit(false);
            
            // Delete orders first
//            String deleteOrdersSql = "DELETE FROM order_details WHERE ProductID = ?";
//            try (java.sql.PreparedStatement pstOrders = con.prepareStatement(deleteOrdersSql)) {
//                pstOrders.setInt(1, id);
//                int ordersDeleted = pstOrders.executeUpdate();
//                
//                // Delete product
//                
//            }
            String deleteProductSql = "DELETE FROM products WHERE ProductID = ?";
                try (java.sql.PreparedStatement pstProduct = con.prepareStatement(deleteProductSql)) {
                    pstProduct.setInt(1, id);
                    int productDeleted = pstProduct.executeUpdate();
                    
                    con.commit();
                    
                    javax.swing.JOptionPane.showMessageDialog(this, 
                        "✅ '" + productName + "' DELETED!\n" +
                        "🗑️ Product: " + productDeleted);
                }
            loadStockData();
            clearFields();
            resetButtons();
            
        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            javax.swing.JOptionPane.showMessageDialog(this, 
                "❌ Error: " + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    }//GEN-LAST:event_BtnDeleteActionPerformed

    private void BtnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnUpdateActionPerformed
        // TODO add your handling code here:
       if (editingProductId == -1) {
        return;
    }

    String name = txtItemName.getText().trim();
    String category = cmbCategory.getSelectedItem().toString();
    String size = cmbSize.getSelectedItem().toString();
    String priceStr = txtPrice.getText().trim();
    String quantityStr = txtQuantity.getText().trim();

    if (name.isEmpty() || priceStr.isEmpty() || quantityStr.isEmpty()) {
        javax.swing.JOptionPane.showMessageDialog(this, "Please fill all fields.");
        return;
    }

    try {
        double price = Double.parseDouble(priceStr);
        int quantity = Integer.parseInt(quantityStr);

        
        if (isProductSizeExists(name, size)) {
            javax.swing.JOptionPane.showMessageDialog(this, 
                "❌ Another product '" + name + "' (" + size + ") already exists!\n" +
                "💡 Choose different name or size.", 
                "Duplicate Product", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "UPDATE products SET Name = ?, Category = ?, Size = ?, Price = ?, Quantity = ? WHERE ProductID = ?";

        try (java.sql.Connection con = ConnectorXampp.connect();
             java.sql.PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, name);
            pst.setString(2, category);
            pst.setString(3, size);
            pst.setDouble(4, price);
            pst.setInt(5, quantity);
            pst.setInt(6, editingProductId);
            int rowsUpdated = pst.executeUpdate();

            if (rowsUpdated > 0) {
                javax.swing.JOptionPane.showMessageDialog(this, "✅ Product updated successfully!");
                loadTableData();  // Refresh table
                clearFields();
                setFieldsEnabled(false);
                editingProductId = -1;
                resetButtons();
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "❌ No product found to update.");
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error updating product: " + e.getMessage());
            e.printStackTrace();
        }

    } catch (NumberFormatException e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Price and Quantity must be numbers.");
    }
    }//GEN-LAST:event_BtnUpdateActionPerformed

    private void BtnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnResetActionPerformed
        // TODO add your handling code here:
        clearFields();
        setFieldsEnabled(false);
        editingProductId = -1;
        resetButtons();
    }//GEN-LAST:event_BtnResetActionPerformed

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchActionPerformed
        // TODO add your handling code here:
        String search = txtSearch.getText().trim();
        if (search.isEmpty()) {
            loadTableData();
        } else {
            loadTableData(search);
        }
    }//GEN-LAST:event_txtSearchActionPerformed

    private void cbcatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbcatActionPerformed
        // TODO add your handling code here:
         loadStockData();
    }//GEN-LAST:event_cbcatActionPerformed

    private void btnSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSizeActionPerformed
        // TODO add your handling code here:
        Size b = new Size();
        b.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnSizeActionPerformed

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
            java.util.logging.Logger.getLogger(Products.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Products.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Products.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Products.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Products().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BTNAdd;
    private javax.swing.JButton BtnDelete;
    private javax.swing.JButton BtnEdit;
    private javax.swing.JButton BtnReset;
    private javax.swing.JButton BtnUpdate;
    private javax.swing.JButton btnAddons;
    private javax.swing.JButton btnCategory;
    private javax.swing.JButton btnDashBoard;
    private javax.swing.JButton btnHistory;
    private javax.swing.JButton btnInventory;
    private javax.swing.JButton btnLogOut;
    private javax.swing.JButton btnPOS;
    private javax.swing.JButton btnProducts;
    private javax.swing.JButton btnSize;
    private javax.swing.JButton btnUtilities;
    private javax.swing.JComboBox<String> cbcat;
    private javax.swing.JComboBox<String> cmbCategory;
    private javax.swing.JComboBox<String> cmbSize;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField txtItemName;
    private javax.swing.JTextField txtPrice;
    private javax.swing.JTextField txtQuantity;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
