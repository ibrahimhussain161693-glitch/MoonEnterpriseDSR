const express = require('express');
const cors = require('cors');
const sqlite3 = require('sqlite3').verbose();
const { createClient } = require('@supabase/supabase-js');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Health Check Route for Render / Cloud Monitoring
app.get('/health', (req, res) => {
    res.status(200).send('OK');
});

// Supabase Cloud Configuration (Set via environment variables or default config)
const SUPABASE_URL = process.env.SUPABASE_URL || '';
const SUPABASE_KEY = process.env.SUPABASE_KEY || '';

let supabase = null;
let useSupabase = false;

if (SUPABASE_URL && SUPABASE_KEY) {
    supabase = createClient(SUPABASE_URL, SUPABASE_KEY);
    useSupabase = true;
    console.log('Connected to Supabase Cloud Database:', SUPABASE_URL);
} else {
    console.log('SUPABASE_URL not provided. Falling back to local SQLite database.');
}

// Database Connection (Local SQLite Fallback)
const db = new sqlite3.Database(path.join(__dirname, 'moon_dsr.db'), (err) => {
    if (err) console.error('Database connection error:', err.message);
    else console.log('Connected to Moon Enterprise DSR SQLite database.');
});

// Initialize Tables & Seed Data for Local SQLite
db.serialize(() => {
    db.run(`CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT,
        phone TEXT UNIQUE,
        password TEXT,
        role TEXT
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS products (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT,
        price REAL,
        stock_cartons INTEGER,
        stock_pieces INTEGER,
        pieces_per_carton INTEGER
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS shops (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT,
        owner_name TEXT,
        phone TEXT,
        address TEXT,
        lat REAL,
        lng REAL,
        due_amount REAL DEFAULT 0
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS orders (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        shop_id INTEGER,
        shop_name TEXT,
        sr_id INTEGER,
        sr_name TEXT,
        dsr_id INTEGER,
        dsr_name TEXT,
        total_bill REAL,
        discount REAL,
        free_gifts TEXT,
        cash_collected REAL,
        due_amount REAL,
        status TEXT DEFAULT 'PENDING',
        is_out_of_location INTEGER DEFAULT 0,
        distance_meters REAL DEFAULT 0,
        sr_lat REAL,
        sr_lng REAL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS order_items (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        order_id INTEGER,
        product_id INTEGER,
        product_name TEXT,
        cartons INTEGER,
        pieces INTEGER,
        price REAL
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS collections (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        shop_id INTEGER,
        shop_name TEXT,
        dsr_id INTEGER,
        dsr_name TEXT,
        amount REAL,
        payment_type TEXT,
        date DATETIME DEFAULT CURRENT_TIMESTAMP
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS discounts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT,
        min_amount REAL,
        discount_amount REAL,
        gift_description TEXT,
        is_active INTEGER DEFAULT 1
    )`);

    // Seed Data for SQLite
    db.get("SELECT COUNT(*) AS count FROM users", (err, row) => {
        if (row && row.count === 0) {
            db.run("INSERT INTO users (name, phone, password, role) VALUES ('System Admin', '01900000000', '1234', 'ADMIN')");
            db.run("INSERT INTO users (name, phone, password, role) VALUES ('Rahim Ahmed (SR)', '01811111111', '1234', 'SR')");
            db.run("INSERT INTO users (name, phone, password, role) VALUES ('Karim Uddin (DSR)', '01822222222', '1234', 'DSR')");
        }
    });

    db.get("SELECT COUNT(*) AS count FROM products", (err, row) => {
        if (row && row.count === 0) {
            db.run("INSERT INTO products (name, price, stock_cartons, stock_pieces, pieces_per_carton) VALUES ('Cone Ice Cream', 35, 50, 200, 24)");
            db.run("INSERT INTO products (name, price, stock_cartons, stock_pieces, pieces_per_carton) VALUES ('Malai Choco Bar', 25, 40, 150, 30)");
            db.run("INSERT INTO products (name, price, stock_cartons, stock_pieces, pieces_per_carton) VALUES ('Strawberry Box 500ml', 120, 30, 80, 12)");
        }
    });

    db.get("SELECT COUNT(*) AS count FROM shops", (err, row) => {
        if (row && row.count === 0) {
            db.run("INSERT INTO shops (name, owner_name, phone, address, lat, lng, due_amount) VALUES ('Bismillah General Store', 'Abdul Jalil', '01711223344', 'Tongi Bazaar, Gazipur', 23.8920, 90.4020, 1200)");
            db.run("INSERT INTO shops (name, owner_name, phone, address, lat, lng, due_amount) VALUES ('Popular Ice Cream Corner', 'Kamal Hossain', '01755667788', 'Station Road, Tongi', 23.8950, 90.4050, 850)");
        }
    });

    db.get("SELECT COUNT(*) AS count FROM discounts", (err, row) => {
        if (row && row.count === 0) {
            db.run("INSERT INTO discounts (title, min_amount, discount_amount, gift_description, is_active) VALUES ('5000+ Order Offer', 5000, 200, '5 Free Cones', 1)");
        }
    });
});

