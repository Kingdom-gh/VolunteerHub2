# VolunteerHub2 - (Improved version)
- Bài tập lớn môn Kiến trúc phần mềm
- Bản gốc: https://github.com/nahidbinwadood/Volunteer-Management-System
## Nhóm 15:
- **Đỗ Đức Thắng ** - 23020158
- **Đỗ Trung Kiên ** - 23020085
- **Lương Vũ Thế ** - 23020159
- **Đầu Hồng Quang ** - 23020135
## Chức năng chính của bản gốc:
- Quản lí sự kiện tình nguyện, tình nguyện viên
## Các cải tiến
### 1. Traefik Load Balancer
- Load Balancer tự động phân phối traffic đến các instance đang hoạt động tốt
- Các instance liên tục được health check bằng cách gọi API healthz(kiểm tra instance còn sống) và readyz(kiểm tra kết nối đến database, redis, mq, ...) -> nếu không hoạt động -> Load Balancer loại instance ra khỏi danh sách các instance có thể nhận request

=> Horizontal scaling, tăng khả năng chịu lỗi
### 2. Rate limiting 
- Rate limiting được cấu hình trực tiếp trên traefik, chặn các request spam
### **Cấu hình Rate Limit**
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
