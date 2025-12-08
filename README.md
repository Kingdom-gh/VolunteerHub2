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
#### **Cấu hình Rate Limit**
- **Average limit:** 200 requests/second  
- **Burst:** 50 requests  
- **Scope:** theo IP address  
=> Chống DoS ở tầng ứng dụng, ngăn backend bị quá tải
### 3. Thêm ORM, index cho DB

=> Tăng tốc độ cho những truy vấn đọc hay được sử dụng, tăng bảo mật, chống SQL injection

### 4. Thêm logic UI, UX
- Bổ sung thông báo tình trạng xử lý đăng ký người dùng, và số đăng ký mới đến người tạo post
- Cho người đăng bài kiểm duyệt đăng ký đến post
- Cải hiệt logic duy nhất 1 đăng ký của 1 người dùng đến cùng 1 post

=> Logic nhất quán, tránh rác cơ sở dữ liệu, cải thiện trải nghiệm người dùng

### 5. Thêm phân trang và cache theo page number
- Cũ: gửi toàn bộ dữ liệu có trong database
- Mới: phân trang lấy dữ liệu 10-15 bản ghi với mỗi api truy vấn đọc dữ liệu
 
=> Tránh lỗi OOM và giúp người dùng không cần chờ tất cả dữ liệu,

### 6. Thêm RabbitMQ xử lý bất đồng bộ request đăng ký/ xóa đăng ký
- Tách thao tác xử lý nặng (như gửi thông báo, lưu cơ sở dữ liệu) được xử lý sau
- Dùng unique constraint để đảm bảo 1 bảng duy nhất 1 cặp gmail và postId
- Dùng idempotency  để ghi nhận duy nhất 1 request được tạo bời người dùng

=> Cải thiện tính toàn vẹn dữ liệu khi lượng lớn người dùng đồng thời đăng ký/ xóa đăng ký
=> Retry 3 lần vào database, lần 1 là 0s, lần 2 là sau 5s, lần 3 là sau 30s

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
