const mysql = require("mysql2");

// Tạo pool kết nối tới MySQL
const pool = mysql.createPool({
  host: "localhost",       // host mặc định cho MySQL local
  user: "root",            // thay bằng user MySQL của bạn
  password: "",      // mật khẩu MySQL local (nếu có)
  database: "volunteerhub",// tên database bạn đã tạo
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
});

// Kiểm tra kết nối
pool.getConnection((err, connection) => {
  if (err) {
    console.error("❌ Lỗi kết nối MySQL:", err);
  } else {
    console.log("✅ Kết nối MySQL thành công!");
    connection.release();
  }
});

module.exports = pool;
