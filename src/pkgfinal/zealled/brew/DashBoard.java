/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pkgfinal.zealled.brew;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.util.Timer;
import java.util.TimerTask;
/**
 *
 * @author ASUS
 */
public class DashBoard extends javax.swing.JFrame {
    private String userName;
    private Timer refreshTimer;

    /**
     * Creates new form DashBoard
     */
    
    public DashBoard() {
        initComponents();
        setupTables();
        loadAllDashboardData(); // Load all data on startup
    }
    
    // ========================= TABLE SETUP =========================
    private void setupTables() {
        setupTopSellersTable();
        setupTopAddonsTable();
    }
    
    // Top Sellers Table
    private void setupTopSellersTable() {
        String[] columns = {"Rank", "Product", "Category", "Size", "Sold Today", "Revenue"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        jTableTopSellers.setModel(model);
        jTableTopSellers.setDefaultEditor(Object.class, null);
        jTableTopSellers.getTableHeader().setReorderingAllowed(false);
    }
    
    // Top Addons Table
    private void setupTopAddonsTable() {
        String[] columns = {"Rank", "Addon Name", "Times Added", "Revenue"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        jTableTopAddons.setModel(model);
        jTableTopAddons.setDefaultEditor(Object.class, null);
        jTableTopAddons.getTableHeader().setReorderingAllowed(false);
    }

    // ========================= MAIN REFRESH METHOD =========================
    private void loadAllDashboardData() {
        try {
            loadBestSellingProducts();
            loadTopAddon();
            
            try (Connection con = ConnectorXampp.connect()) {
                loadTopSellersTable(con);
                loadTodaySalesData(con);
                loadWeeklySalesData(con);
                loadMonthlySalesData(con);
                loadTopAddonsTable(con);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading dashboard data: " + e.getMessage(), 
                "Dashboard Error", JOptionPane.ERROR_MESSAGE);
        }
    }

 
    // ========================= BEST SELLING PRODUCT (LABEL) =========================
    private void loadBestSellingProducts() {
        try (Connection con = ConnectorXampp.connect()) {
            String sql = """
                SELECT p.Name, p.Size, COALESCE(SUM(od.Quantity), 0) as sold
                FROM order_details od
                JOIN products p ON od.ProductID = p.ProductID
                JOIN orders o ON od.OrderID = o.OrderID
                WHERE DATE(o.OrderDate)=CURDATE()
                GROUP BY p.ProductID
                ORDER BY sold DESC
                LIMIT 1
            """;

            try (PreparedStatement pst = con.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    lblTopSeller.setText("Top Seller: " 
                            + rs.getString("Name") + " (" 
                            + rs.getString("Size") + ") - " 
                            + rs.getInt("sold") + " sold");
                } else {
                    lblTopSeller.setText("Top Seller: No sales today");
                }
            }
        } catch (Exception e) {
            lblTopSeller.setText("Error loading top seller");
        }
    }

    // ========================= TOP SELLERS TABLE (DAILY) =========================
    private void loadTopSellersTable(Connection con) throws Exception {
        String sql = """
            SELECT p.Name, p.Category, p.Size,
                   COALESCE(SUM(od.Quantity), 0) as sold,
                   COALESCE(SUM(od.Subtotal), 0) as revenue
            FROM order_details od
            JOIN products p ON od.ProductID = p.ProductID
            JOIN orders o ON od.OrderID = o.OrderID
            WHERE DATE(o.OrderDate)=CURDATE()
            GROUP BY p.ProductID
            ORDER BY sold DESC
            LIMIT 10
        """;

        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            DefaultTableModel model = (DefaultTableModel) jTableTopSellers.getModel();
            model.setRowCount(0);

            int rank = 1;
            while (rs.next()) {
                model.addRow(new Object[]{
                    rank++,
                    rs.getString("Name"),
                    rs.getString("Category"),
                    rs.getString("Size"),
                    rs.getInt("sold"),
                    "₱" + String.format("%.2f", rs.getDouble("revenue"))
                });
            }
        }
    }