// Calculate distance between 2 coordinates in meters (Haversine formula)
function getDistanceMeters(lat1, lon1, lat2, lon2) {
    if (!lat1 || !lon1 || !lat2 || !lon2) return 0;
    const R = 6371e3; // metres
    const φ1 = lat1 * Math.PI/180;
    const φ2 = lat2 * Math.PI/180;
    const Δφ = (lat2-lat1) * Math.PI/180;
    const Δλ = (lon2-lon1) * Math.PI/180;

    const a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
              Math.cos(φ1) * Math.cos(φ2) *
              Math.sin(Δλ/2) * Math.sin(Δλ/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
}

// REST API Endpoints

// Login API
app.post('/api/login', async (req, res) => {
    const { phone, password } = req.body;

    if (useSupabase) {
        const { data, error } = await supabase.from('users').select('*').eq('phone', phone).eq('password', password).single();
        if (error || !data) return res.status(401).json({ success: false, message: 'Invalid phone or password!' });
        return res.json({ success: true, user: data });
    }

    db.get("SELECT * FROM users WHERE phone = ? AND password = ?", [phone, password], (err, user) => {
        if (err) return res.status(500).json({ success: false, message: err.message });
        if (!user) return res.status(401).json({ success: false, message: 'Invalid phone or password!' });
        res.json({ success: true, user });
    });
});

// User Management API
app.get('/api/users', async (req, res) => {
    if (useSupabase) {
        const { data, error } = await supabase.from('users').select('id, name, phone, role');
        if (error) return res.status(500).json({ error: error.message });
        return res.json(data);
    }

    db.all("SELECT id, name, phone, role FROM users", [], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

app.post('/api/users', async (req, res) => {
    const { name, phone, password, role } = req.body;
    if (!name || !phone || !password || !role) {
        return res.status(400).json({ success: false, message: 'All fields are required!' });
    }

    if (useSupabase) {
        const { data, error } = await supabase.from('users').insert([{ name, phone, password, role }]).select().single();
        if (error) return res.status(500).json({ success: false, error: error.message });
        return res.json({ success: true, user_id: data.id, message: 'User profile created successfully!' });
    }

    db.run("INSERT INTO users (name, phone, password, role) VALUES (?, ?, ?, ?)",
        [name, phone, password, role],
        function(err) {
            if (err) return res.status(500).json({ success: false, error: err.message });
            res.json({ success: true, user_id: this.lastID, message: 'User profile created successfully!' });
        }
    );
});

// Products & Stock API
app.get('/api/products', async (req, res) => {
    if (useSupabase) {
        const { data, error } = await supabase.from('products').select('*');
        if (error) return res.status(500).json({ error: error.message });
        return res.json(data);
    }

    db.all("SELECT * FROM products", [], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

app.post('/api/products/add', async (req, res) => {
    const { name, price, stock_cartons, stock_pieces, pieces_per_carton } = req.body;
    if (!name || !price) {
        return res.status(400).json({ success: false, message: 'Product name and price are required!' });
    }

    if (useSupabase) {
        const { data, error } = await supabase.from('products').insert([{
            name, price, stock_cartons: stock_cartons || 0, stock_pieces: stock_pieces || 0, pieces_per_carton: pieces_per_carton || 1
        }]).select().single();
        if (error) return res.status(500).json({ success: false, error: error.message });
        return res.json({ success: true, product_id: data.id, message: 'New product added successfully!' });
    }

    db.run("INSERT INTO products (name, price, stock_cartons, stock_pieces, pieces_per_carton) VALUES (?, ?, ?, ?, ?)",
        [name, price, stock_cartons || 0, stock_pieces || 0, pieces_per_carton || 1],
        function(err) {
            if (err) return res.status(500).json({ success: false, error: err.message });
            res.json({ success: true, product_id: this.lastID, message: 'New product added successfully!' });
        }
    );
});

app.post('/api/products/stock', async (req, res) => {
    const { id, stock_cartons, stock_pieces } = req.body;

    if (useSupabase) {
        const { data: prod } = await supabase.from('products').select('stock_cartons, stock_pieces').eq('id', id).single();
        if (prod) {
            const newCartons = (prod.stock_cartons || 0) + (parseInt(stock_cartons) || 0);
            const newPieces = (prod.stock_pieces || 0) + (parseInt(stock_pieces) || 0);
            await supabase.from('products').update({ stock_cartons: newCartons, stock_pieces: newPieces }).eq('id', id);
            return res.json({ success: true, message: 'Stock updated successfully!' });
        }
    }

    db.run("UPDATE products SET stock_cartons = stock_cartons + ?, stock_pieces = stock_pieces + ? WHERE id = ?",
        [stock_cartons || 0, stock_pieces || 0, id],
        function(err) {
            if (err) return res.status(500).json({ success: false, error: err.message });
            res.json({ success: true, message: 'Stock updated successfully!' });
        }
    );
});

// Shops API
app.get('/api/shops', async (req, res) => {
    if (useSupabase) {
        const { data, error } = await supabase.from('shops').select('*');
        if (error) return res.status(500).json({ error: error.message });
        return res.json(data);
    }

    db.all("SELECT * FROM shops", [], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

app.post('/api/shops', async (req, res) => {
    const { name, owner_name, phone, address, lat, lng } = req.body;

    if (useSupabase) {
        const { data, error } = await supabase.from('shops').insert([{ name, owner_name, phone, address, lat, lng }]).select().single();
        if (error) return res.status(500).json({ success: false, error: error.message });
        return res.json({ success: true, shop_id: data.id });
    }

    db.run("INSERT INTO shops (name, owner_name, phone, address, lat, lng) VALUES (?, ?, ?, ?, ?, ?)",
        [name, owner_name, phone, address, lat, lng],
        function(err) {
            if (err) return res.status(500).json({ success: false, error: err.message });
            res.json({ success: true, shop_id: this.lastID });
        }
    );
});

// Get DSR List
app.get('/api/dsrs', async (req, res) => {
    if (useSupabase) {
        const { data, error } = await supabase.from('users').select('id, name, phone').eq('role', 'DSR');
        if (error) return res.status(500).json({ error: error.message });
        return res.json(data);
    }

    db.all("SELECT id, name, phone FROM users WHERE role = 'DSR'", [], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

// Discounts API
app.get('/api/discounts', async (req, res) => {
    if (useSupabase) {
        const { data, error } = await supabase.from('discounts').select('*').eq('is_active', 1);
        if (error) return res.status(500).json({ error: error.message });
        return res.json(data);
    }

    db.all("SELECT * FROM discounts WHERE is_active = 1", [], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

// Order Placement API
app.post('/api/orders', async (req, res) => {
    const { shop_id, sr_id, sr_name, dsr_id, total_bill, discount, free_gifts, cash_collected, sr_lat, sr_lng, items } = req.body;

    if (useSupabase) {
        const { data: shop } = await supabase.from('shops').select('name, lat, lng, due_amount').eq('id', shop_id).single();
        if (!shop) return res.status(400).json({ success: false, message: 'Shop not found!' });

        const { data: dsr } = await supabase.from('users').select('name').eq('id', dsr_id).single();
        const dsr_name = dsr ? dsr.name : 'Unassigned';

        const distance = getDistanceMeters(sr_lat, sr_lng, shop.lat, shop.lng);
        const is_out_of_location = (distance > 50) ? 1 : 0;

        const net_bill = total_bill - discount;
        const due = net_bill - cash_collected;

        const { data: newOrder, error: orderErr } = await supabase.from('orders').insert([{
            shop_id, shop_name: shop.name, sr_id, sr_name, dsr_id, dsr_name, total_bill, discount, free_gifts, cash_collected, due_amount: due, is_out_of_location, distance_meters: Math.round(distance), sr_lat, sr_lng
        }]).select().single();

        if (orderErr) return res.status(500).json({ success: false, error: orderErr.message });

        // Update Shop Due
        await supabase.from('shops').update({ due_amount: (shop.due_amount || 0) + due }).eq('id', shop_id);

        // Insert Order Items & Deduct Stock
        if (items && Array.isArray(items)) {
            for (const item of items) {
                await supabase.from('order_items').insert([{
                    order_id: newOrder.id, product_id: item.product_id, product_name: item.product_name, cartons: item.cartons, pieces: item.pieces, price: item.price
                }]);

                const { data: prod } = await supabase.from('products').select('stock_cartons, stock_pieces').eq('id', item.product_id).single();
                if (prod) {
                    await supabase.from('products').update({
                        stock_cartons: Math.max(0, prod.stock_cartons - item.cartons),
                        stock_pieces: Math.max(0, prod.stock_pieces - item.pieces)
                    }).eq('id', item.product_id);
                }
            }
        }

        return res.json({
            success: true,
            order_id: newOrder.id,
            is_out_of_location,
            distance_meters: Math.round(distance),
            message: is_out_of_location ? `Order accepted! WARNING: Out-of-location entry (${Math.round(distance)}m from shop).` : 'Order submitted successfully!'
        });
    }

    db.get("SELECT name, lat, lng, due_amount FROM shops WHERE id = ?", [shop_id], (err, shop) => {
        if (err || !shop) return res.status(400).json({ success: false, message: 'Shop not found!' });

        db.get("SELECT name FROM users WHERE id = ?", [dsr_id], (err2, dsr) => {
            const dsr_name = dsr ? dsr.name : 'Unassigned';

            // Calculate distance for Geo-fencing check (Radius 50m)
            const distance = getDistanceMeters(sr_lat, sr_lng, shop.lat, shop.lng);
            const is_out_of_location = (distance > 50) ? 1 : 0;

            const net_bill = total_bill - discount;
            const due = net_bill - cash_collected;

            db.run(`INSERT INTO orders (shop_id, shop_name, sr_id, sr_name, dsr_id, dsr_name, total_bill, discount, free_gifts, cash_collected, due_amount, is_out_of_location, distance_meters, sr_lat, sr_lng)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
                [shop_id, shop.name, sr_id, sr_name, dsr_id, dsr_name, total_bill, discount, free_gifts, cash_collected, due, is_out_of_location, Math.round(distance), sr_lat, sr_lng],
                function(err3) {
                    if (err3) return res.status(500).json({ success: false, error: err3.message });
                    const order_id = this.lastID;

                    // Update shop due amount
                    db.run("UPDATE shops SET due_amount = due_amount + ? WHERE id = ?", [due, shop_id]);

                    // Insert Order Items & Deduct Stock
                    if (items && Array.isArray(items)) {
                        items.forEach(item => {
                            db.run("INSERT INTO order_items (order_id, product_id, product_name, cartons, pieces, price) VALUES (?, ?, ?, ?, ?, ?)",
                                [order_id, item.product_id, item.product_name, item.cartons, item.pieces, item.price]);

                            db.run("UPDATE products SET stock_cartons = stock_cartons - ?, stock_pieces = stock_pieces - ? WHERE id = ?",
                                [item.cartons, item.pieces, item.product_id]);
                        });
                    }

                    res.json({
                        success: true,
                        order_id,
                        is_out_of_location,
                        distance_meters: Math.round(distance),
                        message: is_out_of_location ? `Order accepted! WARNING: Out-of-location entry (${Math.round(distance)}m from shop).` : 'Order submitted successfully!'
                    });
                }
            );
        });
    });
});

// DSR Today's Orders API
app.get('/api/orders/dsr/:dsr_id/today', async (req, res) => {
    const dsr_id = req.params.dsr_id;

    if (useSupabase) {
        const { data, error } = await supabase.from('orders').select('*').eq('dsr_id', dsr_id);
        if (error) return res.status(500).json({ error: error.message });
        return res.json(data);
    }

    db.all(`SELECT o.*, s.lat AS shop_lat, s.lng AS shop_lng, s.address AS shop_address
            FROM orders o JOIN shops s ON o.shop_id = s.id
            WHERE o.dsr_id = ? AND date(o.created_at) = date('now')`, [dsr_id], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

// DSR Aggregated Load Sheet API (SUM product quantities for today's assigned orders)
app.get('/api/orders/dsr/:dsr_id/load-sheet', async (req, res) => {
    const dsr_id = req.params.dsr_id;

    if (useSupabase) {
        const { data, error } = await supabase.from('order_items').select('product_name, cartons, pieces');
        if (error) return res.status(500).json({ error: error.message });
        return res.json(data);
    }

    db.all(`SELECT oi.product_name, SUM(oi.cartons) AS total_cartons, SUM(oi.pieces) AS total_pieces
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.dsr_id = ? AND date(o.created_at) = date('now')
            GROUP BY oi.product_id, oi.product_name`, [dsr_id], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

// Collections API
app.post('/api/collections', async (req, res) => {
    const { shop_id, dsr_id, dsr_name, amount, payment_type } = req.body;

    if (useSupabase) {
        const { data: shop } = await supabase.from('shops').select('name, due_amount').eq('id', shop_id).single();
        if (!shop) return res.status(400).json({ success: false, message: 'Shop not found' });

        const { data: col, error } = await supabase.from('collections').insert([{
            shop_id, shop_name: shop.name, dsr_id, dsr_name, amount, payment_type
        }]).select().single();

        if (error) return res.status(500).json({ success: false, error: error.message });

        await supabase.from('shops').update({ due_amount: (shop.due_amount || 0) - amount }).eq('id', shop_id);

        return res.json({ success: true, collection_id: col.id, message: 'Collection saved successfully!' });
    }

    db.get("SELECT name FROM shops WHERE id = ?", [shop_id], (err, shop) => {
        if (err || !shop) return res.status(400).json({ success: false, message: 'Shop not found' });

        db.run("INSERT INTO collections (shop_id, shop_name, dsr_id, dsr_name, amount, payment_type) VALUES (?, ?, ?, ?, ?, ?)",
            [shop_id, shop.name, dsr_id, dsr_name, amount, payment_type],
            function(err2) {
                if (err2) return res.status(500).json({ success: false, error: err2.message });

                // Deduct due amount from shop
                db.run("UPDATE shops SET due_amount = due_amount - ? WHERE id = ?", [amount, shop_id]);

                res.json({ success: true, collection_id: this.lastID, message: 'Collection saved successfully!' });
            }
        );
    });
});

// Shop Digital Ledger API with Filters
app.get('/api/shops/:shop_id/ledger', async (req, res) => {
    const shop_id = req.params.shop_id;
    const filter = req.query.filter || 'ALL'; // ALL, PURCHASE, PAYMENT, DISCOUNT

    if (useSupabase) {
        const { data: orders } = await supabase.from('orders').select('id, total_bill, discount, free_gifts, created_at').eq('shop_id', shop_id);
        const { data: collections } = await supabase.from('collections').select('id, amount, payment_type, date').eq('shop_id', shop_id);
        const { data: shop } = await supabase.from('shops').select('due_amount').eq('id', shop_id).single();

        let txs = [];
        if (orders) {
            orders.forEach(o => txs.push({ type: 'PURCHASE', id: o.id, amount: o.total_bill, discount: o.discount, free_gifts: o.free_gifts, date: o.created_at }));
        }
        if (collections) {
            collections.forEach(c => txs.push({ type: 'PAYMENT', id: c.id, amount: c.amount, discount: 0, free_gifts: c.payment_type, date: c.date }));
        }

        if (filter === 'PURCHASE') txs = txs.filter(r => r.type === 'PURCHASE');
        if (filter === 'PAYMENT') txs = txs.filter(r => r.type === 'PAYMENT');
        if (filter === 'DISCOUNT') txs = txs.filter(r => r.discount > 0);

        return res.json({
            current_due: shop ? shop.due_amount : 0,
            transactions: txs
        });
    }

    let query = `
        SELECT 'PURCHASE' AS type, id, total_bill AS amount, discount, free_gifts, created_at AS date
        FROM orders WHERE shop_id = ?
        UNION ALL
        SELECT 'PAYMENT' AS type, id, amount, 0 AS discount, payment_type AS free_gifts, date
        FROM collections WHERE shop_id = ?
        ORDER BY date DESC
    `;

    db.all(query, [shop_id, shop_id], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });

        let filteredRows = rows;
        if (filter === 'PURCHASE') filteredRows = rows.filter(r => r.type === 'PURCHASE');
        if (filter === 'PAYMENT') filteredRows = rows.filter(r => r.type === 'PAYMENT');
        if (filter === 'DISCOUNT') filteredRows = rows.filter(r => r.discount > 0);

        db.get("SELECT due_amount FROM shops WHERE id = ?", [shop_id], (err2, shop) => {
            res.json({
                current_due: shop ? shop.due_amount : 0,
                transactions: filteredRows
            });
        });
    });
});

// Admin Dashboard Summary API
app.get('/api/admin/dashboard', async (req, res) => {
    if (useSupabase) {
        const { data: orders } = await supabase.from('orders').select('*');
        const { data: shops } = await supabase.from('shops').select('*');
        const { data: products } = await supabase.from('products').select('*');
        const { data: users } = await supabase.from('users').select('id, name, phone, role');

        let totalSales = 0, totalCollection = 0, totalDue = 0, flaggedCount = 0;

        if (orders) {
            orders.forEach(o => {
                totalSales += (o.total_bill || 0) - (o.discount || 0);
                totalCollection += (o.cash_collected || 0);
                totalDue += (o.due_amount || 0);
                if (o.is_out_of_location) flaggedCount++;
            });
        }

        return res.json({
            total_sales: totalSales,
            total_collection: totalCollection,
            total_due: totalDue,
            flagged_orders_count: flaggedCount,
            recent_orders: orders ? orders.slice(0, 20) : [],
            shops: shops || [],
            products: products || [],
            users: users || []
        });
    }

    db.get("SELECT SUM(total_bill - discount) AS total_sales, SUM(cash_collected) AS total_collection, SUM(due_amount) AS total_due FROM orders", [], (err, orderSummary) => {
        db.get("SELECT COUNT(*) AS flagged_count FROM orders WHERE is_out_of_location = 1", [], (err2, flagged) => {
            db.all("SELECT * FROM orders ORDER BY created_at DESC LIMIT 20", [], (err3, recentOrders) => {
                db.all("SELECT * FROM shops", [], (err4, shops) => {
                    db.all("SELECT * FROM products", [], (err5, products) => {
                        db.all("SELECT id, name, phone, role FROM users", [], (err6, users) => {
                            res.json({
                                total_sales: (orderSummary && orderSummary.total_sales) || 0,
                                total_collection: (orderSummary && orderSummary.total_collection) || 0,
                                total_due: (orderSummary && orderSummary.total_due) || 0,
                                flagged_orders_count: (flagged && flagged.flagged_count) || 0,
                                recent_orders: recentOrders || [],
                                shops: shops || [],
                                products: products || [],
                                users: users || []
                            });
                        });
                    });
                });
            });
        });
    });
});

// Admin Web Dashboard Page
app.get('/', (req, res) => {
    res.send(`
    <!DOCTYPE html>
    <html>
    <head>
        <title>Moon Enterprise DSR - Master Control Panel</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f4f6f9; margin: 0; padding: 20px; }
            h1 { color: #1A237E; margin-bottom: 5px; }
            .sub { color: #555; margin-bottom: 20px; }
            .card-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }
            .card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
            .card h3 { margin: 0 0 10px 0; font-size: 14px; color: #666; }
            .card .value { font-size: 24px; font-weight: bold; color: #1A237E; }
            .card.flagged { border-left: 5px solid #d32f2f; }
            .card.flagged .value { color: #d32f2f; }

            .tab-nav { display: flex; gap: 10px; margin-bottom: 20px; border-bottom: 2px solid #ccc; }
            .tab-btn { padding: 12px 20px; background: #e0e0e0; border: none; font-weight: bold; cursor: pointer; border-radius: 6px 6px 0 0; color: #333; }
            .tab-btn.active { background: #1A237E; color: white; }
            .tab-content { display: none; }
            .tab-content.active { display: block; }

            .section-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; }
            @media (max-width: 800px) { .section-grid { grid-template-columns: 1fr; } }

            .form-box { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
            .form-box h3 { margin-top: 0; color: #1A237E; }
            .form-group { margin-bottom: 12px; }
            .form-group label { display: block; font-size: 13px; font-weight: bold; margin-bottom: 4px; }
            .form-group input, .form-group select { width: 100%; padding: 10px; box-sizing: border-box; border: 1px solid #ccc; border-radius: 4px; }
            .btn-submit { width: 100%; padding: 12px; background: #1A237E; color: white; border: none; border-radius: 4px; font-size: 15px; font-weight: bold; cursor: pointer; }
            .btn-submit:hover { background: #0D47A1; }

            table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.1); margin-bottom: 20px; }
            th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #ddd; }
            th { background: #1A237E; color: white; }
            .badge-flagged { background: #ffebee; color: #c62828; padding: 4px 8px; border-radius: 4px; font-weight: bold; font-size: 12px; }
            .badge-ok { background: #e8f5e9; color: #2e7d32; padding: 4px 8px; border-radius: 4px; font-weight: bold; font-size: 12px; }
            .badge-role { background: #e3f2fd; color: #1565c0; padding: 4px 8px; border-radius: 4px; font-weight: bold; font-size: 12px; }
        </style>
    </head>
    <body>
        <h1>MOON ENTERPRISE DSR - Master Control Panel</h1>
        <div class="sub">Server mode: <b>${useSupabase ? 'Supabase Cloud Database' : 'Local SQLite Database'}</b></div>

        <!-- Top Overview Summary -->
        <div class="card-grid">
            <div class="card">
                <h3>Total Monthly Sales</h3>
                <div class="value" id="valSales">Tk. 0</div>
            </div>
            <div class="card">
                <h3>Total Cash Collection</h3>
                <div class="value" style="color: #2e7d32" id="valCollection">Tk. 0</div>
            </div>
            <div class="card">
                <h3>Outstanding Due</h3>
                <div class="value" style="color: #c62828" id="valDue">Tk. 0</div>
            </div>
            <div class="card flagged">
                <h3>Geo-Fence Alert Flags</h3>
                <div class="value" id="valFlags">0 Out-of-Location</div>
            </div>
        </div>

        <!-- Tab Navigation -->
        <div class="tab-nav">
            <button class="tab-btn active" onclick="showTab('tabOrders')">📊 Live Orders & Audit Logs</button>
            <button class="tab-btn" onclick="showTab('tabStock')">📦 Stock & Inventory Management</button>
            <button class="tab-btn" onclick="showTab('tabUsers')">👥 SR & DSR User Profiles</button>
        </div>

        <!-- TAB 1: Live Orders & Geo-Fence Audit Logs -->
        <div id="tabOrders" class="tab-content active">
            <h2>Live Orders & Geo-Fence Audit Logs</h2>
            <table>
                <thead>
                    <tr>
                        <th>Order #</th>
                        <th>Date/Time</th>
                        <th>Shop Name</th>
                        <th>SR Name</th>
                        <th>DSR Name</th>
                        <th>Total Bill</th>
                        <th>Cash Paid</th>
                        <th>Due</th>
                        <th>Location Status</th>
                    </tr>
                </thead>
                <tbody id="orderTable">
                    <tr><td colspan="9">Loading live orders...</td></tr>
                </tbody>
            </table>
        </div>

        <!-- TAB 2: Stock & Inventory Management -->
        <div id="tabStock" class="tab-content">
            <h2>Factory / Company Stock Overview</h2>
            <div class="section-grid">
                <div>
                    <table>
                        <thead>
                            <tr>
                                <th>Product ID</th>
                                <th>Product Name</th>
                                <th>Unit Price</th>
                                <th>Stock Cartons</th>
                                <th>Stock Pieces</th>
                                <th>Pieces/Carton</th>
                            </tr>
                        </thead>
                        <tbody id="stockTable">
                            <tr><td colspan="6">Loading current stock...</td></tr>
                        </tbody>
                    </table>
                </div>

                <div>
                    <!-- Form 1: Add New Product -->
                    <div class="form-box" style="margin-bottom: 20px;">
                        <h3>➕ Add New Product</h3>
                        <form id="formNewProduct" onsubmit="handleNewProduct(event)">
                            <div class="form-group">
                                <label>Product Name</label>
                                <input type="text" id="npName" required placeholder="e.g. Choco Crunch Box">
                            </div>
                            <div class="form-group">
                                <label>Unit Price (Tk.)</label>
                                <input type="number" step="0.01" id="npPrice" required placeholder="40">
                            </div>
                            <div class="form-group">
                                <label>Stock Cartons</label>
                                <input type="number" id="npCartons" value="0">
                            </div>
                            <div class="form-group">
                                <label>Stock Pieces</label>
                                <input type="number" id="npPieces" value="0">
                            </div>
                            <div class="form-group">
                                <label>Pieces Per Carton</label>
                                <input type="number" id="npPpc" value="24">
                            </div>
                            <button type="submit" class="btn-submit">Add Product</button>
                        </form>
                    </div>

                    <!-- Form 2: Add Incoming Stock to Existing Product -->
                    <div class="form-box">
                        <h3>📥 Stock Entry (Add Incoming Stock)</h3>
                        <form id="formAddStock" onsubmit="handleAddStock(event)">
                            <div class="form-group">
                                <label>Select Product</label>
                                <select id="asProductId" required></select>
                            </div>
                            <div class="form-group">
                                <label>Add Cartons</label>
                                <input type="number" id="asCartons" value="0">
                            </div>
                            <div class="form-group">
                                <label>Add Loose Pieces</label>
                                <input type="number" id="asPieces" value="0">
                            </div>
                            <button type="submit" class="btn-submit" style="background: #2e7d32;">Update Stock</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>

        <!-- TAB 3: SR & DSR User Profiles -->
        <div id="tabUsers" class="tab-content">
            <h2>SR, DSR & Admin User Profiles</h2>
            <div class="section-grid">
                <div>
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>User Name</th>
                                <th>Mobile Number</th>
                                <th>Role</th>
                            </tr>
                        </thead>
                        <tbody id="usersTable">
                            <tr><td colspan="4">Loading profiles...</td></tr>
                        </tbody>
                    </table>
                </div>

                <div>
                    <div class="form-box">
                        <h3>👤 Create SR / DSR Profile</h3>
                        <form id="formNewUser" onsubmit="handleNewUser(event)">
                            <div class="form-group">
                                <label>Full Name</label>
                                <input type="text" id="nuName" required placeholder="e.g. Shakil Hossain">
                            </div>
                            <div class="form-group">
                                <label>Mobile Number (Phone)</label>
                                <input type="text" id="nuPhone" required placeholder="01711001122">
                            </div>
                            <div class="form-group">
                                <label>Password</label>
                                <input type="password" id="nuPassword" required placeholder="1234">
                            </div>
                            <div class="form-group">
                                <label>Assign Role</label>
                                <select id="nuRole" required>
                                    <option value="SR">Sales Representative (SR)</option>
                                    <option value="DSR">Delivery Representative (DSR)</option>
                                    <option value="ADMIN">System Admin</option>
                                </select>
                            </div>
                            <button type="submit" class="btn-submit">Create User Profile</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>

        <script>
            function showTab(tabId) {
                document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
                document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

                event.target.classList.add('active');
                document.getElementById(tabId).classList.add('active');
            }

            async function loadDashboard() {
                try {
                    const res = await fetch('/api/admin/dashboard');
                    const data = await res.json();

                    document.getElementById('valSales').innerText = 'Tk. ' + data.total_sales;
                    document.getElementById('valCollection').innerText = 'Tk. ' + data.total_collection;
                    document.getElementById('valDue').innerText = 'Tk. ' + data.total_due;
                    document.getElementById('valFlags').innerText = data.flagged_orders_count + ' Flagged';

                    // 1. Render Orders Table
                    const table = document.getElementById('orderTable');
                    if(data.recent_orders.length === 0) {
                        table.innerHTML = '<tr><td colspan="9">No orders placed yet.</td></tr>';
                    } else {
                        table.innerHTML = data.recent_orders.map(o => \`
                            <tr>
                                <td>#\${o.id}</td>
                                <td>\${o.created_at}</td>
                                <td><b>\${o.shop_name}</b></td>
                                <td>\${o.sr_name}</td>
                                <td>\${o.dsr_name}</td>
                                <td>Tk. \${o.total_bill}</td>
                                <td>Tk. \${o.cash_collected}</td>
                                <td>Tk. \${o.due_amount}</td>
                                <td>
                                    \${o.is_out_of_location ?
                                        \`<span class="badge-flagged">🚩 OUT OF LOCATION (\${o.distance_meters}m)</span>\` :
                                        \`<span class="badge-ok">VERIFIED (Near Shop)</span>\`
                                    }
                                </td>
                            </tr>
                        \`).join('');
                    }

                    // 2. Render Stock Table
                    const stockTable = document.getElementById('stockTable');
                    const selectProduct = document.getElementById('asProductId');

                    if(data.products.length === 0) {
                        stockTable.innerHTML = '<tr><td colspan="6">No products found.</td></tr>';
                        selectProduct.innerHTML = '<option value="">No products</option>';
                    } else {
                        stockTable.innerHTML = data.products.map(p => \`
                            <tr>
                                <td>#\${p.id}</td>
                                <td><b>\${p.name}</b></td>
                                <td>Tk. \${p.price}</td>
                                <td><b>\${p.stock_cartons}</b> Cartons</td>
                                <td><b>\${p.stock_pieces}</b> Pieces</td>
                                <td>\${p.pieces_per_carton}</td>
                            </tr>
                        \`).join('');

                        selectProduct.innerHTML = data.products.map(p => \`<option value="\${p.id}">\${p.name} (Current: \${p.stock_cartons} Cartons, \${p.stock_pieces} Pieces)</option>\`).join('');
                    }

                    // 3. Render Users Table
                    const usersTable = document.getElementById('usersTable');
                    if(data.users.length === 0) {
                        usersTable.innerHTML = '<tr><td colspan="4">No users registered.</td></tr>';
                    } else {
                        usersTable.innerHTML = data.users.map(u => \`
                            <tr>
                                <td>#\${u.id}</td>
                                <td><b>\${u.name}</b></td>
                                <td>\${u.phone}</td>
                                <td><span class="badge-role">\${u.role}</span></td>
                            </tr>
                        \`).join('');
                    }

                } catch(e) {
                    console.error(e);
                }
            }

            async function handleNewProduct(e) {
                e.preventDefault();
                const name = document.getElementById('npName').value;
                const price = document.getElementById('npPrice').value;
                const stock_cartons = document.getElementById('npCartons').value;
                const stock_pieces = document.getElementById('npPieces').value;
                const pieces_per_carton = document.getElementById('npPpc').value;

                const res = await fetch('/api/products/add', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, price, stock_cartons, stock_pieces, pieces_per_carton })
                });
                const result = await res.json();
                if(result.success) {
                    alert('Product added successfully!');
                    document.getElementById('formNewProduct').reset();
                    loadDashboard();
                } else {
                    alert('Error: ' + result.message);
                }
            }

            async function handleAddStock(e) {
                e.preventDefault();
                const id = document.getElementById('asProductId').value;
                const stock_cartons = document.getElementById('asCartons').value;
                const stock_pieces = document.getElementById('asPieces').value;

                const res = await fetch('/api/products/stock', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ id, stock_cartons, stock_pieces })
                });
                const result = await res.json();
                if(result.success) {
                    alert('Stock updated successfully!');
                    document.getElementById('formAddStock').reset();
                    loadDashboard();
                } else {
                    alert('Error: ' + result.message);
                }
            }

            async function handleNewUser(e) {
                e.preventDefault();
                const name = document.getElementById('nuName').value;
                const phone = document.getElementById('nuPhone').value;
                const password = document.getElementById('nuPassword').value;
                const role = document.getElementById('nuRole').value;

                const res = await fetch('/api/users', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, phone, password, role })
                });
                const result = await res.json();
                if(result.success) {
                    alert('User profile created successfully!');
                    document.getElementById('formNewUser').reset();
                    loadDashboard();
                } else {
                    alert('Error: ' + result.message);
                }
            }

            loadDashboard();
            setInterval(loadDashboard, 5000);
        </script>
    </body>
    </html>
    `);
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`Moon Enterprise DSR Backend Server listening on http://0.0.0.0:${PORT}`);
});
