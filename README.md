# VolunteerHub2 - (Improved version)
- Bài tập lớn môn Kiến trúc phần mềm
- Bản gốc: https://github.com/nahidbinwadood/Volunteer-Management-System
## Nhóm 15:
- Đỗ Đức Thắng - 23020158
- Đỗ Trung Kiên - 23020085
- Lương Vũ Thế - 23020159
- Đầu Hồng Quang - 23020135
## Chức năng chính của bản gốc:
- Quản lí sự kiện tình nguyện, tình nguyện viên
## Các cải tiến
### 1. Intelligent Load Balancing & Resilience (Traefik)

- Smart Traffic Distribution: Traefik tự động điều phối tải, cô lập các node lỗi và tự động tái hòa nhập khi ổn định  
- Performance-Aware Health Check: Thay vì chỉ kiểm tra kết nối (Ping), hệ thống giám sát P95 Latency và Thread Saturation để phát hiện sớm suy giảm hiệu năng  
- Overload Protection: Cơ chế ngắt mạch chủ động khi Thread Pool chạm ngưỡng cảnh báo (90%), ngăn chặn hiện tượng treo hệ thống  
- High Availability: Hỗ trợ Horizontal Scaling tin cậy, tối ưu hóa khả năng chịu lỗi và đảm bảo tính sẵn sàng cao  
### 2. Rate limiting 
- Rate limiting được cấu hình trực tiếp trên traefik, chặn các request spam
#### **Cấu hình Rate Limit**
- **Average limit:** 200 requests/second  
- **Burst:** 50 requests  
- **Scope:** theo IP address  
=> Chống DoS ở tầng ứng dụng, ngăn backend bị quá tải
### 3. Thêm ORM, index cho DB

=> Tăng tốc độ cho những truy vấn đọc hay được sử dụng, tăng bảo mật, chống SQL injection

## Cài đặt và chạy dự án
```bash
# Clone
git clone <repo-link>

# Chạy backend
cd back_end2
mvn spring-boot:run


# Chạy client
cd client-side
npm install
npm run dev