    // ========================= TOP ADDON (DAILY) =========================
    private void loadTopAddon() {
        try (Connection con = ConnectorXampp.connect()) {
            String sql = """
                SELECT a.Name, COALESCE(COUNT(*), 0) as times_added, 
                       COALESCE(SUM(od.AddonPrice * od.Quantity), 0) as revenue
                FROM order_details od
                JOIN addons a ON od.AddonName = a.Name
                JOIN orders o ON od.OrderID = o.OrderID
                WHERE DATE(o.OrderDate) = CURDATE() AND od.AddonName != 'None'
                GROUP BY a.Name
                ORDER BY times_added DESC
                LIMIT 1
            """;

            try (PreparedStatement pst = con.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    lblTopAddon.setText("Top Addon: " 
                            + rs.getString("Name") + " - " 
                            + rs.getInt("times_added") + " times (₱" 
                            + String.format("%.2f", rs.getDouble("revenue")) + ")");
                } else {
                    lblTopAddon.setText("Top Addon: None today");
                }
            }
        } catch (Exception e) {
            lblTopAddon.setText("Error loading top addon");
        }
    }

    // ========================= TOP ADDONS TABLE (DAILY) =========================
    private void loadTopAddonsTable(Connection con) throws Exception {
        String sql = """
            SELECT a.Name as AddonName,
                   COALESCE(COUNT(*), 0) as times_added,
                   COALESCE(SUM(od.AddonPrice * od.Quantity), 0) as revenue
            FROM order_details od
            JOIN addons a ON od.AddonName = a.Name
            JOIN orders o ON od.OrderID = o.OrderID
            WHERE DATE(o.OrderDate) = CURDATE() AND od.AddonName != 'None'
            GROUP BY a.Name
            ORDER BY times_added DESC
            LIMIT 10
        """;

        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            DefaultTableModel model = (DefaultTableModel) jTableTopAddons.getModel();
            model.setRowCount(0);

            int rank = 1;
            while (rs.next()) {
                model.addRow(new Object[]{
                    rank++,
                    rs.getString("AddonName"),
                    rs.getInt("times_added"),
                    "₱" + String.format("%.2f", rs.getDouble("revenue"))
                });
            }
        }
    }
    
    // ========================= TODAY'S SALES ========================
    private void loadTodaySalesData(Connection con) throws Exception {
        String sql = """
            SELECT COUNT(*) as orders,
                   COALESCE(SUM(TotalAmount), 0) as total,
                   COALESCE(AVG(TotalAmount), 0) as avg
            FROM orders
            WHERE DATE(OrderDate) = CURDATE()
        """;

        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                lblTodayOrders.setText("Today's Orders: " + rs.getInt("orders"));
                lblTodaySales.setText("Today's Sales: ₱" + String.format("%.2f", rs.getDouble("total")));
                lblTodayAvgOrder.setText("Avg Order: ₱" + String.format("%.2f", rs.getDouble("avg")));
            }
        }

        // Today's Revenue (same as sales)
        lblTodayRevenue.setText(lblTodaySales.getText().replace("Sales", "Revenue"));
        
        // Today's Profit 
        String sqlProfit = """
            SELECT COALESCE(SUM(od.Subtotal), 0) as revenue
            FROM order_details od
            JOIN orders o ON od.OrderID = o.OrderID
            WHERE DATE(o.OrderDate)=CURDATE()
        """;
        try (PreparedStatement pst = con.prepareStatement(sqlProfit);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                double revenue = rs.getDouble("revenue");
                double profit = revenue * 0.4; // 40% profit margin
                lblTodayProfit.setText("Today's Profit: ₱" + String.format("%.2f", profit));
            }
        }
    }
    
    // ========================= WEEKLY SALES =========================
    private void loadWeeklySalesData(Connection con) throws Exception {
        String sql = """
            SELECT COUNT(*) as orders,
                   COALESCE(SUM(TotalAmount), 0) as total,
                   COALESCE(AVG(TotalAmount), 0) as avg
            FROM orders
            WHERE YEARWEEK(OrderDate, 1) = YEARWEEK(CURDATE(), 1)
        """;

        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                lblWeekOrders.setText("This Week's Orders: " + rs.getInt("orders"));
                lblWeekSales.setText("This Week's Sales: ₱" + String.format("%.2f", rs.getDouble("total")));
                lblWeekAvgOrder.setText("Avg Order: ₱" + String.format("%.2f", rs.getDouble("avg")));
            }
        }

        lblWeekRevenue.setText(lblWeekSales.getText().replace("Sales", "Revenue"));

        // Weekly Profit
        String sqlProfit = """
            SELECT COALESCE(SUM(od.Subtotal), 0) as revenue
            FROM order_details od
            JOIN orders o ON od.OrderID = o.OrderID
            WHERE YEARWEEK(o.OrderDate, 1) = YEARWEEK(CURDATE(), 1)
        """;
        try (PreparedStatement pst = con.prepareStatement(sqlProfit);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                double revenue = rs.getDouble("revenue");
                double profit = revenue * 0.4;
                lblWeekProfit.setText("This Week's Profit: ₱" + String.format("%.2f", profit));
            }
        }
    }

    // ========================= MONTHLY SALES =========================
    private void loadMonthlySalesData(Connection con) throws Exception {
        String sql = """
            SELECT COUNT(*) as orders,
                   COALESCE(SUM(TotalAmount), 0) as total,
                   COALESCE(AVG(TotalAmount), 0) as avg
            FROM orders
            WHERE YEAR(OrderDate) = YEAR(CURDATE())
            AND MONTH(OrderDate) = MONTH(CURDATE())
        """;

        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                lblMonthOrders.setText("This Month's Orders: " + rs.getInt("orders"));
                lblMonthSales.setText("This Month's Sales: ₱" + String.format("%.2f", rs.getDouble("total")));
                lblMonthAvgOrder.setText("Avg Order: ₱" + String.format("%.2f", rs.getDouble("avg")));
            }
        }

        lblMonthRevenue.setText(lblMonthSales.getText().replace("Sales", "Revenue"));

        // Monthly Profit
        String sqlProfit = """
            SELECT COALESCE(SUM(od.Subtotal), 0) as revenue
            FROM order_details od
            JOIN orders o ON od.OrderID = o.OrderID
            WHERE YEAR(o.OrderDate)=YEAR(CURDATE()) 
            AND MONTH(o.OrderDate)=MONTH(CURDATE())
        """;
        try (PreparedStatement pst = con.prepareStatement(sqlProfit);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                double revenue = rs.getDouble("revenue");
                double profit = revenue * 0.4;
                lblMonthProfit.setText("This Month's Profit: ₱" + String.format("%.2f", profit));
            }
        }
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
        lblWelcome = new javax.swing.JLabel();
        btnSize = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        btnInventory = new javax.swing.JButton();
        btnPOS = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        lblTodaySales = new javax.swing.JLabel();
        lblTodayOrders = new javax.swing.JLabel();
        lblTodayProfit = new javax.swing.JLabel();
        lblTodayRevenue = new javax.swing.JLabel();
        lblTodayAvgOrder = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        lblWeekOrders = new javax.swing.JLabel();
        lblWeekAvgOrder = new javax.swing.JLabel();
        lblWeekSales = new javax.swing.JLabel();
        lblWeekRevenue = new javax.swing.JLabel();
        lblWeekProfit = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        lblMonthOrders = new javax.swing.JLabel();
        lblMonthSales = new javax.swing.JLabel();
        lblMonthAvgOrder = new javax.swing.JLabel();
        lblMonthRevenue = new javax.swing.JLabel();
        lblMonthProfit = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel4 = new javax.swing.JPanel();
        lblTopSeller = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTableTopSellers = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        lblTopAddonsTitle = new javax.swing.JLabel();
        lblTopAddon = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableTopAddons = new javax.swing.JTable();
        btnRefresh = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setForeground(new java.awt.Color(40, 40, 40));

        jPanel3.setBackground(new java.awt.Color(40, 40, 40));

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
        btnHistory.setText("History");
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

        btnInventory.setText("Inventory");
        btnInventory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnInventoryActionPerformed(evt);
            }
        });

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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnLogOut, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(17, 17, 17))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnInventory)
                    .addComponent(btnPOS))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblWelcome, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnSize, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(btnHistory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnAddons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnCategory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnProducts, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnDashBoard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnUtilities, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 152, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 77, Short.MAX_VALUE)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblWelcome, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addComponent(btnInventory)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnPOS)))
                .addGap(64, 64, 64)
                .addComponent(btnLogOut)
                .addGap(57, 57, 57))
        );

        jPanel9.setBackground(new java.awt.Color(40, 40, 40));

        jTabbedPane2.setForeground(new java.awt.Color(197, 160, 114));

        jPanel6.setForeground(new java.awt.Color(40, 40, 40));

        jLabel1.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(197, 160, 114));
        jLabel1.setText("Today's Sales");

        lblTodaySales.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTodaySales.setText(".");

        lblTodayOrders.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTodayOrders.setText(".");

        lblTodayProfit.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTodayProfit.setText(".");

        lblTodayRevenue.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTodayRevenue.setText(".");

        lblTodayAvgOrder.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTodayAvgOrder.setText(".");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblTodayOrders, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblTodayRevenue, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblTodayProfit, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblTodayAvgOrder, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(111, 111, 111)
                        .addComponent(jLabel1))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblTodaySales, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(11, 11, 11)
                .addComponent(lblTodayOrders)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblTodaySales)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblTodayRevenue)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblTodayProfit)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblTodayAvgOrder)
                .addContainerGap(49, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Today", jPanel6);

        jLabel3.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(197, 160, 114));
        jLabel3.setText("Weekly Sales");

        lblWeekOrders.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblWeekOrders.setText(".");

        lblWeekAvgOrder.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblWeekAvgOrder.setText(".");

        lblWeekSales.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblWeekSales.setText(".");

        lblWeekRevenue.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblWeekRevenue.setText(".");

        lblWeekProfit.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblWeekProfit.setText(".");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(113, 113, 113)
                .addComponent(jLabel3)
                .addContainerGap(130, Short.MAX_VALUE))
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblWeekOrders, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblWeekAvgOrder, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblWeekSales, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblWeekRevenue, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblWeekProfit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblWeekOrders)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblWeekSales)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblWeekAvgOrder)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblWeekRevenue)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblWeekProfit)
                .addContainerGap(48, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Weekly", jPanel7);

        jLabel2.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(197, 160, 114));
        jLabel2.setText("Monthly's Sales");

        lblMonthOrders.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblMonthOrders.setText(".");

        lblMonthSales.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblMonthSales.setText(".");

        lblMonthAvgOrder.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblMonthAvgOrder.setText(".");

        lblMonthRevenue.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblMonthRevenue.setText(".");

        lblMonthProfit.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblMonthProfit.setText(".");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblMonthOrders, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblMonthSales, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblMonthAvgOrder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblMonthRevenue, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblMonthProfit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(101, 101, 101)
                .addComponent(jLabel2)
                .addContainerGap(119, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMonthOrders)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMonthSales)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMonthRevenue)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMonthProfit)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMonthAvgOrder)
                .addContainerGap(48, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Monthly", jPanel8);

        jTabbedPane1.setForeground(new java.awt.Color(197, 160, 114));

        jPanel4.setForeground(new java.awt.Color(40, 40, 40));

        lblTopSeller.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTopSeller.setForeground(new java.awt.Color(40, 40, 40));
        lblTopSeller.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTopSeller.setText(".");

        jTableTopSellers.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        jTableTopSellers.setForeground(new java.awt.Color(197, 160, 114));
        jTableTopSellers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Rank", "Product", "Category", "Size", "Sold", "Revenue"
            }
        ));
        jScrollPane3.setViewportView(jTableTopSellers);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(lblTopSeller, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 975, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(260, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTopSeller)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 737, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Products", jPanel4);

        lblTopAddonsTitle.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTopAddonsTitle.setForeground(new java.awt.Color(197, 160, 114));
        lblTopAddonsTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTopAddonsTitle.setText("Top Addons Today");
        lblTopAddonsTitle.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        lblTopAddon.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTopAddon.setForeground(new java.awt.Color(40, 40, 40));
        lblTopAddon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTopAddon.setText(".");

        jTableTopAddons.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        jTableTopAddons.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Rank", "Addon", "Sold", "Revenue", "Avg Price"
            }
        ));
        jScrollPane2.setViewportView(jTableTopAddons);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(lblTopAddon, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblTopAddonsTitle, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 914, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 980, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(255, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(lblTopAddonsTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblTopAddon)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 708, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Addons", jPanel5);

        btnRefresh.setText("Refresh");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jTabbedPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addGap(99, 99, 99)
                        .addComponent(btnRefresh)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1241, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(158, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 761, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addComponent(jTabbedPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(59, 59, 59)
                        .addComponent(btnRefresh)))
                .addContainerGap(56, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1920, 820));

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

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        // TODO add your handling code here:
        btnRefresh.setText("Refreshing...");
        btnRefresh.setEnabled(false);
    
        javax.swing.SwingUtilities.invokeLater(() -> {
        loadAllDashboardData();  // Reloads ALL dashboard data
        btnRefresh.setText("🔄 Refresh");
        btnRefresh.setEnabled(true);
    });
    }//GEN-LAST:event_btnRefreshActionPerformed

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
            java.util.logging.Logger.getLogger(DashBoard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DashBoard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DashBoard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DashBoard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DashBoard().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddons;
    private javax.swing.JButton btnCategory;
    private javax.swing.JButton btnDashBoard;
    private javax.swing.JButton btnHistory;
    private javax.swing.JButton btnInventory;
    private javax.swing.JButton btnLogOut;
    private javax.swing.JButton btnPOS;
    private javax.swing.JButton btnProducts;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnSize;
    private javax.swing.JButton btnUtilities;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTable jTableTopAddons;
    private javax.swing.JTable jTableTopSellers;
    private javax.swing.JLabel lblMonthAvgOrder;
    private javax.swing.JLabel lblMonthOrders;
    private javax.swing.JLabel lblMonthProfit;
    private javax.swing.JLabel lblMonthRevenue;
    private javax.swing.JLabel lblMonthSales;
    private javax.swing.JLabel lblTodayAvgOrder;
    private javax.swing.JLabel lblTodayOrders;
    private javax.swing.JLabel lblTodayProfit;
    private javax.swing.JLabel lblTodayRevenue;
    private javax.swing.JLabel lblTodaySales;
    private javax.swing.JLabel lblTopAddon;
    private javax.swing.JLabel lblTopAddonsTitle;
    private javax.swing.JLabel lblTopSeller;
    private javax.swing.JLabel lblWeekAvgOrder;
    private javax.swing.JLabel lblWeekOrders;
    private javax.swing.JLabel lblWeekProfit;
    private javax.swing.JLabel lblWeekRevenue;
    private javax.swing.JLabel lblWeekSales;
    private javax.swing.JLabel lblWelcome;
    // End of variables declaration//GEN-END:variables

    
}
